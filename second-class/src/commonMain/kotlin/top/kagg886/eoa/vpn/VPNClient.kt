package top.kagg886.eoa.vpn

import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.kagg886.eoa.util.Storage
import top.kagg886.eoa.util.StorageCookieStorage
import top.kagg886.eoa.util.internal.HtmlFormat
import top.kagg886.eoa.vpn.bean.CaptchaReturn
import top.kagg886.eoa.vpn.bean.PortalReturn
import top.kagg886.eoa.vpn.bean.Resource
import top.kagg886.eoa.vpn.internal.aes
import kotlin.time.Clock

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/19 23:51
 * ================================================
 */

class VPNClient(private val storage: Storage, private val username: String, private val password: String) :
    AutoCloseable {
    private val cookie = StorageCookieStorage(this@VPNClient.storage)
    private val it = HttpClient {
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
            storage = cookie
        }
    }

    suspend fun ticket() =
        cookie.get(Url("https://webvpn.sylu.edu.cn")).first { it.name == "wengine_vpn_ticketwebvpn_sylu_edu_cn" }

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

