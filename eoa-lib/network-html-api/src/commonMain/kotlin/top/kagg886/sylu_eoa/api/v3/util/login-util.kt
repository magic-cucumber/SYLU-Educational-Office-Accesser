package top.kagg886.sylu_eoa.api.v3.util

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.maxAge
import io.ktor.http.renderCookieHeader
import io.ktor.util.date.GMTDate

internal fun Document.checkLogin(): Pair<Boolean, String> {
    val errorMessage = this.getElementsByTag("h5").any { it.text() == "用户登录" }
    if (!errorMessage) {
        return Pair(true, "Cookie过期")
    }
    val test: Element? = this.getElementById("tips")
    if (test != null) {
        return Pair(false, test.text())
    }
    return Pair(false, "Cookie过期")
}

internal fun String.isUserNameAndPasswordInvalid(): Boolean = this.contains("用户名或密码不正确，请重新输入！")

fun HttpMessageBuilder.cookie(cookie: Cookie) {
    val renderedCookie = cookie.let(::renderCookieHeader)
    if (HttpHeaders.Cookie !in headers) {
        headers.append(HttpHeaders.Cookie, renderedCookie)
        return
    }
    headers[HttpHeaders.Cookie] = headers[HttpHeaders.Cookie] + "; " + renderedCookie
}

fun HttpMessageBuilder.clearCookie() = headers.remove(HttpHeaders.Cookie)
