package top.kagg886.backend.config

import androidx.compose.ui.graphics.Color
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
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

    override var ktorLogLevel: LogLevel by json(
        "ktor-log-level",
        LogLevel.HEADERS
    )
}

sealed interface AppSettingsMMKVType {
    var color: Color
    var theme: AppTheme
    var ktorLogLevel: LogLevel

    enum class AppTheme {
        Light,
        SystemDefault,
        Dark;
    }
}