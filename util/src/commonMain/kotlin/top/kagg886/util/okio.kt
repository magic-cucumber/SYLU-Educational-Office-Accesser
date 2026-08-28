package top.kagg886.util

import okio.*
import okio.Path.Companion.toPath

fun Path.sink(append: Boolean = false): Sink = sink0(this, append)
fun Path.source(): Source = FileSystem.SYSTEM.source(this)

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

infix fun Path.zip(target: Path) =
    zip0(this, target)

internal expect fun zip0(src: Path, dst: Path)
