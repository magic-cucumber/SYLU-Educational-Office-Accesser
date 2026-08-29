@file:OptIn(CryptographyProviderApi::class, DelicateCryptographyApi::class)

package top.kagg886.eoa.pages.main.settings.feedback

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
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import okio.ByteString.Companion.toByteString
import org.orbitmvi.orbit.syntax.SubStateSyntax
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.util.BaseViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.Platform
import top.kagg886.util.asKtorLogger
import top.kagg886.util.http.HttpClient
import top.kagg886.util.current
import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class FeedbackModel : BaseViewModel<FeedbackState, FeedbackSideEffect>(
    name = "FeedbackModel",
    initial = FeedbackState.Loading
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
            json(contentType = ContentType.Text.Plain)
        }
    }

    private var prepared: PreparedFeedback? = null

    override suspend fun Syntax<FeedbackState, FeedbackSideEffect>.init() {
        addCloseable(client)

        if (AppSyncMMKV.profile == null) {
            postSideEffect(
                FeedbackSideEffect.Toast(
                    SnackBarType.Error,
                    "请等待同步成功后再使用。"
                )
            )
            delay(3.seconds)
            postSideEffect(FeedbackSideEffect.Close)
            return
        }

        val config = try {
            client.get(
                "https://${BuildConfig.MESSAGE_GITEE_HOST}/kagg886/sylu-educational-office-accesser/raw/master-4.0/runtime/crash.json"
            ).body<CrashConfig>()
        } catch (e: Exception) {
            logger.e("获取 crash.json 失败", e)
            postSideEffect(
                FeedbackSideEffect.Toast(
                    SnackBarType.Error,
                    "无法连接反馈服务，请稍后重试"
                )
            )
            delay(3.seconds)
            postSideEffect(FeedbackSideEffect.Close)
            return
        }

        val rsa = try {
            val cipher = CryptographyProvider.Default.get(RSA.PKCS1)
            val jwk = JsonWebKeys.encodeRsaPublicKey(
                algorithmId = RSA.PKCS1,
                digest = null,
                n = Base64.decode(config.modulus),
                e = Base64.decode(config.exponent),
            )
            cipher.publicKeyDecoder(SHA512).decodeFromByteArrayBlocking(
                format = RSA.PublicKey.Format.JWK,
                bytes = jwk,
            )
        } catch (e: Exception) {
            logger.e("构造反馈服务 RSA 公钥失败", e)
            postSideEffect(
                FeedbackSideEffect.Toast(
                    SnackBarType.Error,
                    "反馈服务目前不可用，请稍后再试"
                )
            )
            delay(3.seconds)
            postSideEffect(FeedbackSideEffect.Close)
            return
        }

        val report = HttpClient {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                url(config.server)
            }
            install(Logging) {
                logger = this@FeedbackModel.logger.asKtorLogger
                level = LogLevel.ALL
            }
        }
        try {
            val payload = ByteArray(128).apply {
                Random(Clock.System.now().toEpochMilliseconds()).nextBytes(this)
            }
            val response = report.post("/test") {
                contentType(ContentType.Application.Json)
                setBody("payload" to Base64.encode(rsa.encryptor().encrypt(payload)))
            }
            if (!response.status.isSuccess()) {
                error("服务端握手失败：${response.status}")
            }
            val data = response.body<FeedbackBaseResponse<String>>()
            if (!data.success) {
                error("服务端握手失败：${data.message}")
            }
            if (data.data != Base64.encode(payload)) {
                error("服务端握手校验失败")
            }
        } catch (e: Exception) {
            logger.e("反馈服务握手失败", e)
            postSideEffect(
                FeedbackSideEffect.Toast(
                    SnackBarType.Error,
                    "反馈服务目前不可用，请稍后再试"
                )
            )
            delay(3.seconds)
            postSideEffect(FeedbackSideEffect.Close)
            return
        } finally {
            report.close()
        }

        prepared = PreparedFeedback(config, rsa)
        reduce { FeedbackState.Success() }
    }

    fun submit(message: String) = intent {
        runOn<FeedbackState.Success> {
            logger.i("准备提交。内容\n$message")
            submitUnsafe(message)
        }
    }

    private suspend fun SubStateSyntax<FeedbackState, FeedbackSideEffect, FeedbackState.Success>.submitUnsafe(
        message: String
    ) {
        if (state.isSubmitting) return
        if (message.isBlank()) {
            postSideEffect(
                FeedbackSideEffect.Toast(
                    SnackBarType.Warning,
                    "反馈内容不能为空"
                )
            )
            return
        }

        reduce { state.copy(isSubmitting = true) }
        try {
            val url = submitFeedback(message)
            postSideEffect(
                FeedbackSideEffect.Toast(
                    SnackBarType.Success,
                    "反馈已提交，请收藏即将打开的链接。"
                )
            )
            delay(3.seconds)
            postSideEffect(FeedbackSideEffect.OpenUrl(url))
            postSideEffect(FeedbackSideEffect.Close)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e("提交反馈失败: ${e.message}", e)
            postSideEffect(
                FeedbackSideEffect.Toast(
                    SnackBarType.Error,
                    e.message ?: "提交反馈失败，请稍后重试"
                )
            )
        } finally {
            reduce {
                state.copy(isSubmitting = false)
            }
        }
    }

    private suspend fun submitFeedback(message: String): String {
        val prepared = prepared ?: error("反馈服务尚未准备完成")

        val aes = CryptographyProvider.Default.get(AES.CBC)
            .keyGenerator(256.bits)
            .generateKey()

        val report = HttpClient {
            install(ContentNegotiation) {
                json()
            }
            defaultRequest {
                url(prepared.config.server)
            }

            install(Logging) {
                logger = this@FeedbackModel.logger.asKtorLogger
                level = LogLevel.ALL
            }
        }

        try {
            val tokenResponse = report.put("/report") {
                contentType(ContentType.Application.Json)
                setBody(
                    ReportTokenGenerateRequest(
                        cipher = Base64.encode(
                            prepared.rsa.encryptor()
                                .encrypt(aes.encodeToByteArray(AES.Key.Format.RAW))
                        ),
                        deviceId = AppSyncMMKV.profile!!.avatar.toByteString().sha256().base64()
                    )
                )
            }
            if (!tokenResponse.status.isSuccess()) {
                error("我们无法处理您的反馈：${tokenResponse.status}")
            }
            val token = tokenResponse.body<FeedbackBaseResponse<String>>().let {
                if (!it.success) error("我们无法处理您的反馈：${it.message}")
                it.data ?: error("我们无法处理您的反馈")
            }

            val response = report.post("/feedback") {
                contentType(ContentType.Application.Json)
                setBody(
                    FeedbackRequest(
                        content = message,
                        token = token,
                        system = Platform.current.toString(),
                        version = BuildConfig.APP_VERSION_NAME,
                    )
                )
            }
            if (!response.status.isSuccess()) {
                error("我们无法处理您的反馈：${response.status}")
            }
            return response.body<FeedbackBaseResponse<String>>().let {
                if (!it.success) error(it.message ?: "当前暂时无法提交反馈，请稍后再试~")
                it.data ?: error("当前暂时无法提交反馈，请稍后再试~")
            }
        } finally {
            report.close()
        }
    }
}

@Serializable
private data class FeedbackRequest(
    val content: String,
    val token: String,
    val system: String,
    val version: String,
)

@Serializable
private data class CrashConfig(
    val server: String,
    val modulus: String,
    val exponent: String,
)

@Serializable
private data class ReportTokenGenerateRequest(
    val cipher: String,
    val deviceId: String,
)

@Serializable
private data class FeedbackBaseResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
)

private data class PreparedFeedback(
    val config: CrashConfig,
    val rsa: RSA.PKCS1.PublicKey,
)

sealed interface FeedbackState {
    data object Loading : FeedbackState
    data class Success(val isSubmitting: Boolean = false) : FeedbackState
}

sealed interface FeedbackSideEffect {
    data class Toast(val type: SnackBarType, val message: String) : FeedbackSideEffect
    data class OpenUrl(val url: String) : FeedbackSideEffect
    data object Close : FeedbackSideEffect
}
