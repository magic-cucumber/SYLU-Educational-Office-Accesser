import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.ComposeUIViewController
import co.touchlab.kermit.Severity
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSDataReadingMappedIfSafe
import platform.Foundation.NSError
import platform.Foundation.NSExtensionItem
import platform.Foundation.NSItemProvider
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeImage
import top.kagg886.eoa.App
import top.kagg886.eoa.ImageProcessingApp
import top.kagg886.eoa.installCoilConfig
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVOptions
import top.kagg886.mkmb.initializeWithMultiProcess
import top.kagg886.util.dataPath
import top.kagg886.util.initializeMMKV
import top.kagg886.util.logger
import kotlin.coroutines.resume

@Suppress("unused")
fun MainViewController(): UIViewController = ComposeUIViewController {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .installCoilConfig()
            .build()
    }

    initializeMMKV()
    MMKV.initializeWithMultiProcess(dataPath.toString(), MMKVOptions().apply {
        logFunc = { level, tag, it ->
            logger.log(
                severity = when (level) {
                    MMKVOptions.LogLevel.Debug -> Severity.Debug
                    MMKVOptions.LogLevel.Info -> Severity.Info
                    MMKVOptions.LogLevel.Warning -> Severity.Warn
                    MMKVOptions.LogLevel.Error -> Severity.Error
                    MMKVOptions.LogLevel.None -> Severity.Assert
                },
                tag = "MMKV $tag",
                message = it,
                throwable = null
            )
        }
    })
    App()
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("unused")
fun ImageProcessingViewController(item: NSExtensionItem, exit: () -> Unit): UIViewController = ComposeUIViewController {
//    initializeMMKV()
    MMKV.initializeWithMultiProcess(dataPath.toString(), MMKVOptions().apply {
        logFunc = { level, tag, it ->
            logger.log(
                severity = when (level) {
                    MMKVOptions.LogLevel.Debug -> Severity.Debug
                    MMKVOptions.LogLevel.Info -> Severity.Info
                    MMKVOptions.LogLevel.Warning -> Severity.Warn
                    MMKVOptions.LogLevel.Error -> Severity.Error
                    MMKVOptions.LogLevel.None -> Severity.Assert
                },
                tag = "MMKV $tag",
                message = it,
                throwable = null
            )
        }
    })
    val image by produceState(Result.failure(Exception("No image"))) {
        val providers = item.attachments as? List<NSItemProvider> ?: return@produceState
        val provider = providers.firstOrNull { it.hasItemConformingToTypeIdentifier(UTTypeImage.identifier) }
            ?: return@produceState

        val unknownResult = withContext(Dispatchers.IO) {
            suspendCancellableCoroutine {
                provider.loadItemForTypeIdentifier(UTTypeImage.identifier, null) { data, err ->
                    if (err != null) {
                        value = Result.failure(Exception(err.localizedDescription))
                        it.resume(null)
                        return@loadItemForTypeIdentifier
                    }

                    it.resume(data!!)
                }
            }
        }

        if (unknownResult == null) {
            return@produceState
        }

        val imageBytes = withContext(Dispatchers.IO) {
            val data = when (unknownResult) {
                is NSURL -> {
                    logger.i("URL: $item")
                    memScoped {
                        val error = alloc<ObjCObjectVar<NSError?>>()
                        val result = NSData.dataWithContentsOfURL(unknownResult, NSDataReadingMappedIfSafe, error.ptr)

                        if (error.value != null) {
                            value = Result.failure(Exception(error.value!!.localizedDescription))
                            return@withContext null
                        }

                        result
                    }
                }

                is UIImage -> {
                    logger.i("Image: $item")
                    UIImagePNGRepresentation(unknownResult)
                }

                is NSData -> {
                    logger.i("Data: $item")
                    unknownResult
                }

                else -> {
                    value = Result.failure(Exception("unknown data type: $unknownResult"))
                    return@withContext null
                }
            }


            data?.toByteString()?.toByteArray()
        }

        if (imageBytes == null) {
            return@produceState
        }

        value = Result.success(
            Image.makeFromEncoded(imageBytes).toComposeImageBitmap()
        )
    }

    if (image.isFailure && image.exceptionOrNull()?.message == "No image") {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    if (image.isFailure) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("无法转换图片。${image.exceptionOrNull()?.message ?: "位置原因"}")
        }
        return@ComposeUIViewController
    }

    ImageProcessingApp(
        modifier = Modifier.fillMaxSize(),
        todo = image.getOrThrow(),
        exit = exit
    )
}
