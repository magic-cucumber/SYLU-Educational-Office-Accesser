package top.kagg886.eoa

import android.R.attr.bitmap
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageProcessingActivity : ComponentActivity() {

    @SuppressLint("ProduceStateDoesNotAssignValue")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val image by produceState<ImageBitmap?>(null) {
                val uri = when {
                    intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true ->
                        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

                    else -> null
                }

                if (uri == null) {
                    value = null
                    return@produceState
                }
                val bitmap = withContext(Dispatchers.IO) {
                    this@ImageProcessingActivity.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }

                value = bitmap?.asImageBitmap()
            }

            Content(image)
        }
    }

    @Composable
    fun Content(image: ImageBitmap?) = when {
        image != null -> ImageProcessingApp(image)
        else -> Unit
    }
}
