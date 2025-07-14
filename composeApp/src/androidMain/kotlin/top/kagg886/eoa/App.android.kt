package top.kagg886.eoa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import top.kagg886.eoa.pages.update.detail.UpdateInfo
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

actual fun downloadResourceUrl(info: UpdateInfo): String =
    info.assets.first { it.name.endsWith(".apk") }.browser_download_url
