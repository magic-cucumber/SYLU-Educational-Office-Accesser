package top.kagg886.eoa.worker

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import top.kagg886.eoa.EOAApplicationLike
import top.kagg886.util.currentApplication
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "top.kagg886.eoa.light-sync"
private val logger = Logger.withTag("LightSyncScheduler")

class LightSyncWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        LightSyncTask.sync((applicationContext as EOAApplicationLike).database)
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.e("Background sync failed", e)
        // Keep the periodic schedule; do not add retries shorter than its interval.
        Result.failure()
    }
}

internal actual suspend fun LightSyncTask.register() {
    val request = PeriodicWorkRequestBuilder<LightSyncWorker>(2, TimeUnit.HOURS)
        .setInitialDelay(2, TimeUnit.HOURS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(currentApplication())
        .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request).await()
}

internal actual suspend fun LightSyncTask.unregister() {
    WorkManager.getInstance(currentApplication()).cancelUniqueWork(WORK_NAME).await()
}

internal actual suspend fun LightSyncTask.isRegister(): Boolean =
    WorkManager.getInstance(currentApplication())
        .getWorkInfosForUniqueWorkFlow(WORK_NAME).first().any { !it.state.isFinished }

@SuppressLint("ObsoleteSdkInt", "PrivateApi", "DiscouragedPrivateApi")
internal actual suspend fun LightSyncTask.canRegister(): Boolean = withContext(Dispatchers.IO) {
    val app = currentApplication()
    try {
        when {
            Build.VERSION.SDK_INT >= 28 ->
                !(app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isBackgroundRestricted

            Build.VERSION.SDK_INT >= 24 -> {
                // AOSP N introduced this app-op, but did not expose its integer API in the SDK.
                val manager = app.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val operation = AppOpsManager::class.java.getDeclaredField("OP_RUN_IN_BACKGROUND")
                    .getInt(null)
                val mode = AppOpsManager::class.java.getDeclaredMethod(
                    "checkOpNoThrow",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                    .invoke(manager, operation, Process.myUid(), app.packageName) as Int
                mode == AppOpsManager.MODE_ALLOWED
            }
            // Android 6 has no AOSP background-execution restriction app-op.
            else -> true
        }
    } catch (e: Exception) {
        logger.e(
            "Unable to query background execution restrictions (API ${Build.VERSION.SDK_INT})",
            e
        )
        false
    }
}
