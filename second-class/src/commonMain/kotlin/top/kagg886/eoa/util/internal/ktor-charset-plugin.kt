@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")


package top.kagg886.eoa.util.internal

import com.fleeksoft.charset.Charsets as FleekCharsets
import com.fleeksoft.io.ByteBufferFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.api.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray


/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/20 11:11
 * ================================================
 */

val HttpResponseCharset = createClientPlugin("HTTPResponseCharset") {
    on(ResponseHook) {
        val header = it.headers[HttpHeaders.ContentType].orEmpty()
        if (!header.startsWith("text/") || !header.contains("charset=", ignoreCase = true)) {
            return@on
        }

        val charsetName = header
            .substringAfter("charset=", "")
            .substringBefore(";")
            .substringBefore(",")
            .trim()
            .trim('"')

        if (charsetName.isEmpty() || charsetName.equals("utf-8", ignoreCase = true)) {
            return@on
        }

        val charset = FleekCharsets.forName(charsetName)

        @OptIn(InternalAPI::class)
        val origin = it.rawContent.readRemaining().readByteArray()
        val doc = ByteBufferFactory.wrap(origin)
            .let(charset::decode)
            .toString() //FIXME: 需要优化
            .encodeToByteArray()

        val headers = Headers.build {
            appendAll(it.headers)
            remove(HttpHeaders.ContentLength)
            set(HttpHeaders.ContentLength, doc.size.toString())
            set(HttpHeaders.ContentType, header.replaceCharset("UTF-8"))
        }

        proceedWith(
            it.call.replaceResponse(headers = headers) { ByteReadChannel(doc) }.response
        )
    }
}

private val regex = Regex("charset\\s*=\\s*\"?[A-Za-z0-9._-]+\"?", RegexOption.IGNORE_CASE)
private fun String.replaceCharset(charset: String): String {
    return replace(regex, "charset=$charset")
}


private object ResponseHook : ClientHook<suspend ResponseHook.Context.(response: HttpResponse) -> Unit> {

    class Context(private val context: PipelineContext<HttpResponse, Unit>) {
        suspend fun proceed() = context.proceed()
        suspend fun proceedWith(response: HttpResponse) = context.proceedWith(response)
    }

    override fun install(
        client: HttpClient,
        handler: suspend Context.(response: HttpResponse) -> Unit
    ) {
        client.receivePipeline.intercept(HttpReceivePipeline.State) {
            handler(Context(this), subject)
        }
    }
}
