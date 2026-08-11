package top.kagg886.report

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import top.kagg886.util.initializeMMKV

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val error = intent.getStringExtra("exceptions")!!
        initializeMMKV()
        setContent {
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
