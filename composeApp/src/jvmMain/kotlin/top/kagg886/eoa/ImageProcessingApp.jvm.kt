package top.kagg886.eoa

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.Image

internal actual suspend fun ImageBitmap.toByteArray(): ByteArray = Image.makeFromBitmap(this.asSkiaBitmap()).encodeToData()!!.bytes
