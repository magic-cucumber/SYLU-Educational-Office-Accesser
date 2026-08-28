package top.kagg886.util

import okio.FileSystem
import okio.Path

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


    TODO("should be develop on macOS")
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