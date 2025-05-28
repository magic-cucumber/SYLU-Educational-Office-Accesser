package top.kagg886.util

import co.touchlab.kermit.Severity
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVOptions
import top.kagg886.mkmb.initialize
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass

fun initializeMMKV() = MMKV.initialize(dataPath.resolve("config").absolutePath().toString()) {
    logFunc = { level,tag,it->
        logger.log(
            severity = when (level) {
                MMKVOptions.LogLevel.Debug -> Severity.Debug
                MMKVOptions.LogLevel.Info -> Severity.Info
                MMKVOptions.LogLevel.Warning -> Severity.Warn
                MMKVOptions.LogLevel.Error -> Severity.Error
                MMKVOptions.LogLevel.None -> Severity.Assert
            },
            tag = "MMKV $tag",
            message = it,
            throwable = null
        )
    }
}

fun MMKV.string(key: String, default: String = "") =
    Delegates.observable(getOrElse(key, default) { getString(key) }) { _, _, new ->
        set(key, new)
    }

fun MMKV.int(key: String, default: Int = 0) =
    Delegates.observable(getOrElse(key, default) { getInt(key) }) { _, _, new ->
        set(key, new)
    }

fun MMKV.boolean(key: String, default: Boolean = false) =
    Delegates.observable(getOrElse(key, default) { getBoolean(key) }) { _, _, new ->
        set(key, new)
    }

fun MMKV.long(key: String, default: Long = 0L) =
    Delegates.observable(getOrElse(key, default) { getLong(key) }) { _, _, new ->
        set(key, new)
    }

fun MMKV.float(key: String, default: Float = 0f) =
    Delegates.observable(getOrElse(key, default) { getFloat(key) }) { _, _, new ->
        set(key, new)
    }

fun MMKV.double(key: String, default: Double = 0.0) =
    Delegates.observable(getOrElse(key, default) { getDouble(key) }) { _, _, new ->
        set(key, new)
    }

fun MMKV.bytes(key: String, default: ByteArray = byteArrayOf()) =
    Delegates.observable(getOrElse(key, default) { getByteArray(key) }) { _, _, new ->
        set(key, new)
    }

fun MMKV.stringSet(key: String, default: List<String> = listOf()) =
    Delegates.observable(getOrElse(key, default) { getStringList(key) }) { _, _, new ->
        set(key, new)
    }


@OptIn(InternalSerializationApi::class)
fun <T : Any> MMKV.jsonOrNull(key: String, default: T? = null, clazz: KClass<T>, json: Json = Json): ReadWriteProperty<Any?, T?> {
    val data = try {
        json.decodeFromString(clazz.serializer(), getOrElse(key, "{}") { getString(key) })
    } catch (_: Exception) {
        default
    }
    return Delegates.observable(data) { _, _, new ->
        if (new != null) {
            set(key, json.encodeToString(clazz.serializer(), new))
            return@observable
        }
        remove(key)
    }
}

inline fun <reified T : Any> MMKV.jsonOrNull(key: String, default: T? = null, json: Json = Json) =
    jsonOrNull(key, default, T::class, json)

@OptIn(InternalSerializationApi::class)
fun <T : Any> MMKV.json(key: String, default: T, clazz: KClass<T>, json: Json = Json): ReadWriteProperty<Any?, T> {
    val data = try {
        json.decodeFromString(clazz.serializer(), getOrElse(key, "{}") { getString(key) })
    } catch (_: Exception) {
        default
    }
    return Delegates.observable(data) { _, _, new ->
        if (new != null) {
            set(key, json.encodeToString(clazz.serializer(), new))
            return@observable
        }
        remove(key)
    }
}

inline fun <reified T : Any> MMKV.json(key: String, default: T, json: Json = Json) =
    json(key, default, T::class, json)


private fun <T> MMKV.getOrElse(key: String, fallback: T, value: MMKV.() -> T): T =
    if (exists(key)) value() else fallback
