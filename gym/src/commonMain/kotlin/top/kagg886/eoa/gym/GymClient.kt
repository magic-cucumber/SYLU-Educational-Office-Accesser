package top.kagg886.eoa.gym

import androidx.annotation.VisibleForTesting
import co.touchlab.kermit.Logger
import io.ktor.client.call.body
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8
import top.kagg886.eoa.gym.internal.GymContext
import top.kagg886.eoa.gym.internal.GymAuthPlugin
import top.kagg886.eoa.gym.internal.SkipGymAuth
import top.kagg886.util.http.HttpClient
import top.kagg886.eoa.gym.internal.GymTransferPlugin
import top.kagg886.util.asKtorLogger
import kotlin.properties.Delegates

class GymClient(private val username: String) {
    private var password: String? = null

    @VisibleForTesting
    private var context by Delegates.notNull<GymContext>()
    private var logger = Logger.withTag("GymClient - $username")

    private val client = HttpClient {
        defaultRequest {
            url(BASE_URL)
        }
        install(Logging) {
            logger = this@GymClient.logger.asKtorLogger
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
        }
        install(ContentEncoding) {
            gzip()
            deflate()
        }

        install(GymAuthPlugin) {
            token = { context?.token }
            login = { this@GymClient.login(password ?: throw GymException("session expired.")) }
        }
        install(GymTransferPlugin) {
            magic = MAGIC
        }

    }

    suspend fun login(pass: String = password ?: error("first login should be password.")) {
        context = client.post("login/mobile/check") {
            attributes.put(SkipGymAuth, Unit)
            contentType(ContentType.Application.Json)
            setBody(
                LoginRequest(
                    username = username,
                    password = pass.encodeUtf8().md5().hex(),
                )
            )
        }.body<GymContext>()
        password = pass
    }

    suspend fun list(): List<GymSummary> {
        val resp = client.post("mobile/gymResult/selectUserPlanList") {
            contentType(ContentType.Application.Json)
            setBody(
                ListRequest(
                    userId = context.userId
                )
            )
        }.body<ListResponse>()

        return resp.data
    }

    suspend fun detail(id: String): List<GymDetail> {
        val resp = client.post("mobile/gymResult/selectUserPlanScore") {
            contentType(ContentType.Application.Json)
            setBody(
                DetailRequest(
                    userId = context.userId,
                    id = id
                )
            )
        }.body<DetailResponse>()

        return resp.data.data
    }

    suspend fun history(
        listId: String,
        detailId: String,
        page: Int = 1,
        size: Int = 10
    ): HistoryResponse {
        val resp = client.post("mobile/gymResult/selectUserPlanSubjectScore") {
            contentType(ContentType.Application.Json)
            setBody(
                HistoryRequest(
                    userId = context.userId,
                    listId = listId,
                    detailId = detailId,
                    pageNum = page,
                    pageSize = size,
                )
            )
        }.body<HistoryResponseOuter>()

        return resp.data
    }

    companion object {
        private const val BASE_URL = "http://47.92.231.221/service/"
        private const val MAGIC = "8d6c5b73a50d4707bd71c93882ddbc8b"
    }
}
