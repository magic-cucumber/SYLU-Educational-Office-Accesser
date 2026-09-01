package top.kagg886.sylu_eoa.api.graduate

import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.serialization
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import top.kagg886.sylu_eoa.api.graduate.util.HtmlFormat
import top.kagg886.sylu_eoa.api.graduate.util.RequestMergePlugin
import top.kagg886.sylu_eoa.api.graduate.util.SuspendableHttpRequestRetry
import top.kagg886.sylu_eoa.api.graduate.util.clearCookie
import top.kagg886.sylu_eoa.api.graduate.util.cookie
import top.kagg886.sylu_eoa.api.graduate.util.kermit
import top.kagg886.sylu_eoa.api.html.config.BuildConfig
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.InvalidCredentialsException
import top.kagg886.sylu_eoa.api.v2.RetryLimitException
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.sylu_eoa.api.v2.bean.ClassReturn
import top.kagg886.sylu_eoa.api.v2.bean.ExamExportOptions
import top.kagg886.sylu_eoa.api.v2.bean.ExamItem
import top.kagg886.sylu_eoa.api.v2.bean.GPAScore
import top.kagg886.sylu_eoa.api.v2.bean.GPAScoreSummary
import top.kagg886.sylu_eoa.api.v2.bean.SchoolCalender
import top.kagg886.sylu_eoa.api.v2.bean.SystemNotice
import top.kagg886.sylu_eoa.api.v2.bean.Term
import top.kagg886.sylu_eoa.api.v2.bean.TermPicker
import top.kagg886.sylu_eoa.api.v2.bean.TermResult
import top.kagg886.sylu_eoa.api.v2.bean.UserProfile
import top.kagg886.util.asKtorLogger
import top.kagg886.util.http.HttpClient
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.seconds

internal class EOAGraduateClient : EOAClient {
    private var storage by Delegates.notNull<StorageCookieStorage>()
    override var username by Delegates.notNull<String>()
    override var password by Delegates.notNull<String>()
    override suspend fun login(captchaHandler: (suspend (a: ByteArray) -> String)?) {
        TODO("Not yet implemented")
    }

    override suspend fun logout() {
        TODO("Not yet implemented")
    }

