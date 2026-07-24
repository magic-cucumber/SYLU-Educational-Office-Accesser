package top.kagg886.eoa.second

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.serialization
import kotlinx.serialization.json.Json
import top.kagg886.eoa.util.DefaultAddableCookiesStorage
import top.kagg886.eoa.util.internal.HtmlFormat
import top.kagg886.eoa.util.internal.HttpResponseCharset
import top.kagg886.sylu_eoa.api.html.util.RSA
import top.kagg886.util.asKtorLogger
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.http.HttpClient

val TW_KEYS = arrayOf(
    "思想成长",
    "实践实习",
    "创新创业",
    "志愿公益",
    "文体活动"
)

class TWUser(
    val baseURL: String,
    val user: String,
    val ticket: Cookie? = null
) : AutoCloseable {
    private val log = "TWUser".asTaggedLogger
    private val client = HttpClient {
        engine {
            followRedirects = false
        }
        defaultRequest {
            url(baseURL)
        }

        install(HttpCookies) {
            storage = ticket?.let { DefaultAddableCookiesStorage(listOf(it)) } ?: AcceptAllCookiesStorage()
        }

        install(Logging) {
            level = LogLevel.ALL
            logger = log.asKtorLogger
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )

            serialization(ContentType.Text.Html, HtmlFormat)
        }

        install(HttpResponseCharset)
    }

    override fun close() = client.close()

    private val pubKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC3hzrH91c0OKgtaSB7GWGfDuUJ" +
            "sMrtiYThDXtJdrCr7exKt2fmIZngoFk71Dv/BPVQCHSuohNNvEV9VVDFSBhsP9xK" +
            "EDAM4/2Lv+wlzN9CuZtLpV3Elo8VacjwMHcjTRmTchRBmijQzZRFrA2LM+qsH3U5" +
            "tRM1uJFbfRMkBq24AwIDAQAB"

    suspend fun login(pass: String) {
        val dom = client.get("UserLogin.aspx").apply {
            if (status == HttpStatusCode.Found) {
                error("vpn ticket outdated.")
            }

            if (status != HttpStatusCode.OK) {
                error("service unavailable.")
            }
        }.body<Document>()
        //https://webvpn.sylu.edu.cn/http/77726476706e69737468656265737421e8f00f8f3e3c7d1e7b0c9ce29b5b/SyluTW/Sys/UserLogin.aspx" id="form1" style="height: 100%
        val loginResp = client.submitForm("UserLogin.aspx", formParameters = Parameters.build {
            set("UserName", user)
            set("__VIEWSTATE", dom.getElementById("__VIEWSTATE")!!.attr("value"))
            set("__VIEWSTATEGENERATOR", dom.getElementById("__VIEWSTATEGENERATOR")!!.attr("value"))
            set("__EVENTVALIDATION", dom.getElementById("__EVENTVALIDATION")!!.attr("value"))
            set("Password", pass)
            set("pwd", RSA.encrypt(pass, pubKey))
            set("pubKey", pubKey)
            set("codeInput", "KHG6")
            set("queryBtn", "%B5%C7++++++++++%C2%BC")
        })

        if (loginResp.status == HttpStatusCode.Found) {
            error("vpn ticket outdated.")
        }


        if (loginResp.headers[HttpHeaders.SetCookie].isNullOrEmpty()) {
            loginResp.body<Document>().getElementsByTag("script").forEach { v ->
                if (v.html().contains("layer.alert('")) {
                    error(
                        v.html().substringAfter("layer.alert('").substringBefore("',")
                    )
                }
            }

            error("can't find login tag, login failed.")
        }
    }

    //https://webvpn.sylu.edu.cn/http/77726476706e69737468656265737421e8f00f8f3e3c7d1e7b0c9ce29b5b/SyluTW/Sys/SystemForm/StuAction/StuActionSearch.aspx
    suspend fun getData(): Map<SecondClassDataSummary, List<SecondClassData>> {
        val map = mutableMapOf<SecondClassDataSummary, MutableList<SecondClassData>>()

        var dom = client.get("SystemForm/FinishExam/StuFinishStudentScore.aspx").apply {
            if (status == HttpStatusCode.Found) {
                error("vpn ticket outdated.")
            }
        }.body<Document>()

        var id = 'A'
        while (id <= 'E') {
            val e: String = dom.getElementById("Count$id")!!.text()
            val now = (e.ifEmpty { "0.00" }).toDouble()
            map[SecondClassDataSummary(TW_KEYS[id - 'A'], now)] = mutableListOf()
            id++
        }

        val e: String = dom.getElementById("SunCount")!!.text()
        val sum1 = (e.ifEmpty { "0.00" }).toDouble()
        map[SecondClassDataSummary("All", sum1)] = mutableListOf()

        dom = client.get("SystemForm/StuAction/StuActionSearch.aspx").apply {
            if (status == HttpStatusCode.Found) {
                error("vpn ticket outdated.")
            }
        }.body()


        fun parse() {
            val data: Elements = dom.getElementsByTag("tr")

            for (i in 2 until data.size) {
                val info: Element = data[i]

                val elements: Elements = info.getElementsByTag("td")
                map[if (elements[3].text() == "技能特长") "文体活动" else elements[3].text()].add(
                    SecondClassData(
                        elements[0].text(),
                        elements[1].text(),  //申请单位
                        elements[2].text(),  //时间
                        elements[4].text(),  //身份
                        elements[5].text().toInt(),  //参与人数
                        elements[7].text().toDouble()
                    )
                )
            }
        }
        parse()


        val page = dom.getElementById("TPaged1")?.getElementsByTag("font")?.last()?.text()!!.toInt()
        for (i in 2..page) {
            dom = client.submitForm("SystemForm/StuAction/StuActionSearch.aspx", formParameters = Parameters.build {
                set("__VIEWSTATE", dom.getElementById("__VIEWSTATE")!!.attr("value"))
                set("__VIEWSTATEGENERATOR", dom.getElementById("__VIEWSTATEGENERATOR")!!.attr("value"))
                set("__EVENTVALIDATION", dom.getElementById("__EVENTVALIDATION")!!.attr("value"))
                set("__VIEWSTATEENCRYPTED", "")
                set("YearTime", "")
                set("ActivityType", "")
                set("OrgNo", "")
                set("ActivityName", "")
                set($$"TPaged1$GotoPage", i.toString())
                set($$"TPaged1$Jump", "跳 转")
            }).apply {
                if (status == HttpStatusCode.Found) {
                    error("vpn ticket outdated.")
                }
            }.body()
            parse()
        }

        return map
    }

}
