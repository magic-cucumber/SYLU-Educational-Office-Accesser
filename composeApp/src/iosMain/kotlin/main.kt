@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.ComposeUIViewController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.toByteString
import org.jetbrains.skia.Image
import platform.Foundation.*
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTTypeImage
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.databaseBuilder
import top.kagg886.eoa.App
import top.kagg886.eoa.ImageProcessingApp
import top.kagg886.eoa.LocalDatabase
import top.kagg886.eoa.installCoilConfig
import top.kagg886.eoa.rememberDeepLinkController
import top.kagg886.eoa.util.registerKermitLoggerIfExists
import top.kagg886.report.CrashApp
import top.kagg886.report.CrashConfig
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.initializeMMKV
import top.kagg886.util.logger
import kotlin.coroutines.resume
import kotlin.experimental.ExperimentalNativeApi

private val logger = "MainViewController".asTaggedLogger

@OptIn(ExperimentalForeignApi::class)
private fun handleObjectiveCException(exception: NSException?) {
    if (exception == null) {
        return
    }
    val error = buildString {
        appendLine("NSException: ${exception.name}")
        appendLine("Reason: ${exception.reason.orEmpty()}")
        appendLine("Call stack:")
        append(exception.callStackSymbols.joinToString("\n"))
    }
    logger.a { error }
    CrashConfig.hasUnResolveCrash = true
    CrashConfig.crashText = error
}

fun createEmptyFlow(): MutableSharedFlow<String?> = MutableSharedFlow(
    replay = 1,
    extraBufferCapacity = 16
)

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@Suppress("unused")
fun MainViewController(deepLinkFlow: MutableSharedFlow<String?> = createEmptyFlow()): UIViewController {
    val database: AppDatabase = databaseBuilder().build()
    registerKermitLoggerIfExists(database.appLogDao())

    initializeMMKV()

    setUnhandledExceptionHook {
        logger.a(it) { "App crashed" }
        CrashConfig.hasUnResolveCrash = true
        CrashConfig.crashText = it.stackTraceToString()
    }

    NSSetUncaughtExceptionHandler(staticCFunction(::handleObjectiveCException))

    UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .flatMap { it.windows.filterIsInstance<platform.UIKit.UIWindow>() }
        .firstOrNull { it.isKeyWindow() }
        ?.layer
        ?.speed = AppSettingsMMKV.animationSpeed

    val controller = ComposeUIViewController {
        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .installCoilConfig()
                .build()
        }

        val controller = rememberDeepLinkController()

        var hasUnResolveCrashInfo by remember {
            mutableStateOf(CrashConfig.hasUnResolveCrash)
        }

        if (hasUnResolveCrashInfo) {
            CrashApp(
                database = database,
                error = CrashConfig.crashText,
                onRestart = {
                    CrashConfig.hasUnResolveCrash = false
                    hasUnResolveCrashInfo = false
                }
            )

            return@ComposeUIViewController
        }

        LaunchedEffect(deepLinkFlow) {
            deepLinkFlow.collect { v ->
                controller.handleDeepLink(v)
            }
        }


        CompositionLocalProvider(LocalDatabase provides database) {
            App(controller)
        }
    }

    return controller
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("unused")
fun ImageProcessingViewController(item: NSExtensionItem, exit: () -> Unit): UIViewController =
    ComposeUIViewController {
        initializeMMKV()
        val image by produceState(Result.failure(Exception("No image"))) {
            val providers =
                item.attachments?.filterIsInstance<NSItemProvider>() ?: return@produceState
            val provider =
                providers.firstOrNull { it.hasItemConformingToTypeIdentifier(UTTypeImage.identifier) }
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
                        this.logger.i("URL: $item")
                        memScoped {
                            val error = alloc<ObjCObjectVar<NSError?>>()
                            val result = NSData.dataWithContentsOfURL(
                                unknownResult,
                                NSDataReadingMappedIfSafe,
                                error.ptr
                            )

                            if (error.value != null) {
                                value =
                                    Result.failure(Exception(error.value!!.localizedDescription))
                                return@withContext null
                            }

                            result
                        }
                    }

                    is UIImage -> {
                        this.logger.i("Image: $item")
                        UIImagePNGRepresentation(unknownResult)
                    }

                    is NSData -> {
                        this.logger.i("Data: $item")
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
