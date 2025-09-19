package top.kagg886.eoa.second

import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.serialization
import io.ktor.util.decodeBase64Bytes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.kagg886.eoa.second.bean.CaptchaReturn
import top.kagg886.eoa.second.bean.Portal
import top.kagg886.eoa.second.bean.PortalReturn
import top.kagg886.eoa.second.bean.Resource
import top.kagg886.eoa.second.internal.HtmlFormat
import top.kagg886.eoa.second.internal.aes
import kotlin.time.Clock

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/19 23:51
 * ================================================
 */

class VPNClient(private val storage: Storage, private val username: String, private val password: String) :
    AutoCloseable {
    val it = HttpClient {
        engine {
            followRedirects = false
        }
        defaultRequest {
            url("https://webvpn.sylu.edu.cn")
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
            serialization(ContentType.Text.Html, HtmlFormat)
        }

        install(ContentEncoding) {
            //gzip, deflate, br, zstd
            gzip()
            deflate()
        }

        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }

//        install(Logging) {
//            level = LogLevel.NONE
//            logger = object : Logger {
//                override fun log(message: String) {
//                    println(message)
//                }
//            }
//        }
    }

    override fun close() = it.close()

    /**
     * @param captchaHandler 该handler传入大图片和小图片。
     *
     * 返回：1.大图片的长度 2.小图片在大图片中的距离。
     */
    suspend fun login(captchaHandler: (suspend (background: ByteArray, slider: ByteArray) -> CaptchaReturn?)? = null) {
        val redirect = it.get("login?cas_login=true").headers[HttpHeaders.Location]!!
        val contextPath = redirect.substringBefore("/login")
        val form = it.get(redirect).body<Document>().getElementById("pwdFromId")!!

        run {
            @Serializable
            data class NeedCaptcha(
                val isNeed: Boolean = false,
            )

            val needCaptcha = it.get("$contextPath/checkNeedCaptcha.htl") {
                parameter("username", username)
                parameter("_", Clock.System.now().toEpochMilliseconds())
            }.body<NeedCaptcha>().isNeed

            if (!needCaptcha) {
                return@run
            }

            checkNotNull(captchaHandler) {
                "the system required captcha but handler not found"
            }

            while (true) {
                @Serializable
                data class Captcha(
                    val smallImage: String, //base64 png
                    val bigImage: String, //base64 png
                    val tagWidth: Int,
                )

                val slider = it.get("$contextPath/common/openSliderCaptcha.htl").body<Captcha>()

                @Serializable
                data class SliderResult(
                    val errorCode: Int, //0为错误，1为成功
                    val errorMsg: String,
                )

                val captcha = captchaHandler(slider.bigImage.decodeBase64Bytes(), slider.smallImage.decodeBase64Bytes())
                checkNotNull(captcha) {
                    "the system required captcha but user cancelled"
                }
                val status =
                    it.submitForm("$contextPath/common/verifySliderCaptcha.htl", formParameters = Parameters.build {
                        //canvasLength=280&moveLength=179

                        set("canvasLength", captcha.backgroundWidth.toString())

                        // 280   590
                        // --- = ---
                        //  x    code
                        set("moveLength", captcha.sliderTarget.toString())
                    }).body<SliderResult>()

                if (status.errorCode == 1) {
                    break
                }
            }
        }

        val loginResp = it.submitForm(
            url = form.attr("action"),
            formParameters = Parameters.build {
                set("username", username)
                set("password", aes(password, form.getElementById("pwdEncryptSalt")!!.value()))
                set("_eventId", "submit")
                set("cllt", "userNameLogin")
                set("dllt", "generalLogin")
                set("lt", "")
                set("execution", form.getElementById("execution")!!.value())
            }
        ) {
            parameter("service", "https://webvpn.sylu.edu.cn/login?cas_login=true")
        }

        if (loginResp.status != HttpStatusCode.Found) {
            val errorTip = loginResp.body<Document>().getElementById("showErrorTip")!!.text()
            error(errorTip)
        }

        //登录成功

        run {
            var link = loginResp.headers[HttpHeaders.Location]!!
            while (true) {
                val status = it.get(link)

                if (status.status == HttpStatusCode.Found) {
                    link = status.headers[HttpHeaders.Location]!!
                    continue
                }
                break
            }
        }
    }

    suspend fun portal(): List<Resource> {
        val portalReturn = it.get("user/portal_groups")

        check(portalReturn.status == HttpStatusCode.OK) {
            "cookie was dead, please login again."
        }

        return portalReturn.body<PortalReturn>().data.map { it.resource }.flatten()
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

interface Storage {
    fun get(): String?
    fun set(value: String)
}

