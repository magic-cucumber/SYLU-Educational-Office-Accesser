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
    App()
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("unused")
fun ImageProcessingViewController(item: NSExtensionItem): UIViewController = ComposeUIViewController {
    initializeMMKV()
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

    ImageProcessingApp(image.getOrThrow())
}

//private func handleSharedImage() {
//        // 2. 获取 Extension Context 中的输入项
//        guard let extensionItems = extensionContext?.inputItems as? [NSExtensionItem] else { return }
//
//        for item in extensionItems {
//            guard let providers = item.attachments else { continue }
//
//            for provider in providers {
//                // 3. 检查是否包含图片类型 (public.image)
//                if provider.hasItemConformingToTypeIdentifier(UTType.image.identifier) {
//
//                    // 4. 异步加载图片
//                    provider.loadItem(forTypeIdentifier: UTType.image.identifier, options: nil) { [weak self] (item, error) in
//                        guard let self = self else { return }
//
//                        if let error = error {
//                            print("加载出错: \(error.localizedDescription)")
//                            return
//                        }
//
//                        // 处理加载到的数据 (可能是 URL 或 UIImage 对象)
//                        self.processLoadedItem(item)
//                    }
//
//                    //以此为例，我们只处理找到的第一张图片，找到后直接返回
//                    return
//                }
//            }
//        }
//    }
//
//    private func processLoadedItem(_ item: NSSecureCoding?) {
//        // 因为 loadItem 回调是在后台线程，UI 更新必须回到主线程
//        DispatchQueue.main.async {
//            if let url = item as? URL {
//                // 情况 A: 分享的是一个文件路径 (URL)
//                // 从磁盘读取图片
//                if let data = try? Data(contentsOf: url) {
//                    self.imageView.image = UIImage(data: data)
//                }
//            } else if let image = item as? UIImage {
//                // 情况 B: 分享的直接是 UIImage 对象 (比如截屏)
//                self.imageView.image = image
//            } else if let data = item as? Data {
//                // 情况 C: 分享的是二进制 Data
//                self.imageView.image = UIImage(data: data)
//            }
//        }
//    }
