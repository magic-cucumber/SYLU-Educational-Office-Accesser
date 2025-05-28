package top.kagg886.util

import okio.Path
import okio.Path.Companion.toPath
import top.kagg886.eoa.EOAApplication

actual val dataPath: Path by lazy {
    EOAApplication.getApp().filesDir.absolutePath.toPath()
}
actual val cachePath: Path by lazy {
    EOAApplication.getApp().cacheDir.absolutePath.toPath()
}