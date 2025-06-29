package top.kagg886.eoa.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import co.touchlab.kermit.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSettingsMMKVType
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.backend.database.databaseBuilder
import top.kagg886.eoa.LocalGlobalViewModelStoreOwner
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
            state.ktorLogLevel.collect {
                AppSettingsMMKV.ktorLogLevel = it
            }
        }

        viewModelScope.launch {
            state.color.collect {
                AppSettingsMMKV.color = it
            }
        }
    }

    fun postNewColorSetting(color: Color) = intent {
        state.color.value = color
    }

    fun postNewThemeSetting(theme: AppSettingsMMKVType.AppTheme) = intent {
        state.theme.value = theme
    }

    fun postNewKtorLogLevelSetting(level: AppSettingsMMKVType.LogLevel) = intent {
        state.ktorLogLevel.value = level
        postSideEffect(RootEffect.Toast("重启生效"))
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

}

data class RootState(
    val color: MutableStateFlow<Color> = MutableStateFlow(AppSettingsMMKV.color),
    val theme: MutableStateFlow<AppSettingsMMKVType.AppTheme> = MutableStateFlow(AppSettingsMMKV.theme),
    val ktorLogLevel: MutableStateFlow<AppSettingsMMKVType.LogLevel> = MutableStateFlow(AppSettingsMMKV.ktorLogLevel),
)

sealed interface RootEffect {
    data class Toast(val msg: String) : RootEffect
}
