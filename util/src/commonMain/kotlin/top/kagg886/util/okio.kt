package top.kagg886.util

import no.synth.kmpzip.okio.zipTo
import okio.*
import okio.Path.Companion.toPath

fun Path.sink(append: Boolean = false): Sink = sink0(this, append)
fun Path.source(): Source = FileSystem.SYSTEM.source(this)

fun Path.metadata(): FileMetadata = FileSystem.SYSTEM.metadata(this)

internal fun sink0(path: Path, append: Boolean = false) =
    with(FileSystem.SYSTEM.openReadWrite(path)) {
        FileHandleSink(this, if (append) appendingSink() else sink(fileOffset = 0))
    }

fun Path.absolutePath() = FileSystem.SYSTEM.canonicalize("".toPath()).resolve(this).normalized()

fun Path.exists() = FileSystem.SYSTEM.exists(this)
fun Path.mkdirs() = FileSystem.SYSTEM.createDirectories(this,true)

fun Path.delete() = FileSystem.SYSTEM.deleteRecursively(this)

fun Path.createNewFile() = sink().close()
fun Path.write(action: BufferedSink.() -> Unit) = FileSystem.SYSTEM.write(this, writerAction =  action)

infix fun Path.copyTo(path: Path) =
    FileSystem.SYSTEM.copy(this,path)
internal class FileHandleSink(private val fileHandle: FileHandle, private val sink: Sink) :
    Sink by sink {
    override fun close() {
        sink.close()
        fileHandle.close()
    }
}

suspend infix fun Path.zip(target: Path) =
    zip0(this, target)

internal suspend fun zip0(src: Path, dst: Path) {
    val fs = FileSystem.SYSTEM

    require(src.isAbsolute) {
        "src must be an absolute path: $src"
    }
    require(dst.isAbsolute) {
        "dst must be an absolute path: $dst"
    }
    require(dst.name.endsWith(".zip", ignoreCase = true)) {
        "dst must end with .zip: $dst"
    }
    require(!dst.exists()) {
        "dst already exists: $dst"
    }

    val dstParent = requireNotNull(dst.parent) {
        "dst must have a parent directory: $dst"
    }

    require(dstParent.exists()) {
        "dst parent does not exist: $dstParent"
    }
    require(fs.metadata(dstParent).isDirectory) {
        "dst parent is not a directory: $dstParent"
    }
    require(src.exists()) {
        "src does not exist: $src"
    }
    require(fs.metadata(src).isDirectory) {
        "src must be a directory: $src"
    }

    val canonicalSrc = fs.canonicalize(src)
    val canonicalDst = fs.canonicalize(dstParent)
        .resolve(dst.name)
        .normalized()

    require(!canonicalDst.isChildOf(canonicalSrc)) {
        "dst cannot be inside src: src=$canonicalSrc, dst=$canonicalDst"
    }

    try {
        fs.zipTo(
            target = dst,
            // 传入 src 的子项，避免把 src 自身作为 ZIP 最外层目录。
            sources = fs.list(canonicalSrc),
        )
    } catch (e: Throwable) {
        if (dst.exists()) {
            runCatching { dst.delete() }
        }
        throw e
    }
}

private fun Path.isChildOf(parent: Path): Boolean {
    val relative = runCatching { relativeTo(parent) }.getOrNull() ?: return false
    return this != parent &&
            relative.segments.isNotEmpty() &&
            relative.segments.first() != ".."
}
