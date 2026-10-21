@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package top.kagg886.eoa.worker

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.*
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundRefreshStatus.UIBackgroundRefreshStatusAvailable
import top.kagg886.backend.database.AppDatabase
import kotlin.coroutines.resume

private const val TASK_ID = "top.kagg886.eoa.light-sync"
private const val ENABLED_KEY = "$TASK_ID.enabled"
private const val NEXT_RUN_KEY = "$TASK_ID.next-run"
private const val INTERVAL_SECONDS = 2 * 60 * 60.0
private val logger = Logger.withTag("LightSyncScheduler")
private val scheduler get() = BGTaskScheduler.sharedScheduler
private val defaults get() = NSUserDefaults.standardUserDefaults
private val lock = NSRecursiveLock()
private var installed = false
private var generation = 0L
private var activeRun: SyncRun? = null

private inline fun <T> locked(block: () -> T): T {
    lock.lock()
    try {
        return block()
    } finally {
        lock.unlock()
    }
}

/** Install once per process before application:didFinishLaunchingWithOptions: returns. */
internal fun installLightSyncTask(database: AppDatabase) = locked {
    if (installed) return@locked
    check(scheduler.registerForTaskWithIdentifier(TASK_ID, usingQueue = null) { task ->
        if (task != null) execute(task, database)
    }) { "Unable to install background handler for $TASK_ID" }
    installed = true
    // Recover an enabled schedule if a previous submission failed or was lost.
    if (defaults.boolForKey(ENABLED_KEY)) ensurePendingRequest()
}

internal actual suspend fun LightSyncTask.register() = locked {
    check(installed) { "initializeLightSyncTask must be called during application launch" }
    if (!defaults.boolForKey(ENABLED_KEY)) {
        defaults.setBool(true, forKey = ENABLED_KEY)
        defaults.setDouble(
            maxOf(defaults.doubleForKey(NEXT_RUN_KEY), now() + INTERVAL_SECONDS),
            forKey = NEXT_RUN_KEY
        )
    }
    ensurePendingRequest()
}

internal actual suspend fun LightSyncTask.unregister() = locked {
    defaults.setBool(false, forKey = ENABLED_KEY)
    generation++
    scheduler.cancelTaskRequestWithIdentifier(TASK_ID)
    logger.i("Cancelled $TASK_ID")
    // Keep the earliest allowed time across disable/re-enable cycles.
    activeRun?.cancel()
    Unit
}

internal actual suspend fun LightSyncTask.isRegister(): Boolean =
    suspendCancellableCoroutine { continuation ->
        scheduler.getPendingTaskRequestsWithCompletionHandler { requests ->
            continuation.resume(
                requests.orEmpty().filterIsInstance<BGTaskRequest>()
                    .any { it.identifier == TASK_ID })
        }
    }

internal actual suspend fun LightSyncTask.canRegister(): Boolean = withContext(Dispatchers.Main) {
    val bundle = NSBundle.mainBundle
    val identifiers =
        bundle.objectForInfoDictionaryKey("BGTaskSchedulerPermittedIdentifiers") as? List<*>
    val modes = bundle.objectForInfoDictionaryKey("UIBackgroundModes") as? List<*>
    UIApplication.sharedApplication.backgroundRefreshStatus == UIBackgroundRefreshStatusAvailable &&
            identifiers?.contains(TASK_ID) == true && modes?.contains("processing") == true
}

// Call only under lock. The generation rejects pending callbacks from an older schedule.
private fun ensurePendingRequest() {
    val expectedGeneration = generation
    scheduler.getPendingTaskRequestsWithCompletionHandler { requests ->
        locked {
            if (generation == expectedGeneration && defaults.boolForKey(ENABLED_KEY) &&
                requests.orEmpty().filterIsInstance<BGTaskRequest>()
                    .none { it.identifier == TASK_ID }
            ) {
                submitNextRequest()
            }
        }
    }
}

private fun now(): Double = NSDate().timeIntervalSince1970

private fun submitNextRequest() {
    val request = BGProcessingTaskRequest(identifier = TASK_ID).apply {
        requiresNetworkConnectivity = true
        requiresExternalPower = false
        earliestBeginDate =
            NSDate.dateWithTimeIntervalSince1970(defaults.doubleForKey(NEXT_RUN_KEY))
    }
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        error.value = null
        if (!scheduler.submitTaskRequest(request, error.ptr)) {
            logger.e("Unable to schedule $TASK_ID: ${error.value}")
        } else {
            logger.i("Scheduled $TASK_ID, earliest: ${request.earliestBeginDate}")
        }
    }
}

private fun execute(task: BGTask, database: AppDatabase) = locked {
    if (!defaults.boolForKey(ENABLED_KEY)) {
        task.setTaskCompletedWithSuccess(false)
        return@locked
    }
    if (now() < defaults.doubleForKey(NEXT_RUN_KEY) || activeRun != null) {
        submitNextRequest()
        task.setTaskCompletedWithSuccess(false)
        return@locked
    }
    // Persist and submit before syncing, so expiration or process termination does not end the cycle.
    defaults.setDouble(now() + INTERVAL_SECONDS, forKey = NEXT_RUN_KEY)
    submitNextRequest()
    logger.i("Executing $TASK_ID")
    val run = SyncRun(task)
    activeRun = run
    run.start(database)
}

private class SyncRun(private val task: BGTask) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var completed = false
    private var success = false

    fun start(database: AppDatabase) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                LightSyncTask.sync(database)
                success = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e("Background sync failed", e)
            }
        }
        job.invokeOnCompletion {
            locked {
                complete(success && it == null)
                if (activeRun === this) activeRun = null
                scope.cancel()
            }
        }
        task.expirationHandler = { locked { cancel() } }
        job.start()
    }

    fun cancel() {
        scope.cancel()
        complete(false)
    }

    private fun complete(success: Boolean) {
        if (completed) return
        completed = true
        task.expirationHandler = null
        task.setTaskCompletedWithSuccess(success)
    }
}
