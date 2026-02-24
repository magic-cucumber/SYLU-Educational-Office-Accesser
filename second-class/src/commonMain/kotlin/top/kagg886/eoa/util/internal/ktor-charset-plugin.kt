@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")


package top.kagg886.eoa.util.internal

import com.fleeksoft.charset.Charsets
import com.fleeksoft.io.ByteBufferFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.api.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.pipeline.*


/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/20 11:11
 * ================================================
 */

val HttpResponseCharset = createClientPlugin("HTTPResponseCharset") {
    on(ResponseHook) {
        val response = it.call.save().response
        val header = response.headers[HttpHeaders.ContentType].orEmpty()

        if (header.startsWith("text/") && !header.contains("charset=")) {
            return@on
        }

        val charset = Charsets.forName(header.substringAfter("charset=").substringBefore(","))

        val origin = response.bodyAsBytes()
        val doc = ByteBufferFactory.wrap(origin)
            .let(charset::decode)
            .toString() //FIXME: 需要优化
            .encodeToByteArray()


        proceedWith(
            SavedHttpResponse(
                call = response.call as SavedHttpCall,
                body = doc,
                origin = response
            )
        )
    }
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
