package top.kagg886.eoa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import top.kagg886.util.initializeMMKV

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeMMKV()
        FileKit.init(this)
        setContent { App() }
    }
}
