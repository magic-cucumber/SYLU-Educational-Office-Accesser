@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package top.kagg886.eoa

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.height
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.kagg886.util.initializeMMKV

class ImageProcessingActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("ProduceStateDoesNotAssignValue")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeMMKV()
        FileKit.init(this)
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

            if (image == null) {
                return@setContent
            }

            val sheetState = rememberModalBottomSheetState(true)

            var showSheet by remember { mutableStateOf(true) }

            LaunchedEffect(showSheet) {
                if (!showSheet) {
                    finish()
                }
            }

            if (showSheet) {
                val configuration = with(LocalDensity.current) {
                    LocalWindowInfo.current.containerSize.height.toDp() * 0.8f
                }
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    sheetState = sheetState,
                ) {
                    ImageProcessingApp(
                        modifier = Modifier.height(configuration),
                        background = BottomSheetDefaults.ContainerColor,
                        todo = image!!,
                        exit = { finish() }
                    )
                }
            }
        }
    }
}
