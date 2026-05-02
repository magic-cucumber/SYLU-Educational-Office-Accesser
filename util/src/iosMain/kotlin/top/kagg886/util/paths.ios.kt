package top.kagg886.util

import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

actual val dataPath: Path by lazy {
    //需要在 Signing & Capabilities 里配置
    NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier("group.top.kagg886.eoa.iosApp.shared")!!.path!!.toPath()
}
actual val cachePath: Path by lazy {
    NSTemporaryDirectory().toPath()
}
