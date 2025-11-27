package top.kagg886.util.http


import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.utils.io.*

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/11/27 13:17
 * ================================================
 */

@KtorDsl
expect fun HttpClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient
