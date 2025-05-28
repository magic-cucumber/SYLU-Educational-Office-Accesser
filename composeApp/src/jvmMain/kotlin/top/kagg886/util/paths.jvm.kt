package top.kagg886.util

import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

actual val dataPath: Path by lazy {
    File(System.getProperty("user.home")).resolve(".config").resolve("eoa").toOkioPath()
}
actual val cachePath: Path by lazy {
    dataPath.resolve("cache")
}