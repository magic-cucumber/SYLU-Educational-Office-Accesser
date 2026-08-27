package top.kagg886.eoa.vpn

import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import top.kagg886.eoa.util.internal.HtmlFormat
import top.kagg886.eoa.second.config.BuildConfig
import top.kagg886.eoa.vpn.bean.CaptchaReturn
import top.kagg886.eoa.vpn.bean.PortalReturn
import top.kagg886.eoa.vpn.bean.Resource
import top.kagg886.eoa.vpn.internal.aes
import top.kagg886.util.asKtorLogger
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.http.HttpClient
import kotlin.io.encoding.Base64
import kotlin.time.Clock

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/19 23:51
 * ================================================
 */

class VPNClient(private val username: String, private val password: String) : AutoCloseable {
    private val logger = "VPNClient".asTaggedLogger
    private val cookie = AcceptAllCookiesStorage()
    private val it = HttpClient {
        engine {
            followRedirects = false
        }
        defaultRequest {
            url("https://webvpn.${BuildConfig.MESSAGE_API_ENDPOINT}")
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

        install(Logging) {
            level = LogLevel.BODY
            logger = this@VPNClient.logger.asKtorLogger
        }
    }

    suspend fun ticket() =
        cookie.get(Url("https://webvpn.${BuildConfig.MESSAGE_API_ENDPOINT}")).first { it.name == "wengine_vpn_ticketwebvpn_sylu_edu_cn" }

    override fun close() = it.close()

    /**
     * @param totpHandler TOTP验证的code
     * @param captchaHandler 该handler传入大图片和小图片。
     *
     * 返回：1.大图片的长度 2.小图片在大图片中的距离。
     */
    suspend fun login(
        totpHandler: (suspend () -> Int?)? = null,
        captchaHandler: (suspend (background: ByteArray, slider: ByteArray) -> CaptchaReturn?)? = null,
    ) {
        val redirect = it.get("login?cas_login=true").headers[HttpHeaders.Location]!!

        if (redirect == "/") return //已登录用户无需重复登录

        val contextPath = redirect.substringBefore("/login")
        val form = it.get(redirect).body<Document>().let {
            it.getElementById("pwdFromId") ?: error("login failed.")
        }

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

                val captcha = captchaHandler(Base64.decode(slider.bigImage), Base64.decode(slider.smallImage))
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
            parameter("service", "https://webvpn.${BuildConfig.MESSAGE_API_ENDPOINT}/login?cas_login=true")
        }

        if (loginResp.status != HttpStatusCode.Found) {
            val errorTip = loginResp.body<Document>().getElementById("showErrorTip")!!.text()
            error(errorTip)
        }

        //登录成功

        //validate的值：登录出现问题返回含特征的html，需要二次验证返回固定值。
        val validate = run {
            var link = loginResp.headers[HttpHeaders.Location]!!
            while (true) {
                val status = it.get(link)

                if (status.status == HttpStatusCode.Found) {
                    link = status.headers[HttpHeaders.Location]!!

                    if (link == "/login?second_login=true") {
                        return@run link
                    }
                    continue
                }
                return@run status.body<String>()
            }
            return@run ""
        }

        //TOTP
        if (validate == "/login?second_login=true") {
            checkNotNull(totpHandler) {
                "the system required TOTP but handler not found"
            }
            while (true) {
                val code = totpHandler()
                checkNotNull(code) {
                    "the system required TOTP but user cancelled"
                }

                @Serializable
                data class TOTPResult(
                    val success: Boolean,
                    val url: String? = null,
                    val message: String? = null,
                )

                val result = it.submitForm(
                    url = "/do-second-login",
                    formParameters = Parameters.build {
                        append("username", "")
                        append("code", code.toString().padStart(6, '0'))
                    }
                ).body<TOTPResult>()

                if (result.success) {
                    //mock browser
                    it.get(result.url!!).body<String>()
                    break
                }

                //{
                //  "success": true,
                //  "url": "/",
                //  "message": "xxxx",
                //}
            }
            return
        }

        if (validate.contains("        var errorMessage = '")) {
            error(validate.substringAfter("var errorMessage = '").substringBefore("'"))
        }
    }

    suspend fun portal(): List<Resource> {
        val portalReturn = it.get("user/portal_groups")

        check(portalReturn.status == HttpStatusCode.OK) {
            "cookie was dead, please login again."
        }

        return portalReturn.body<PortalReturn>().data.flatMap { it.resource }
    }
}
