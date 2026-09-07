package top.kagg886.sylu_eoa.api.graduate

import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.BodyFilterResult
import io.ktor.client.plugins.logging.CommonLogBodyFilter
import io.ktor.client.plugins.logging.LogBodyFilter
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.isTextType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.serialization
import io.ktor.utils.io.ByteReadChannel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.kagg886.sylu_eoa.api.graduate.util.RequestMergePlugin
import top.kagg886.sylu_eoa.api.graduate.util.ResponseBodyRewritePlugin
import top.kagg886.sylu_eoa.api.graduate.util.kermit
import top.kagg886.sylu_eoa.api.graduate.config.BuildConfig
import top.kagg886.sylu_eoa.api.html.util.RSA
import top.kagg886.sylu_eoa.api.v2.BadCredentialsException
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.NeedCaptchaException
import top.kagg886.sylu_eoa.api.v2.RetryLimitException
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.sylu_eoa.api.v2.UnknownException
import top.kagg886.sylu_eoa.api.v2.bean.ClassReturn
import top.kagg886.sylu_eoa.api.v2.bean.ClassTable
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
import kotlin.math.abs
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
            val captcha = client.get(
                "Home/VerificationCode?codetype=stucode&&t=${
                    Clock.System.now().toEpochMilliseconds()
                }"
            )
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
            @SerialName("jbxx")
            val info: GraduateUserInfo,
        )

        val profile =
            client.get("student/grgl/xsxx_jbxx?_=${Clock.System.now().toEpochMilliseconds()}")
                .body<GraduateUserProfileReturn>()
                .info

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

    @OptIn(ExperimentalTime::class)
    override suspend fun getSchoolCalender(): SchoolCalender {
        @Serializable
        data class Registration(val zcrq: String = "")

        val registrations = client.post("student/grgl/bindXsZcXx")
            .body<List<Registration>>()

        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

        val anchorDate = registrations
            .mapNotNull { registration ->
                runCatching { LocalDate.parse(registration.zcrq) }.getOrNull()
            }
            .minByOrNull { date -> abs(date.toEpochDays() - today.toEpochDays()) }
            ?: throw UnknownException("校历接口未返回有效的注册日期")

        val daysFromMonday = anchorDate.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
        val previousMonday = anchorDate.plus(-daysFromMonday, DateTimeUnit.DAY)
        val nextMonday = previousMonday.plus(7, DateTimeUnit.DAY)
        val startDate =
            if (anchorDate.toEpochDays() - previousMonday.toEpochDays() <= nextMonday.toEpochDays() - anchorDate.toEpochDays()) previousMonday else nextMonday

        return SchoolCalender(
            start = startDate,
            end = startDate.plus(20, DateTimeUnit.WEEK),
        )
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getAllAvailableTerms(): TermResult {
        @Serializable
        data class GraduateTerm(
            @SerialName("termcode")
            val termCode: String,
            val selected: Boolean = false,
        )

        val terms =
            client.get("student/default/bindterm?_=${Clock.System.now().toEpochMilliseconds()}")
                .body<List<GraduateTerm>>()

        val termPickers = terms.map { term ->
            val code = term.termCode.split("-")
            if (code.size != 3 || code[0].length != 2 || code[1].length != 2) {
                throw UnknownException("学期接口返回了无效的学期代码：${term.termCode}")
            }

            val semesterName = when (code[2]) {
                "01" -> "第一学期"
                "02" -> "第二学期"
                else -> throw UnknownException("学期接口返回了无效的学期代码：${term.termCode}")
            }

            TermPicker(
                yearName = "20${code[0]}-20${code[1]}学年" to code.take(2).joinToString("-"),
                yearCode = semesterName to code[2],
            )
        }

        val defaultIndex = terms.indexOfFirst { it.selected }
        if (defaultIndex < 0) {
            throw UnknownException("学期接口未返回默认学期")
        }

        return TermResult(
            list = termPickers,
            default = termPickers[defaultIndex],
        )
    }

    override suspend fun getClassTable(
        picker: TermPicker,
        firstDay: LocalDate
    ): ClassReturn {
        fun getTimeByLessonNumber(lessonNumber: Int): Pair<LocalTime, LocalTime> =
            when (lessonNumber) {
                1 -> LocalTime.parse("08:00") to LocalTime.parse("08:45")
                2 -> LocalTime.parse("08:55") to LocalTime.parse("09:40")
                3 -> LocalTime.parse("10:00") to LocalTime.parse("10:45")
                4 -> LocalTime.parse("10:55") to LocalTime.parse("11:40")

                5 -> LocalTime.parse("13:00") to LocalTime.parse("13:45")
                6 -> LocalTime.parse("13:55") to LocalTime.parse("14:40")
                7 -> LocalTime.parse("14:50") to LocalTime.parse("15:35")
                8 -> LocalTime.parse("15:45") to LocalTime.parse("16:30")
                9 -> LocalTime.parse("16:40") to LocalTime.parse("17:25")
                10 -> LocalTime.parse("17:35") to LocalTime.parse("18:20")

                11 -> LocalTime.parse("19:30") to LocalTime.parse("20:15")
                12 -> LocalTime.parse("20:25") to LocalTime.parse("21:10")

                else -> error("No such lesson: $lessonNumber")
            }

        @Serializable
        data class InternalClassRow(
            val jcid: Int,
            val sjbz: String,
            val mc: String,
            val z1: String?,
            val z2: String?,
            val z3: String?,
            val z4: String?,
            val z5: String?,
            val z6: String?,
            val z7: String?,
        )

        @Serializable
        data class InternalClassReturn(
            val rows: List<InternalClassRow>,
            val week: String,
        )

        data class ParsedClass(
            val name: String,
            val teacher: String,
            val room: String,
            val weeks: List<Int>,
        )

        fun String.parseWeeks(): List<Int> = split(",").flatMap { item ->
            val value = item.removeSuffix("周")
            val range = value.split("-")

            if (range.size == 1) {
                listOf(range[0].toInt())
            } else {
                (range[0].toInt()..range[1].toInt()).toList()
            }
        }

        fun String.parseClass(): ParsedClass {
            // 例如：
            // 人工智能1班[3-10周] 黄海新[主校区综合楼A223]

            val weekStart = indexOf('[')
            val weekEnd = indexOf(']', weekStart)

            val roomStart = indexOf('[', weekEnd)
            val roomEnd = indexOf(']', roomStart)

            return ParsedClass(
                name = substring(0, weekStart),
                teacher = substring(weekEnd + 1, roomStart).trim(),
                room = substring(roomStart + 1, roomEnd),
                weeks = substring(weekStart + 1, weekEnd).parseWeeks(),
            )
        }

        fun String.parseCell(): List<ParsedClass> =
            split("<br/><br/>")
                .map { it.removePrefix("<br/>") }
                .map { it.parseClass() }

        fun InternalClassRow.cells() = listOf(
            z1, z2, z3, z4, z5, z6, z7
        )

        fun Int.toLessonNumber(): Int =
            (this / 10 - 1) * 4 + this % 10

        val term = picker.asTerm()

        val result =
            client.submitForm(url = "/student/pygl/py_kbcx_ew", formParameters = Parameters.build {
                append("kblx", "xs")
                append("termcode", "${term.xnm}-${term.xqm}")
            }).body<InternalClassReturn>()

        val tables = result.rows
            .filter { it.jcid != 40 }
            .flatMap { row ->
                val lessonNumber = row.jcid.toLessonNumber()
                val (startTime, endTime) = getTimeByLessonNumber(lessonNumber)

                row.cells().flatMapIndexed { dayIndex, cell ->
                    cell?.parseCell()?.flatMap { course ->
                        course.weeks.map { week ->
                            val date = firstDay
                                .plus(week - 1, DateTimeUnit.WEEK)
                                .plus(dayIndex, DateTimeUnit.DAY)

                            ClassTable(
                                id = "${course.name}|${course.teacher}|${course.room}",
                                name = course.name,
                                teacher = course.teacher,
                                room = course.room,

                                // 此接口不提供这些信息。
                                score = "0",
                                classType = "",
                                isDegreeProgram = false,

                                startTime = date.atTime(startTime),
                                endTime = date.atTime(endTime),
                            )
                        }
                    }.orEmpty()
                }
            }

        return ClassReturn(
            extend = emptyList(),
            tables = tables,
        )
    }

    override suspend fun getExamList(picker: TermPicker): List<ExamItem> = listOf()

    override suspend fun getExamInfo(examItem: ExamItem): List<List<String>> = listOf()

    override suspend fun getExamExportSink(term: Term, config: ExamExportOptions): ByteArray =
        byteArrayOf()


    override suspend fun getGPAScores(): List<GPAScoreSummary> = listOf()

    override suspend fun getGPAScoreList(summary: GPAScoreSummary): List<GPAScore> = listOf()
    override suspend fun getNotice(hasRead: Boolean): List<SystemNotice> = listOf()
    override suspend fun markNoticeReadable(noticeId: String): Boolean = false

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
