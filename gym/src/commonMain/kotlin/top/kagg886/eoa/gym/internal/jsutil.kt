package top.kagg886.eoa.gym.internal


import kotlinx.serialization.json.*
import okio.ByteString.Companion.encodeUtf8
import kotlin.collections.iterator

internal fun JsonObject.sign(sign: String): String = buildSignSource(sign)
    .encodeUtf8()
    .md5().hex()
    .uppercase()

private fun JsonObject.buildSignSource(sign: String): String {
    val keys = buildList {
        for ((key, value) in this@buildSignSource) {
            if (key == "code_names") add(key)
            if (value is JsonPrimitive && !value.isString && value.content == "0") {
                add(key)
            }
            if (key != "sign" && value != JsonNull && !value.jsEqualsEmptyString()) {
                add(key)
            }
        }
    }.sorted()

    val source = buildString {
        for (key in keys) {
            val value = getValue(key)
            append('&')
            append(key)
            append('=')
            append(value.signValue())
        }
    }

    return when {
        source.isEmpty() -> "key=$sign"
        "&key" in source -> source.drop(1)
        else -> "${source.drop(1)}&key=$sign"
    }
}

private fun JsonElement.signValue(): String {
    if (!jsTruthy()) return jsString()

    return when (this) {
        is JsonObject -> {
            // JS: isNaN(plainObject) 通常为 true
            val result = entries.associate { (key, value) ->
                require(value is JsonArray) {
                    "JS 原逻辑要求对象属性 $key 是数组"
                }
                key to JsonArray(value.map { it.sort() })
            }
            JsonObject(result).toString()
        }

        is JsonArray -> {
            JsonArray(
                content = map { element ->
                    if (element is JsonPrimitive && element.isString) {
                        element
                    } else {
                        element.sort()
                    }
                }
            ).toString()
        }

        is JsonPrimitive -> jsString()
    }
}

/**
 * 对应 JS Object.keys(value).sort()。
 * 只排序对象的第一层属性。
 */
private fun JsonElement.sort(): JsonObject = when (this) {
    is JsonObject -> JsonObject(entries.toList().sortedBy { it.key }
        .associate { it.key to it.value })

    is JsonArray -> JsonObject(
        indices.associate { index ->
            index.toString() to get(index)
        }
    )

    is JsonPrimitive -> {
        require(this != JsonNull) {
            "Object.keys(null) 会抛出异常"
        }

        JsonObject(emptyMap())
    }
}

/**
 * JavaScript Boolean(value)
 */
private fun JsonElement.jsTruthy(): Boolean = when (this) {
    JsonNull -> false
    is JsonObject, is JsonArray -> true
    is JsonPrimitive -> when {
        isString -> content.isNotEmpty()
        content == "false" -> false
        content == "0" || content == "-0" -> false
        else -> true
    }
}

/**
 * JavaScript String(value)
 */
private fun JsonElement.jsString(): String = when (this) {
    JsonNull -> "null"
    is JsonObject -> "[object Object]"
    is JsonArray -> joinToString(",") {
        if (it == JsonNull) "" else it.jsString()
    }

    is JsonPrimitive -> content
}

/**
 * JavaScript value == ""
 */
private fun JsonElement.jsEqualsEmptyString(): Boolean = when (this) {
    JsonNull -> false
    is JsonObject -> false
    is JsonArray -> jsString().isEmpty()
    is JsonPrimitive -> when {
        isString -> content.isEmpty()
        content == "false" -> true
        content == "true" -> false
        else -> content.toDoubleOrNull() == 0.0
    }
}