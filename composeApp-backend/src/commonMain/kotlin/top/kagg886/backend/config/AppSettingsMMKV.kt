package top.kagg886.backend.config

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVMode
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.util.ColorAsArgbSerializer
import top.kagg886.util.DurationAsMillsSerializer
import top.kagg886.util.boolean
import top.kagg886.util.float
import top.kagg886.util.json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

object AppSettingsMMKV : MMKV by MMKV.mmkvWithID("app-settings", mode = MMKVMode.MULTI_PROCESS), AppSettingsMMKVType {
    /**
     * 内置的预设主题色
     */
    val presetsColor: Map<String, Color> = mapOf(
        "赤铁红" to Color(200, 8, 21),
        "落叶橙" to Color(255, 85, 34),
        "贝斯黄" to Color(255, 221, 136),
        "风祝绿" to Color(26, 240, 79),
        "拉格蓝" to Color(118, 145, 217),
    )

    override var theme: AppSettingsMMKVType.AppTheme by json(
        "theme",
        AppSettingsMMKVType.AppTheme.SystemDefault
    )
    override var color: Color by json("color", Color(255, 136, 153), Json {
        serializersModule = SerializersModule {
            contextual(Color::class, ColorAsArgbSerializer)
        }
    })

    override var showHolidayCourse: Boolean by boolean("show-holiday-course", false)

    override var systemWidgetRadius: Boolean by boolean("system-widget-radius", true)

    override var showExperimentClass: Boolean by boolean("show-experiment-class", true)

    override var hideWeekendCourse: Boolean by boolean("hide-weekend-course", true)

    override var animationSpeed: Float by float("animation-speed", 1f)

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
    var showHolidayCourse: Boolean

    var systemWidgetRadius: Boolean

    var showExperimentClass: Boolean

    var hideWeekendCourse: Boolean

    var animationSpeed: Float

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
