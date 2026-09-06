package top.kagg886.sylu_eoa.api.graduate

import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.serialization
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.kagg886.sylu_eoa.api.graduate.util.RequestMergePlugin
import top.kagg886.sylu_eoa.api.graduate.util.ResponseBodyRewritePlugin
import top.kagg886.sylu_eoa.api.graduate.util.kermit
import top.kagg886.sylu_eoa.api.html.config.BuildConfig
import top.kagg886.sylu_eoa.api.html.util.RSA
import top.kagg886.sylu_eoa.api.v2.BadCredentialsException
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.NeedCaptchaException
import top.kagg886.sylu_eoa.api.v2.RetryLimitException
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.sylu_eoa.api.v2.UnknownException
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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

internal class EOAGraduateClient : EOAClient {
    private var storage by Delegates.notNull<StorageCookieStorage>()
    override var username by Delegates.notNull<String>()
    override var password by Delegates.notNull<String>()
    override var captchaHandler: (suspend (a: ByteArray) -> String)? = null

    override suspend fun login() = internalLogin(captchaHandler = captchaHandler)

    override suspend fun logout() {
        client.get("home/logout")
        //下次登录时需要抛出异常
        username = ""
        password = ""
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
            defaultRequest { url("https://yjsgl.${BuildConfig.MESSAGE_API_ENDPOINT}") }

            install(ContentNegotiation) {
                json(json)
                serialization(ContentType.Text.Html, json)
            }

            install(ResponseBodyRewritePlugin)

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

            install(RequestMergePlugin)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun internalLogin(captchaHandler: (suspend (ByteArray) -> String)? = null) {
        if (username.isBlank() || password.isBlank()) {
            throw BadCredentialsException()
        }

        @Serializable
        data class LoginInfo(
            @SerialName("UserId")
            val name: String,
            @SerialName("Password")
            val pass: String,
            @SerialName("Vericode")
            val code: String,
            val url: String,
            val city: String,
        )

        @Serializable
        data class LoginReturn(
            val jg: String,
            val msg: String = "",
            val url: String = "",
        )

        val loginPage = client.get("home/stulogin").body<String>().let { Ksoup.parse(it) }
        val pubkey = loginPage.getElementById("pubkey")
            ?.attr("value")
            .orEmpty()
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .filterNot { it.isWhitespace() }

        if (pubkey.isBlank()) {
            throw UnknownException("登录页缺少公钥")
        }

        do {
            val captcha = client.get("Home/VerificationCode?codetype=stucode&&t=${Clock.System.now().toEpochMilliseconds()}")
                .body<ByteArray>()
                .let { captchaHandler?.invoke(it) ?: throw NeedCaptchaException() }

            val response = client.submitForm(
                url = "home/stulogin_do",
                formParameters = Parameters.build {
                    append(
                        "json",
                        json.encodeToString(
                            LoginInfo(
                                name = username,
                                pass = RSA.encrypt(password, pubkey),
                                code = captcha,
                                url = loginPage.getElementById("hfgoto")?.attr("value").orEmpty(),
                                city = loginPage.getElementById("hfcity")?.attr("value").orEmpty(),
                            )
                        )
                    )
                }
            ).body<LoginReturn>()


            if (response.jg == "1") {
                return
            }

            if (response.msg.contains("验证码错误")) {
                continue
            }
            if (response.msg.contains("密码错误")) {
                throw BadCredentialsException()
            }

            if (response.msg.contains("用户不存在")) {
                throw BadCredentialsException()
            }

            if (response.msg.contains("连续登录错误5次，请两小时后再登录！")) {
                throw RetryLimitException(UnknownException("登陆次数过多，请2小时后再试。"))
            }

            throw UnknownException(response.msg)
        } while (true)
    }

    override fun init(storage: Storage) {
        this.storage = StorageCookieStorage(storage)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getUserProfile(): UserProfile {
        @Serializable
        data class GraduateUserInfo(
            // 学号
            val xh: String = "",
            // 姓名
            val xm: String = "",
            // 学生所在学院名称
            val xsmc: String = "",
            // 专业名称
            val zymc: String = "",
            // 邮箱
            val email: String = "",
            // 手机号码
            val sjhm: String = "",
            // 联系电话
            val lxdh: String = "",
            // 政治面貌
            val zzmm: String = "",
            // 外语语种
            val wyyz: String = "英语",
        )

        @Serializable
        data class GraduateUserProfileReturn(
            val jbxx: GraduateUserInfo,
        )

        val profile = client.get("student/grgl/xsxx_jbxx?_=${Clock.System.now().toEpochMilliseconds()}")
            .body<GraduateUserProfileReturn>()
            .jbxx

        val avatar = client.get("student/grgl/PotoImageShow/?bh=${profile.xh}").body<ByteArray>()

        return UserProfile(
            name = profile.xm,
            collegeName = profile.xsmc,
            studyName = profile.zymc,
            avatar = avatar,
            email = profile.email,
            phone = profile.sjhm.ifBlank { profile.lxdh },
            id = profile.xh,
            policy = profile.zzmm,
            language = profile.wyyz,
        )
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
