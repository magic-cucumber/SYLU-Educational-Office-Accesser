package top.kagg886.eoa.gym.internal

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import top.kagg886.eoa.gym.GymException
import kotlin.properties.Delegates

internal class GymTransferPluginConfig {
    var magic by Delegates.notNull<String>()
}

internal val GymTransferPlugin = createClientPlugin(
    name = "GymTransferPlugin",
    createConfiguration = ::GymTransferPluginConfig,
) {
    val config = pluginConfig
    transformRequestBody { request, content, _ ->
        if (request.method != HttpMethod.Post) {
            return@transformRequestBody null
        }

        val body = when (content) {
            is JsonObject -> content
            is OutgoingContent.ByteArrayContent if (content.contentType?.match(ContentType.Application.Json) == true) ->
                Json.parseToJsonElement(content.bytes().decodeToString()) as? JsonObject

            else -> null
        }

        if (body == null) return@transformRequestBody null

        val contentType = request.headers[HttpHeaders.ContentType]
            ?.let(ContentType::parse)
            ?: (content as? OutgoingContent)?.contentType

        if (contentType?.match(ContentType.Application.Json) != true) {
            return@transformRequestBody null
        }

        val signed = body.toMutableMap().apply {
            put("sign", JsonPrimitive(body.sign(config.magic)))
        }

        TextContent(
            text = JsonObject(signed).toString(),
            contentType = contentType
        )
    }

    onResponse { response ->
        // 过期响应交给 Auth 插件处理，避免提前被业务错误检查拦截。
        if (response.headers["abnormal"] == "TIMEOUT") return@onResponse
        if (response.contentType()?.match(ContentType.Application.Json) != true) {
            return@onResponse
        }

        val content = response.bodyAsText()
        val json = Json.parseToJsonElement(content) as? JsonObject
            ?: throw GymException("Invalid response")

        val status = json["status"]?.jsonPrimitive?.content

        if (status != "PASS") {
            val message = json["message"]?.jsonPrimitive?.content
            throw GymException(message ?: "Request failed: $status")
        }
    }
}
