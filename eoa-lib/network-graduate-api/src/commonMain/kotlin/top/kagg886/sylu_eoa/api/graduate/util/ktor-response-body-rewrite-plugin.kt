@file:OptIn(
    CryptographyProviderApi::class,
    DelicateCryptographyApi::class,
    InternalAPI::class,
)

package top.kagg886.sylu_eoa.api.graduate.util

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.CryptographyProviderApi
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import io.ktor.client.call.replaceResponse
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlin.io.encoding.Base64

private const val AES_KEY = "southsoft12345!#"
private const val SPECIAL_VALUE = "6Bw7oUSiTq2Yi0GzxTPJwg=="

/**
 * Decrypts graduate API responses before the logging plugin sees them.
 *
 * Responses that are not encrypted are passed through unchanged. The response
 * body is read once here and replaced with a replayable channel so downstream
 * plugins and callers can still consume it.
 */
internal val ResponseBodyRewritePlugin: ClientPlugin<Unit> =
    createClientPlugin(name = "GraduateResponseBodyRewritePlugin") {
        val aesKey = lazy {
            CryptographyProvider.Default
                .get(AES.ECB)
                .keyDecoder()
                .decodeFromByteArrayBlocking(AES.Key.Format.RAW, AES_KEY.encodeToByteArray())
        }

        client.receivePipeline.intercept(HttpReceivePipeline.State) {
            val originalBody = subject.rawContent.readRemaining().readByteArray()
            val rewrittenBody = decryptResponse(originalBody, aesKey)
            val body = rewrittenBody ?: originalBody
            val headers = if (rewrittenBody == null) {
                subject.headers
            } else {
                subject.headers.withContentLength(body.size)
            }

            proceedWith(
                subject.call.replaceResponse(headers = headers) { ByteReadChannel(body) }.response
            )
        }
    }

private fun decryptResponse(body: ByteArray, aesKey: Lazy<AES.ECB.Key>): ByteArray? {
    val response = body.decodeToString()
    if (response == SPECIAL_VALUE) {
        return "-".encodeToByteArray()
    }

    return runCatching {
        aesKey.value.cipher(padding = true)
            .decryptBlocking(Base64.decode(response))
            .decodeToString()
            .takeUnless(String::isEmpty)
            ?.encodeToByteArray()
    }.getOrNull()
}

private fun Headers.withContentLength(length: Int): Headers = Headers.build {
    appendAll(this@withContentLength)
    remove(HttpHeaders.ContentLength)
    set(HttpHeaders.ContentLength, length.toString())
}