    override suspend fun markNoticeReadable(noticeId: String): Boolean {
        TODO("Not yet implemented")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    //懒加载，因为client需要在init之后才能使用
    private val client by lazy {
        HttpClient {
            defaultRequest { url("https://jxw.${BuildConfig.MESSAGE_API_ENDPOINT}") }

            install(ContentNegotiation) {
                json(json)

                serialization(ContentType.Text.Html, HtmlFormat)
            }

            install(Logging) {
                logger = kermit.asKtorLogger
                level = LogLevel.ALL
                sanitizeHeader("---hidden---") { header ->
                    when {
                        header.equals(HttpHeaders.Cookie, ignoreCase = true) -> true
                        header.equals(HttpHeaders.SetCookie, ignoreCase = true) -> true
                        else -> false
                    }
                }
            }

            install(HttpCookies) {
                storage = this@EOAGraduateClient.storage
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30.seconds.inWholeMilliseconds
            }

            install(SuspendableHttpRequestRetry) {
                retryOnExceptionIf { req, cause ->
                    val url = req.url.build().fullPath
                    //登出接口不需要retry
                    if (url.startsWith("/logout")) {
                        return@retryOnExceptionIf false
                    }

                    if (url.startsWith("/xtgl/login_getPublicKey.html")) {
                        return@retryOnExceptionIf false
                    }

                    //登录接口下放在业务端处理
                    if (url.startsWith("/xtgl/login_slogin.html")) {
                        return@retryOnExceptionIf false
                    }
                    cause !is InvalidCredentialsException && cause !is RetryLimitException
                }
                retryIf { _, resp ->
                    //除此之外全部redirect到登录页面的均需要重发
                    if (resp.headers[HttpHeaders.Location] == "/xtgl/login_slogin.html") {
                        return@retryIf true
                    }

                    //可能json api会返回这个值
                    if (resp.status.value == 901) {
                        return@retryIf true
                    }

                    false
                }
                exponentialDelay()
                maxRetries = 3

                modifyRequest { req ->
                    kermit.d("Retry request: ${req.url.build().fullPath}, $retryCount / $maxRetries")
                    //重新登录。每次 Ktor retry 只尝试一次，重试次数交给 HttpRequestRetry 控制。
                    val cookie = try {
                        //TODO internal 登录代码
                        storage.get(req.url.build())
                    } catch (e: Exception) {
                        //同样这里需要throw，否则未登录异常会被忽略
                        if (e is InvalidCredentialsException) {
                            throw e
                        }
                        kermit.d("Retry request: ${req.url.build().fullPath} failed.", e)
                        throw if (retryCount >= maxRetries) RetryLimitException(e) else e
                    }

                    if (cookie.isEmpty()) {
                        throw RetryLimitException(this.cause)
                    }

                    //清空原请求cookie
                    req.clearCookie()
                    //设置新cookie
                    for (c in cookie) {
                        req.cookie(c)
                    }
                }
            }

            install(RequestMergePlugin)
        }
    }

    override fun init(storage: Storage) {
        this.storage = StorageCookieStorage(storage)
    }

    override suspend fun getUserProfile(): UserProfile {
        TODO("Not yet implemented")
    }

    override suspend fun getSchoolCalender(): SchoolCalender {
        TODO("Not yet implemented")
    }

    override suspend fun getAllAvailableTerms(): TermResult {
        TODO("Not yet implemented")
    }

    override suspend fun getExamList(picker: TermPicker): List<ExamItem> {
        TODO("Not yet implemented")
    }

    override suspend fun getExamInfo(examItem: ExamItem): List<List<String>> {
        TODO("Not yet implemented")
    }

    override suspend fun getExamExportSink(
        term: Term,
        config: ExamExportOptions
    ): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun getClassTable(
        picker: TermPicker,
        firstDay: LocalDate
    ): ClassReturn {
        TODO("Not yet implemented")
    }

    override suspend fun getGPAScores(): List<GPAScoreSummary> {
        TODO("Not yet implemented")
    }

    override suspend fun getGPAScoreList(summary: GPAScoreSummary): List<GPAScore> {
        TODO("Not yet implemented")
    }

    override suspend fun getNotice(hasRead: Boolean): List<SystemNotice> {
        TODO("Not yet implemented")
    }
}

/**
 *
 * 同步 [Storage] 和 [CookiesStorage] 的存储器
 * 用于存储 [Cookie]
 *
 * 当 [get] 时, 会先从 [CookiesStorage] 中获取, 如果没有, 则从 [Storage] 中获取
 * 当 [addCookie] 时, 会先添加到 [CookiesStorage] 中, 然后再添加到 [Storage] 中
 *
 * 这样可以保证 [CookiesStorage] 中的 [Cookie] 是最新的, 而 [Storage] 中的 [Cookie] 是过期的
 *
 * @param storages 存储
 * @param cookies 存储
 * @author kagg886
 */
private class StorageCookieStorage(
    private val storages: Storage,
    private val cookies: CookiesStorage = AcceptAllCookiesStorage()
) : Storage by storages, CookiesStorage by cookies {
    private val list: MutableMap<String, List<Cookie>> = storages.get()?.let {
        runCatching { Json.decodeFromString<MutableMap<String, List<Cookie>>>(it) }.getOrNull()
    } ?: mutableMapOf()

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        cookies.addCookie(requestUrl, cookie)
        list[requestUrl.host] = cookies.get(requestUrl)
        storages.set(Json.encodeToString(list))
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        return cookies.get(requestUrl).ifEmpty {
            list[requestUrl.host] ?: emptyList()
        }
    }
}