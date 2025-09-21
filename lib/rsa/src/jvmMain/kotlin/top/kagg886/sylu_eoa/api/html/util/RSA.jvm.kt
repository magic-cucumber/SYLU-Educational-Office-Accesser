package top.kagg886.sylu_eoa.api.html.util

import okio.BufferedSource
import okio.HashingSink
import okio.Path
import okio.Path.Companion.toPath
import okio.blackholeSink
import okio.buffer
import okio.source
import top.kagg886.util.absolutePath
import top.kagg886.util.createNewFile
import top.kagg886.util.exists
import top.kagg886.util.mkdirs
import top.kagg886.util.parentFile
import top.kagg886.util.sink
import top.kagg886.util.source
import java.nio.charset.StandardCharsets

actual val RSA: RSAPlatform by lazy {
    val name = when (jvmTarget) {
        JvmTarget.WINDOWS -> "security.dll"
        JvmTarget.LINUX -> "libsecurity.so"
        JvmTarget.MACOS -> "libsecurity.dylib"
    }

    val home = System.getProperty("user.home").toPath()
    val libPath = home / ".config" / "eoa" / "lib" / name
    if (!libPath.exists()) {
        useRes("/$name") { exportLibToPath(libPath) }
    } else {
        val newHash = useRes("/security.hash") { readString(StandardCharsets.UTF_8) }
        val oldHash = libPath.md5()
        if (newHash != oldHash) useRes("/$name") { exportLibToPath(libPath) }
    }
    System.load(libPath.absolutePath().toString())

    val internalRSA = NativeRSA()
    object : RSAPlatform {
        override fun encrypt(plaintext: String, exponent: String, modulus: String): String {
            return internalRSA.encrypt(plaintext, exponent, modulus)
        }
    }
}


internal inline fun <R> useRes(name: String, f: BufferedSource.() -> R) =
    RSAPlatform::class.java.getResourceAsStream(name)!!.source().buffer().use(f)

private fun BufferedSource.exportLibToPath(libPath: Path) {
    libPath.parentFile()!!.mkdirs()
    libPath.createNewFile()
    libPath.sink().use(::readAll)
}

fun Path.md5() = source().buffer().use { src ->
    HashingSink.md5(blackholeSink()).use { dst ->
        src.readAll(dst)
        dst.hash.hex().lowercase()
    }
}
