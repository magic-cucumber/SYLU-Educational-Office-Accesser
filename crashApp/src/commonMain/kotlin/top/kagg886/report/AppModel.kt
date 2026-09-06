@file:OptIn(CryptographyProviderApi::class, DelicateCryptographyApi::class)

package top.kagg886.report

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.CryptographyProviderApi
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA512
import dev.whyoleg.cryptography.providers.base.materials.JsonWebKeys
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.okio.asKotlinxIoRawSink
import kotlinx.io.okio.asKotlinxIoRawSource
import kotlinx.serialization.Serializable
import okio.ByteString.Companion.toByteString
import okio.Path.Companion.toPath
import okio.use
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import top.kagg886.backend.config.AppExportMMKV
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.databasePath
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.util.Platform
import top.kagg886.util.asKtorLogger
import top.kagg886.util.cachePath
import top.kagg886.util.copyTo
import top.kagg886.util.createNewFile
import top.kagg886.util.current
import top.kagg886.util.delete
import top.kagg886.util.http.HttpClient
import top.kagg886.util.metadata
import top.kagg886.util.mkdirs
import top.kagg886.util.sink
import top.kagg886.util.source
import top.kagg886.util.write
import top.kagg886.util.zip
import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.uuid.Uuid

class AppModel(private val database: AppDatabase, private val crash: String) : ViewModel(),
    OrbitContainerHost<AppModelState, AppModelState, Unit> {
    private val logger = Logger.withTag("CrashAppModel")
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
            json(contentType = ContentType.Text.Plain)
        }

        install(Logging) {
            logger = this@AppModel.logger.asKtorLogger
            level = LogLevel.ALL
        }
    }

    override val container: OrbitContainer<AppModelState, AppModelState, Unit> =
        orbitContainer(AppModelState.Initializing) {
            addCloseable(client)

            if (AppSyncMMKV.profile == null) {
                reduce { AppModelState.CrashManually }
                return@orbitContainer
            }


            //取配置
            val config = try {
                //
                client.get("https://${BuildConfig.MESSAGE_GITEE_HOST}/kagg886/sylu-educational-office-accesser/raw/master-4.0/runtime/crash.json")
                    .body<CrashConfig>()
            } catch (e: Exception) {
                logger.e("failed to found crash.json", e)
                reduce { AppModelState.CrashManually }
                return@orbitContainer
            }

            val progress = MutableStateFlow<Float?>(null)
            val label = MutableStateFlow("正在寻找可用的方案...")
            val success = MutableStateFlow(false)
            reduce { AppModelState.CrashAutoUpload(progress, label, success) }

            //构造rsa参数
            val rsa = try {
                val cipher = CryptographyProvider.Default.get(RSA.PKCS1)
                val jwk = JsonWebKeys.encodeRsaPublicKey(
                    algorithmId = RSA.PKCS1,
                    digest = null,
                    n = Base64.decode(config.modulus),
                    e = Base64.decode(config.exponent),
                )

                val pubKey = cipher.publicKeyDecoder(SHA512).decodeFromByteArrayBlocking(
                    format = RSA.PublicKey.Format.JWK,
                    bytes = jwk,
                )

                pubKey
            } catch (e: Exception) {
                logger.e("failed to construct RSA encryptor", e)
                reduce { AppModelState.CrashManually }
                return@orbitContainer
            }

            //构造aes参数
            val aes = try {
                val cipher = CryptographyProvider.Default.get(AES.CBC)
                cipher.keyGenerator(256.bits).generateKey()
            } catch (e: Exception) {
                logger.e("failed to construct AES encryptor", e)
                reduce { AppModelState.CrashManually }
                return@orbitContainer
            }

            //构造上报客户端
            val report = run {
                val client = HttpClient {
                    install(ContentNegotiation) {
                        json()
                    }

                    defaultRequest {
                        url(config.server)
                    }

                    install(Logging) {
                        logger = this@AppModel.logger.asKtorLogger
                        level = LogLevel.ALL
                    }
                }
                addCloseable(client)
                client
            }

            /**
             * 使用 /test 路由测试rsa链路是否通顺。
             * - 服务器error返回server returned unsuccessfully status code
             * - 服务器无法解密返回server can't continue handshake.
             * - 服务器返回错误结果返回handshake failed.
             */
            try {
                label.emit("正在连接服务...")
                val payload = ByteArray(128).apply {
                    Random(
                        Clock.System.now().toEpochMilliseconds()
                    ).nextBytes(this)
                }
                val resp = report.post("/test") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        EncryptedPayload(
                            first = "payload",
                            second = Base64.encode(rsa.encryptor().encrypt(payload)),
                        )
                    )
                }

                if (!resp.status.isSuccess()) error("server returned unsuccessfully status code: ${resp.status}")

                val data = resp.body<BaseResponse<String>>()
                if (!data.success) error("server can't continue handshake: ${data.message}")
                if (!data.data.equals(Base64.encode(payload))) {
                    error("handshake failed.")
                }
            } catch (e: Exception) {
                logger.e("failed to handshake to report server.", e)
                reduce { AppModelState.CrashManually }
                return@orbitContainer
            }

            //开始正式握手了，首先加密AES密钥送到服务端获取一次性token

            label.emit("正在提交问题信息...")
            progress.emit(0f)

            val token = try {
                val resp = report.put("/report") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ReportTokenGenerateRequest(
                            cipher = Base64.encode(
                                rsa.encryptor()
                                    .encrypt(aes.encodeToByteArray(AES.Key.Format.RAW))
                            ),
                            deviceId = AppSyncMMKV.profile!!.avatar.toByteString().sha256().base64()
                        )
                    )
                }
                if (!resp.status.isSuccess()) error("server returned unsuccessfully status code: ${resp.status}")

                val data = resp.body<BaseResponse<String>>()
                if (!data.success) error("server can't continue generate report token: ${data.message}")

                data.data!!
            } catch (e: Exception) {
                logger.e("failed to generate report token to report server.", e)
                reduce { AppModelState.CrashManually }
                return@orbitContainer
            }

            //准备崩溃报告。
            val (id, zip) = withContext(Dispatchers.IO) {
                val id = Uuid.random().toHexString()
                val root = cachePath.resolve(id)
                root.mkdirs()

                with(root.resolve("summary.txt")) {
                    createNewFile()
                    write { writeUtf8(crash) }
                }

                with(root.resolve("mmkv.toml")) {
                    createNewFile()
                    write {
                        writeUtf8(
                            buildString {
                                appendLine("[app-settings]")
                                appendLine("theme=${AppSettingsMMKV.theme}")
                                appendLine("color=${AppSettingsMMKV.color}")
                                appendLine("system-widget-radius=${AppSettingsMMKV.systemWidgetRadius}")
                                appendLine("show-experiment-class=${AppSettingsMMKV.showExperimentClass}")
                                appendLine("hide-weekend-course=${AppSettingsMMKV.hideWeekendCourse}")
                                appendLine("animation-speed=${AppSettingsMMKV.animationSpeed}")
                                appendLine("duration=${AppSettingsMMKV.syncDuration}")
                                appendLine("home-module=${AppSettingsMMKV.homeModule}")

                                appendLine()
                                appendLine("[initialize-setting]")
                                appendLine("initialize=${AppInitializeMMKV.initialize}")
                                appendLine("calendarId=${AppInitializeMMKV.calendarId}")
                                appendLine("size=${AppInitializeMMKV.size}")
                                appendLine("offset=${AppInitializeMMKV.offset}")
                                appendLine("announce=${AppInitializeMMKV.announce}")
                                appendLine("link=${AppInitializeMMKV.link}")
                                appendLine("tutorial-summary=${AppInitializeMMKV.tutorialSummary}")
                                appendLine("tutorial-course-list=${AppInitializeMMKV.tutorialCourseList}")
                                appendLine("tutorial-course-manage=${AppInitializeMMKV.tutorialCourseManage}")
                                appendLine("tutorial-exam-list=${AppInitializeMMKV.tutorialExamList}")
                                appendLine("tutorial-second-class-login=${AppInitializeMMKV.tutorialSecondClassLogin}")
                                appendLine("tutorial-ai-settings=${AppInitializeMMKV.tutorialAISettings}")

                                appendLine()
                                appendLine("[login-properties]")
                                appendLine("username=***")
                                appendLine("password=***")
                                appendLine("session-key=***")
                                appendLine("client-id=${AppLoginPropertiesMMKV.clientId}")

                                appendLine()
                                appendLine("[second-class]")
                                appendLine("vpn-password=***")
                                appendLine("tw-password=***")

                                appendLine()
                                appendLine("[app-sync-config]")
                                appendLine("profile=***")
                                appendLine("picker=${AppSyncMMKV.picker}")
                                appendLine("school-calender=${AppSyncMMKV.calender}")

                                appendLine()
                                appendLine("[class-export-settings]")
                                appendLine("columns=${AppExportMMKV.columns}")
                                appendLine("selected=${AppExportMMKV.selected}")
                            }
                        )
                    }
                }

                with(root.resolve("app.db")) {
                    databasePath.toPath() copyTo this
                }

                with(root.resolve("platform")) {
                    createNewFile()
                    write {
                        writeUtf8(
                            buildString {
                                appendLine("Application Info:")
                                appendLine("    Platform: ${Platform.current}")
                                appendLine("    Version: ${BuildConfig.APP_VERSION_NAME}(${BuildConfig.APP_VERSION_CODE})")

                                with(Platform.current) {
                                    if (this is Platform.Android) {
                                        appendLine("    Desugaring: ${BuildConfig.APP_DESUGAR_ENABLED}")
                                    }
                                }

                            }
                        )
                    }
                }

                //压缩并删除源文件
                val dst = cachePath.resolve(".${id}.zip")
                root.zip(dst)
                root.delete()

                //加密并删除源文件
                val dst1 = cachePath.resolve("${id}.bin")
                dst1.createNewFile()

                dst.source().use { i ->
                    val i = aes.cipher(true).encryptingSource(i.asKotlinxIoRawSource())
                    val o = dst1.sink()

                    o.use { o ->
                        i.buffered().transferTo(o.asKotlinxIoRawSink())
                        o.flush()
                    }
                }

                dst.delete()

                id to dst1
            }


            //然后开始上传文件。
            try {
                val resp = report.submitFormWithBinaryData(
                    url = "/report",
                    formData = formData {
                        append("token", token)
                        append(
                            key = "file",
                            value = InputProvider {
                                zip.source().asKotlinxIoRawSource().buffered()
                            },
                            Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"${id}.bin\""
                                )
                                append(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.OctetStream.toString()
                                )
                                append(
                                    HttpHeaders.ContentLength,
                                    zip.metadata().size.toString()
                                )
                            }
                        )
                    }
                ) {
                    onUpload { bytesSentTotal, contentLength ->
                        if (contentLength != null) {
                            val pg = bytesSentTotal.toFloat() / contentLength
                            logger.d("uploading... ${pg * 100}%($bytesSentTotal / $contentLength)")
                            progress.emit(pg)
                        }
                    }
                }

                if (!resp.status.isSuccess()) error("server returned unsuccessfully status code: ${resp.status}, file size is ${zip.metadata().size}, body: ${resp.body<String>()}")

                val data = resp.body<BaseResponse<String>>()
                if (!data.success) error("server can't continue upload: ${data.message}")

            } catch (e: Exception) {
                logger.e("failed to upload file.", e)
                reduce { AppModelState.CrashManually }
                return@orbitContainer
            } finally {
                zip.delete()
            }

            success.emit(true)
            label.emit("问题信息已提交，请重新启动应用。")
        }

}


sealed interface AppModelState {
    data object Initializing : AppModelState
    data object CrashManually : AppModelState
    data class CrashAutoUpload(
        val progress: MutableStateFlow<Float?>,
        val label: MutableStateFlow<String>,
        val success: MutableStateFlow<Boolean>
    ) : AppModelState
}


@Serializable
private data class CrashConfig(
    val server: String,
    val modulus: String,
    val exponent: String,
)

@Serializable
private data class BaseResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

@Serializable
private data class ReportTokenGenerateRequest(
    val cipher: String,
    val deviceId: String,
)

@Serializable
private data class EncryptedPayload(
    val first: String,
    val second: String,
)
