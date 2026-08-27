package top.kagg886.report

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import top.kagg886.eoa.EOAApplication
import top.kagg886.eoa.LocalDatabase
import top.kagg886.util.initializeMMKV

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val error = intent.getStringExtra("exceptions")!!
        val application = application as EOAApplication
        initializeMMKV()
        setContent {
            CompositionLocalProvider(LocalDatabase provides application.database) {
                CrashApp(
                    error = error,
                    onRestart = {
                        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                            startActivity(Intent.makeRestartActivityTask(intent.component))
                        }
                        finish()
                    }
                )
            }
        }
    }
}
