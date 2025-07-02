package top.kagg886.eoa.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import co.touchlab.kermit.Severity
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.backend.database.databaseBuilder
import top.kagg886.eoa.LocalGlobalViewModelStoreOwner
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.update.UpdateInfo
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.asTaggedLogger
import kotlin.time.Duration.Companion.days

@Composable
fun rootViewModel(): RootViewModel {
    val scope = LocalGlobalViewModelStoreOwner.current
    return viewModel(scope) {
        RootViewModel()
    }
}

class RootViewModel : ViewModel(), ContainerHost<RootState, RootEffect> {
    private val logger = "RootViewModel".asTaggedLogger
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    val database: AppDatabase = databaseBuilder().apply {
        fallbackToDestructiveMigrationOnDowngrade(true)
        fallbackToDestructiveMigration(true)
        fallbackToDestructiveMigrationFrom(true, 1)
        setQueryCoroutineContext(Dispatchers.IO)
    }.build()

    val appLogDao = database.appLogDao()

    override val container: Container<RootState, RootEffect> = container(RootState()) {
        appLogDao.clear((Clock.System.now() - 1.days).toEpochMilliseconds())
        "RootViewModel".asTaggedLogger.i("日志清理成功。")

        viewModelScope.launch {
            state.theme.collect {
                AppSettingsMMKV.theme = it
            }
        }

        viewModelScope.launch {
            state.module.collect {
                AppSettingsMMKV.homeModule = it
            }
        }

        viewModelScope.launch {
            state.color.collect {
                AppSettingsMMKV.color = it
            }
        }

        checkUpdate()
        checkAnnouncement()
    }

    fun postNewColorSetting(color: Color) = intent {
        state.color.value = color
    }

    fun postNewThemeSetting(theme: AppSettingsMMKVType.AppTheme) = intent {
        state.theme.value = theme
    }

    fun postEOAModuleSetting(module: List<EOAHomeModule>) = intent {
        state.module.value = module
    }

    fun log(severity: Severity, tag: String, message: String, throwable: Throwable?) = intent {
        appLogDao.insert(
            AppLog(
                tag = tag,
                level = severity,
                message = message,
                time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                stacktrace = throwable?.stackTraceToString()
            )
        )
    }


    fun checkUpdate() = intent {
        val info = try {
            client.get("https://gitee.com/api/v5/repos/kagg886/sylu-educational-office-accesser/releases/latest")
                .body<UpdateInfo>()
        } catch (e: Exception) {
            logger.w("检查更新失败", e)
            postSideEffect(RootEffect.Toast(SnackBarType.Error, "检查更新失败，请检查网络连接。"))
            return@intent
        }
        if (info.name != BuildConfig.APP_VERSION_NAME) {
            postSideEffect(RootEffect.NavigateToUpdatePage(info))
        } else {
            postSideEffect(RootEffect.Toast(SnackBarType.Success, "已是最新版本"))
        }
    }

    fun checkAnnouncement() = intent {
        val latest = try {
            client.get("https://gitee.com/kagg886/sylu-educational-office-accesser/raw/master-4.0/runtime/broadcast.md")
                .bodyAsBytes().decodeToString()
        } catch (e: Exception) {
            logger.w("检查公告失败", e)
            return@intent
        }

        val exists = AppInitializeMMKV.announce

        if (latest != exists) {
            AppInitializeMMKV.announce = latest
            postSideEffect(RootEffect.NavigateToAnnouncePage(latest))
        }
    }
}

data class RootState(
    val color: MutableStateFlow<Color> = MutableStateFlow(AppSettingsMMKV.color),
    val theme: MutableStateFlow<AppSettingsMMKVType.AppTheme> = MutableStateFlow(AppSettingsMMKV.theme),
    val module: MutableStateFlow<List<EOAHomeModule>> = MutableStateFlow(AppSettingsMMKV.homeModule),
)

sealed interface RootEffect {
    data class Toast(val type: SnackBarType, val msg: String) : RootEffect
    data class NavigateToUpdatePage(val data: UpdateInfo) : RootEffect
    data class NavigateToAnnouncePage(val data: String) : RootEffect
}
