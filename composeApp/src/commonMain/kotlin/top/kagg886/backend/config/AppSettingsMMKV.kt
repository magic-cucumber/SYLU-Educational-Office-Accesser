package top.kagg886.backend.config

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.util.ColorAsArgbSerializer
import top.kagg886.util.DurationAsMillsSerializer
import top.kagg886.util.boolean
import top.kagg886.util.json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

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

    override var systemWidgetRadius: Boolean by boolean("system-widget-radius",true)

    override var syncDuration: Duration by json("duration", 7.days, Json {
        serializersModule = SerializersModule {
            contextual(Duration::class, DurationAsMillsSerializer)
        }
    })

    override var homeModule: List<EOAHomeModule> by json(
        "home-module", listOf(
            EOAHomeModule.SUMMARY,
            EOAHomeModule.COURSE,
            EOAHomeModule.EXAM,
        )
    )
}

sealed interface AppSettingsMMKVType {
    var color: Color
    var theme: AppTheme

    var systemWidgetRadius: Boolean

    var homeModule: List<EOAHomeModule>

    var syncDuration: Duration

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
}
