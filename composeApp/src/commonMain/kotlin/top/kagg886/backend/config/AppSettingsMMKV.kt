package top.kagg886.backend.config

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.util.ColorAsArgbSerializer
import top.kagg886.util.json

object AppSettingsMMKV : MMKV by MMKV.mmkvWithID("app-settings"), AppSettingsMMKVType {
    override var theme: AppSettingsMMKVType.AppTheme by json(
        "theme",
        AppSettingsMMKVType.AppTheme.SystemDefault
    )
    override var color: Color by json("color", Color(255, 136, 153), Json {
        serializersModule = SerializersModule {
            contextual(Color::class, ColorAsArgbSerializer)
        }
    })

    override var ktorLogLevel: AppSettingsMMKVType.LogLevel by json(
        "ktor-log-level",
        AppSettingsMMKVType.LogLevel.HEADERS
    )
}

sealed interface AppSettingsMMKVType {
    var color: Color
    var theme: AppTheme
    var ktorLogLevel: LogLevel

    /**
     * 需要@Serializable支持
     * 使得iOS程序不崩溃
     */
    @Serializable
    enum class AppTheme {
        Light,
        SystemDefault,
        Dark;
    }

    @Serializable
    enum class LogLevel {
        ALL,
        HEADERS,
        BODY,
        INFO,
        NONE;

        fun toKtorLogLevel(): io.ktor.client.plugins.logging.LogLevel =
            io.ktor.client.plugins.logging.LogLevel.valueOf(name)
    }
}
