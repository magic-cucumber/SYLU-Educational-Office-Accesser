package top.kagg886.eoa

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import coil3.ImageLoader
import coil3.SingletonImageLoader
import top.kagg886.report.CrashActivity
import top.kagg886.util.logger
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class EOAApplication : Application(), SingletonImageLoader.Factory, Thread.UncaughtExceptionHandler {
    override fun onCreate() {
        super.onCreate()
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
        logger.withTag("EOAApplication").e("App crashed", e)
        thread {
            val intent = Intent(this, CrashActivity::class.java)
            intent.putExtra("exceptions", e.stackTraceToString())
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            Thread.sleep(1)
            exitProcess(0)
        }
    }

    companion object {
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
