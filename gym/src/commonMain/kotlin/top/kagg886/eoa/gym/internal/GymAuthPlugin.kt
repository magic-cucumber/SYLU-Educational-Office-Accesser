package top.kagg886.eoa.gym.internal

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import top.kagg886.eoa.gym.GymException

internal val SkipGymAuth = AttributeKey<Unit>("SkipGymAuth")

internal class GymAuthPluginConfig {
    lateinit var token: () -> String?
    lateinit var login: suspend () -> Unit
}

internal val GymAuthPlugin = createClientPlugin("GymAuthPlugin", ::GymAuthPluginConfig) {
    val token = pluginConfig.token
    val login = pluginConfig.login
    val mutex = Mutex()
    var generation = 0L

    on(Send) { request ->
        val skipAuth = request.attributes.contains(SkipGymAuth)
        // 重新登录的请求不参与自动认证，避免递归刷新。
        if (skipAuth) {
            val call = proceed(request)
            if (call.response.headers["abnormal"] == "TIMEOUT") {
                throw GymException("Session expired during login")
            }
            return@on call
        }

        val (sentToken, sentGeneration) = mutex.withLock { token() to generation }
        request.headers.remove(HttpHeaders.Authorization)
        sentToken?.let { request.headers.append(HttpHeaders.Authorization, it) }

        val call = proceed(request)
        if (call.response.headers["abnormal"] != "TIMEOUT") return@on call

        val refreshedToken = mutex.withLock {
            // 同一批过期请求只刷新一次，即便服务端重新发放相同 token。
            if (generation == sentGeneration && token() == sentToken) {
                login()
                generation++
            }
            token()
        }

        val retry = HttpRequestBuilder().takeFrom(request)
        retry.headers.remove(HttpHeaders.Authorization)
        refreshedToken?.let { retry.headers.append(HttpHeaders.Authorization, it) }
        val retriedCall = proceed(retry)
        if (retriedCall.response.headers["abnormal"] == "TIMEOUT") {
            throw GymException("Session expired after re-login")
        }
        retriedCall
    }
}
