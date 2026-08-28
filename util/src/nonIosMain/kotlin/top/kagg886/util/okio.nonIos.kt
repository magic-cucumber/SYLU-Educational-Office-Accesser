package top.kagg886.util
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

internal actual fun zip0(src: Path, dst: Path) {
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

    /*
     * src 可以 canonicalize，因为它已经存在。
     *
     * dst 不存在，不能直接 canonicalize(dst)，
     * 所以 canonicalize(parent) 后再拼接文件名。
     */
    val canonicalSrc = fs.canonicalize(src)
    val canonicalDst = fs.canonicalize(dstParent)
        .resolve(dst.name)
        .normalized()

    require(!canonicalDst.isChildOf(canonicalSrc)) {
        "dst cannot be inside src: src=$canonicalSrc, dst=$canonicalDst"
    }

    try {
        dst.sink()
            .buffer()
            .outputStream()
            .let(::ZipOutputStream)
            .use { zip ->
                val buffer = Buffer()

                fun addDirectory(directory: Path, relativePath: String) {
                    /*
                     * 为非根目录写入显式目录 Entry。
                     * 这样空目录也可以被保留下来。
                     */
                    if (relativePath.isNotEmpty()) {
                        zip.putNextEntry(
                            ZipEntry("$relativePath/")
                        )
                        zip.closeEntry()
                    }

                    for (child in fs.list(directory)) {
                        val childName = child.name
                        val childRelativePath =
                            if (relativePath.isEmpty()) {
                                childName
                            } else {
                                "$relativePath/$childName"
                            }

                        val metadata = fs.metadata(child)

                        if (metadata.isDirectory) {
                            addDirectory(
                                directory = child,
                                relativePath = childRelativePath,
                            )
                        } else if (metadata.isRegularFile) {
                            zip.putNextEntry(
                                ZipEntry(childRelativePath)
                            )

                            fs.source(child).use { source ->
                                while (true) {
                                    val read = source.read(
                                        buffer,
                                        64L * 1024L,
                                    )
                                    if (read == -1L) {
                                        break
                                    }

                                    buffer.writeTo(
                                        zip,
                                        read,
                                    )
                                }
                            }

                            zip.closeEntry()
                        }
                    }
                }

                /*
                 * 压缩 src 的内容，而不是把 src 自身作为
                 * ZIP 最外层目录。
                 *
                 * 例如：
                 *
                 * /tmp/foo/a.txt
                 * /tmp/foo/bar/b.txt
                 *
                 * ZIP:
                 * a.txt
                 * bar/
                 * bar/b.txt
                 */
                addDirectory(
                    directory = canonicalSrc,
                    relativePath = "",
                )
            }
    } catch (e: Throwable) {
        /*
         * 避免压缩失败后留下不完整的 zip。
         */
        if (dst.exists()) {
            runCatching {
                dst.delete()
            }
        }
        throw e
    }
}

private fun Path.isChildOf(parent: Path): Boolean {
    val current = parent

    while (true) {
        if (current == this) {
            return false
        }

        val next = this.relativeToOrNull(current)
            ?: return false

        return next.segments.isNotEmpty() &&
                next.segments.first() != ".."
    }
}

private fun Path.relativeToOrNull(base: Path): Path? {
    return runCatching {
        relativeTo(base)
    }.getOrNull()
}