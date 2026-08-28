package top.kagg886.eoa

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import co.touchlab.kermit.Severity
import coil3.ImageLoader
import coil3.SingletonImageLoader
import kotlinx.coroutines.runBlocking
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.databaseBuilder
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.eoa.util.registerKermitLoggerIfExists
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import kotlin.time.Clock

class EOAApplication : Application(), SingletonImageLoader.Factory, Thread.UncaughtExceptionHandler {
    private val databaseDelegate = lazy {
        val db = databaseBuilder().build()
        registerKermitLoggerIfExists(db.appLogDao())
        db
    }
    val database: AppDatabase by databaseDelegate

    private val handlingCrash = AtomicBoolean(false)
    private var systemCrashHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()

        // CrashActivity runs in a dedicated process and opens a fresh database after
        // the crashed process has closed its own Room instance.
        if (isCrashProcess()) return

        database
        systemCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        Handler(Looper.getMainLooper()).post {
            try {
                while (true) {
                    Looper.loop()
                }
            } catch (e: Throwable) {
                uncaughtException(Thread.currentThread(), e)
            }
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .installCoilConfig()
            .build()
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (!handlingCrash.compareAndSet(false, true)) {
            terminateWithSystemHandler(t, e)
            return
        }

        Log.e(TAG, "App crashed on thread ${t.name}", e)
        val stackTrace = runCatching { e.stackTraceToString() }
            .getOrElse { "${e::class.qualifiedName}: ${e.message}" }

        persistCrashAndCloseDatabase(t, e, stackTrace)

        try {
            val intent = Intent().apply {
                setClassName(
                    packageName,
                    "top.kagg886.report.CrashActivity"
                )
                putExtra(EXTRA_EXCEPTIONS, stackTrace.take(MAX_INTENT_STACK_TRACE_LENGTH))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
        } catch (launchError: Throwable) {
            Log.e(TAG, "Unable to launch CrashActivity", launchError)
            terminateWithSystemHandler(t, e)
            return
        }

        // startActivity() has synchronously submitted the launch request to the
        // system. CrashActivity lives in :crash, so terminating this process cannot
        // close its new database or kill the crash screen.
        Process.killProcess(Process.myPid())
        exitProcess(CRASH_EXIT_CODE)
    }

    private fun persistCrashAndCloseDatabase(thread: Thread, error: Throwable, stackTrace: String) {
        if (!databaseDelegate.isInitialized()) return

        val db = databaseDelegate.value
        try {
            runBlocking {
                db.appLogDao().insert(
                    AppLog(
                        tag = TAG,
                        level = Severity.Error,
                        message = "App crashed on thread ${thread.name}",
                        time = Clock.System.now(),
                        stacktrace = stackTrace
                    )
                )
            }
        } catch (saveError: Throwable) {
            Log.e(TAG, "Unable to persist crash log", saveError)
        } finally {
            try {
                db.close()
            } catch (closeError: Throwable) {
                Log.e(TAG, "Unable to close database", closeError)
            }
        }
    }

    private fun terminateWithSystemHandler(thread: Thread, error: Throwable) {
        val handler = systemCrashHandler
        if (handler != null && handler !== this) {
            handler.uncaughtException(thread, error)
        }
        Process.killProcess(Process.myPid())
        exitProcess(CRASH_EXIT_CODE)
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun isCrashProcess(): Boolean {
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.runningAppProcesses
                ?.firstOrNull { it.pid == Process.myPid() }
                ?.processName
        }
        return processName == "$packageName:crash"
    }

    companion object {
        private const val TAG = "EOAApplication"
        private const val EXTRA_EXCEPTIONS = "exceptions"
        private const val MAX_INTENT_STACK_TRACE_LENGTH = 200_000
        private const val CRASH_EXIT_CODE = 10

        @SuppressLint("DiscouragedPrivateApi", "PrivateApi")
        fun getApp(): Application {
            var application: Application? = null
            try {
                val atClass = Class.forName("android.app.ActivityThread")
                val currentApplicationMethod = atClass.getDeclaredMethod("currentApplication")
                currentApplicationMethod.isAccessible = true
                application = currentApplicationMethod.invoke(null) as Application
            } catch (ignored: Exception) {
            }
            if (application != null) return application
            try {
                val atClass = Class.forName("android.app.AppGlobals")
                val currentApplicationMethod = atClass.getDeclaredMethod("getInitialApplication")
                currentApplicationMethod.isAccessible = true
                application = currentApplicationMethod.invoke(null) as Application
            } catch (ignored: Exception) {
            }
            return application!!
        }
    }
}
