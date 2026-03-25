package top.kagg886.util

import okio.*
import okio.Path.Companion.toPath

fun Path.sink(append: Boolean = false): Sink = sink0(this, append)

internal fun sink0(path: Path, append: Boolean = false) = with(FileSystem.SYSTEM.openReadWrite(path)) {
    FileHandleSink(this, if (append) appendingSink() else sink(fileOffset = 0))
}

fun Path.absolutePath() = FileSystem.SYSTEM.canonicalize("".toPath()).resolve(this).normalized()

fun Path.exists() = FileSystem.SYSTEM.exists(this)
fun Path.mkdirs() = FileSystem.SYSTEM.createDirectories(this)

fun Path.delete() = FileSystem.SYSTEM.delete(this)

fun Path.createNewFile() = sink().close()

val Path.size
    get() = FileSystem.SYSTEM.metadata(this).size
fun Path.writeByteArray(data: ByteArray) = FileSystem.SYSTEM.sink(this, false).use {
    it.write(Buffer().write(data), data.size.toLong())
    it.flush()
}

internal class FileHandleSink(private val fileHandle: FileHandle, private val sink: Sink) : Sink by sink {
    override fun close() {
        sink.close()
        fileHandle.close()
    }
}
