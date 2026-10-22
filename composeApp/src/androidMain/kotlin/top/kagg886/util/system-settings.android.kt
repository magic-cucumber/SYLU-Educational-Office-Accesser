package top.kagg886.util

import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

actual fun openAppSystemSettings() {
    val app = currentApplication()
    app.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${app.packageName}".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
