package top.kagg886.sylu_eoa.api.html.util

import io.ktor.client.call.*
import io.ktor.client.plugins.api.*
import io.ktor.client.request.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString
import top.kagg886.util.asTaggedLogger


internal class RequestMergePluginConfig {
    var fingerprint: HttpRequestData.() -> String = HttpRequestData::fingerprint

    fun fingerprint(block: HttpRequestData.() -> String) {
        this.fingerprint = block
    }
}

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/7 22:43
 * ================================================
 */
internal val RequestMergePlugin = createClientPlugin(
    name = "RequestMergePlugin",
    createConfiguration = ::RequestMergePluginConfig
) {
    val config = pluginConfig
    val logger = "RequestMergePlugin".asTaggedLogger
    val cache = mutableMapOf<String, Deferred<HttpClientCall>>()
    val lock = Mutex()

    suspend fun Send.Sender.intercept(it: HttpRequestBuilder): HttpClientCall {
        val fingerprint = config.fingerprint(it.build())
        val deferred = lock.withLock {
            cache.getOrPut(fingerprint) {
                async {
                    logger.d("start to proceed request: $fingerprint (${it.url.build()})")

                    //this response should be read more times, so we must wrap it.
                    val data = proceed(it).save()

                    lock.withLock {
                        logger.d("execute complete, now let's remove deferred from cache")
                        cache.remove(fingerprint)
                    }

                    data
                }
            }
        }
        val result = runCatching {
            logger.d("wait $fingerprint")
            deferred.await()
        }

        lock.withLock {
            cache.remove(fingerprint)
        }

        return result.getOrThrow()
    }

    on(Send, Send.Sender::intercept)
}

@OptIn(ExperimentalStdlibApi::class)
private fun HttpRequestData.fingerprint(): String {
    val data = ByteString.of(
        data = """
            ${this.url}
            ${this.method.value}
            ${this.headers.entries().toList().sortedBy { it.key }.joinToString { "${it.key} --> ${it.value}" }}
            ${this.body}
        """.trimIndent().toByteArray()
    )
    return data.md5().toByteArray().toHexString()
}
