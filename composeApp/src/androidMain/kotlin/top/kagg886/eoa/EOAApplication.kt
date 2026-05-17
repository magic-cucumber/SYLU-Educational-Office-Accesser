package top.kagg886.eoa

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import top.kagg886.util.asTaggedLogger

class EOAApplication : Application(), SingletonImageLoader.Factory, Thread.UncaughtExceptionHandler {
    private val logger = "EOAApplication".asTaggedLogger
    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .installCoilConfig()
            .build()
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        logger.a(e) { "App crashed on thread ${t.name}" }
        defaultUncaughtExceptionHandler?.uncaughtException(t, e)
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
