package top.kagg886.sylu_eoa.api.html

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.select.Elements
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.kagg886.sylu_eoa.api.html.util.*
import top.kagg886.sylu_eoa.api.v2.*
import top.kagg886.sylu_eoa.api.v2.bean.*
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.seconds

internal class EOAHTMLClient : EOAClient {
    private var storage by Delegates.notNull<StorageCookieStorage>()
    override var username by Delegates.notNull<String>()
    override var password by Delegates.notNull<String>()

    private val json = Json {
        ignoreUnknownKeys = true
    }

    //懒加载，因为client需要在init之后才能使用
    private val client by lazy {
        HttpClient {
            defaultRequest { url("https://jxw.sylu.edu.cn") }

            install(ContentNegotiation) {
                json(json)

                serialization(ContentType.Text.Html, HtmlFormat)
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) = kermit.d(message)
                }
                level = LogLevel.ALL
            }

            install(HttpCookies) {
                storage = this@EOAHTMLClient.storage
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30.seconds.inWholeMilliseconds
            }

            install(HttpRequestRetry) {
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
                    if (retryCount == 0) {
                        return@modifyRequest
                    }
                    for (i in 1..maxRetries) {
                        kermit.d("Retry request: ${req.url.build().fullPath}, $i / $maxRetries")
                        //重新登录
                        val cookie = runBlocking {
                            try {
                                internalLogin()
                                storage.get(req.url.build())
                            } catch (e: Exception) {
                                //同样这里需要throw，否则未登录异常会被忽略
                                if (e is InvalidCredentialsException) {
                                    throw e
                                }
                                kermit.d("Retry request: ${req.url.build().fullPath} failed.",e)
                                null
                            }
                        }

                        if (cookie == null) {
                            continue
                        }

                        //清空原请求cookie
                        req.clearCookie()
                        //设置新cookie
                        for (c in cookie) {
                            req.cookie(c)
                        }
                        return@modifyRequest
                    }
                    throw RetryLimitException(this.cause)
                }
            }

            install(RequestMergePlugin)
        }
    }

    override suspend fun login(captchaHandler: (suspend (ByteArray) -> String)?) =
        internalLogin(null, captchaHandler)

    private suspend fun internalLogin(
        captcha: String? = null,
        captchaHandler: (suspend (ByteArray) -> String)? = null
    ) {
        if (username.isBlank() || password.isBlank()) {
            throw BadCredentialsException()
        }
        @Serializable
        data class RSAReturn(
            val modulus: String, val exponent: String
        )

        val rsaReturn = client.get(
            "xtgl/login_getPublicKey.html?time=${
                Clock.System.now().toEpochMilliseconds()
            }"
        )
            .body<RSAReturn>()


        val encryptPass = RSA.encrypt(password, rsaReturn.exponent, rsaReturn.modulus)

        val status = client.submitForm(
            url = "xtgl/login_slogin.html",
            formParameters = Parameters.build {
                append("yhm", username)
                append("mm", encryptPass)
                captcha?.let { append("yzm", it) }
            }
        )

        if (status.status == HttpStatusCode.Found) {
            return
        }

        val (_, message) = status.body<Document>().checkLogin()

        if (message == "验证码输入错误！") {
            val captcha = client.get("kaptcha").body<ByteArray>().let {
                captchaHandler?.invoke(it) ?: throw NeedCaptchaException()
            }
            internalLogin(captcha, captchaHandler)
            return
        }
        if (message.isUserNameAndPasswordInvalid()) {
            throw BadCredentialsException()
        }
        throw UnknownException(message)
    }

    override suspend fun logout() {
        client.get("/logout")
        //下次登录时需要抛出异常
        username = ""
        password = ""
    }

    override suspend fun getUserProfile(): UserProfile {
        val document =
            client.get("xsxxxggl/xsgrxxwh_cxXsgrxx.html?gnmkdm=N100801&layout=default&su=$username")
                .body<Document>()
        val avt = document.getElementsByTag("img")[0].attr("src")

        val img: ByteArray = client.get(avt).body()
        val ele: Elements = document.getElementsByClass("form-control-static")
        return UserProfile(
            ele[1].text(),
            ele[14].text(),
            ele[15].text(),
            img,
            ele[26].text(),
            ele[27].text(),
            ele[7].text(),
            ele[10].text(),
            ele[24].text()
        )
    }

    override suspend fun getSchoolCalender(): SchoolCalender {
        val document =
            client.post("xtgl/index_cxAreaSix.html?localeKey=zh_CN&gnmkdm=index&su=$username")
                .body<Document>()

        val (startDateString, endDateString) = document.getElementsByAttribute("colspan")[0].text()
            .let {
                val l = it.indexOf("(")
                val r = it.indexOf(")")

                it.substring(l + 1, r).split("至").toList()
            }
        return SchoolCalender(
            start = LocalDate.parse(startDateString),
            end = LocalDate.parse(endDateString)
        )
    }

    override suspend fun getAllAvailableTerms(): TermResult {
        val termPickers = mutableListOf<TermPicker>()

        var defaultYearName: String? = null
        var defaultYearNameVal: String? = null
        var defaultYearCode: String? = null
        var defaultYearCodeVal: String? = null

        val document =
            client.get("cjcx/cjcx_cxDgXscj.html?gnmkdm=N305005&layout=default&su=$username")
                .body<Document>()

        val tempYearNameMap = mutableMapOf<String, String>()
        val tempYearCodeMap = mutableMapOf<String, String>()

        for (e in document.getElementById("xnm")!!.getElementsByTag("option")) {
            if (e.attr("selected") == "selected") {
                defaultYearName = e.text()
                defaultYearNameVal = e.attr("value")
            }
            tempYearNameMap[e.text()] = e.attr("value")
        }

        for (e in document.getElementById("xqm")!!.getElementsByTag("option")) {
            if (e.attr("selected") != "") {
                defaultYearCode = e.text()
                defaultYearCodeVal = e.attr("value")
            }
            tempYearCodeMap[e.text()] = e.attr("value")
        }

        tempYearNameMap.forEach a@{ name ->
            tempYearCodeMap.forEach b@{ code ->
                if (code.value.isBlank()) { //过滤学期名为 全部(空)的情况
                    return@b
                }
                termPickers.add(TermPicker(Pair(name.key, name.value), Pair(code.key, code.value)))
            }
        }

        return TermResult(
            termPickers,
            TermPicker(
                Pair(defaultYearName!!, defaultYearNameVal!!),
                Pair(defaultYearCode!!, defaultYearCodeVal!!)
            )
        )
    }

    override suspend fun getExamList(picker: TermPicker): List<ExamItem> {

        @Serializable
        data class ExamListReturn(
            val items: List<ExamItem>
        )

        val document = client.submitForm(
            url = "cjcx/cjcx_cxXsgrcj.html?doType=query&gnmkdm=N305005&su=$username",
            formParameters = Parameters.build {
                this["xnm"] = picker.asTerm().xnm
                this["xqm"] = picker.asTerm().xqm
                this["_search"] = "false"
                this["nd"] = "${Clock.System.now().toEpochMilliseconds()}"
                this["queryModel.showCount"] = "5000"
                this["queryModel.currentPage"] = "1"
                this["time"] = "2"
            }
        ).body<ExamListReturn>()

        return document.items
    }

    override suspend fun getExamInfo(examItem: ExamItem): List<List<String>> {
        val tr = client.submitForm(
            url = "cjcx/cjcx_cxCjxqGjh.html?gnmkdm=N305005&su=$username",
            formParameters = Parameters.build {
                this["jxb_id"] = examItem.detailsID
                this["xnm"] = examItem.year
                this["xqm"] = examItem.semester
                this["kcmc"] = examItem.name
            }
        ).body<Document>().getElementsByTag("tr")

        val rtn = mutableListOf<List<String>>()
        for (j in 1..<tr.size) {
            val trs = mutableListOf<String>()
            tr[j].getElementsByTag("td").run {
                trs.add(text())
            }
            rtn.add(trs)
        }

        return rtn
    }

    override suspend fun getClassTable(picker: TermPicker): List<ClassUnit> {
        @Serializable
        data class ClassUnitReturn(
            val kbList: List<ClassUnit>
        )

        val doc = client.submitForm(
            "kbcx/xskbcx_cxXsgrkb.html?gnmkdm=ssss&su=$username",
            formParameters = Parameters.build {
                this["xnm"] = picker.asTerm().xnm
                this["xqm"] = picker.asTerm().xqm
                this["kzlx"] = "ck"
                this["xsdm"] = ""
            }
        ).body<ClassUnitReturn>()

        return doc.kbList
    }

    override suspend fun getGPAScores(): List<GPAScoreSummary> {
        @Serializable
        data class GPAScoreSummaryReturn(
            val items: List<GPAScoreSummary>
        )

        val doc = client.submitForm(
            url = "xmfzgl/xshdfzcx_cxXshdfzcxIndex.html?doType=query&gnmkdm=N4780&su=$username",
            formParameters = Parameters.build {
                this["queryModel.showCount"] = "5000"
                this["queryModel.currentPage"] = "1"
                this["_search"] = "false"
                this["nd"] = "${Clock.System.now().toEpochMilliseconds()}"
                this["time"] = "2"
            }
        ).body<GPAScoreSummaryReturn>()

        return doc.items
    }

    override suspend fun getGPAScoreList(summary: GPAScoreSummary): List<GPAScore> {
        @Serializable
        data class GPAListReturn(
            val items: List<GPAScore>
        )

        val doc = client.submitForm(
            url = "xmfzgl/xshdfzcx_cxXmfzqr.html?gnmkdm=N4780&su=$username",
            formParameters = Parameters.build {
                this["xmlbmc"] = summary.name
            }
        ).body<GPAListReturn>()
        return doc.items
    }

    override suspend fun getNotice(hasRead: Boolean): List<SystemNotice> {
        @Serializable
        data class SystemNoticeReturn(
            val items: List<SystemNotice>
        )

        val doc = client.submitForm(
            url = "xtgl/index_cxDbsy.html?doType=query",
            formParameters = Parameters.build {
                this["sfyy"] = if (hasRead) "2" else "1"
                this["queryModel.showCount"] = "5000"
                this["queryModel.currentPage"] = "1"
                this["queryModel.sortName"] = "cjsj"
                this["queryModel.sortOrder"] = "desc"
                this["_search"] = "false"
                this["nd"] = "${Clock.System.now().toEpochMilliseconds()}"
            }
        ).body<SystemNoticeReturn>()

        return doc.items
    }

    override suspend fun markNoticeReadable(noticeId: String): Boolean {
        val data = client.submitForm(
            url = "/xtgl/index_cxXxdlztgx.html",
            formParameters = Parameters.build {
                this["zjxx"] = noticeId
            }
        ).body<String>()

        return data.contains("操作成功")
    }

    override fun init(storage: Storage) {
        this.storage = StorageCookieStorage(storage)
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
