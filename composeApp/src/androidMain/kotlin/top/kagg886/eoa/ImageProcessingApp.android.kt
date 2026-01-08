package top.kagg886.eoa

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

internal actual suspend fun ImageBitmap.toByteArray(): ByteArray = ByteArrayOutputStream().apply {
    this@toByteArray.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, this)
}.toByteArray()
