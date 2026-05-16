package top.kagg886.eoa.pages.logcat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import co.touchlab.kermit.Severity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.buffered
import kotlinx.io.writeString
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.backend.database.dao.log
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.logger
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class LogcatModel(private val database: AppDatabase) : ViewModel(), ContainerHost<LogcatState, LogcatSideEffect> {
    private val appLogDao = database.appLogDao()
    override val container: Container<LogcatState, LogcatSideEffect> = container(LogcatState.Loading) { all().join() }

    fun all(level: Severity? = Severity.Info) = intent {
        val data = Pager(
            config = PagingConfig(
                10,
                enablePlaceholders = false
            )
        ) { appLogDao.getLogsByPage(level?.ordinal) }.flow.cachedIn(viewModelScope)

        reduce {
            LogcatState.LoadingSuccess(level, data)
        }
    }

    fun clean() = intent {
        withContext(Dispatchers.IO) {
            appLogDao.clear()
        }
        postSideEffect(LogcatSideEffect.ShowToast(SnackBarType.Success, "日志已清空"))
    }

    fun test() = intent {
        logger.i("这是一条测试log")
    }

    fun export() = intent {
        val file = FileKit.openFileSaver(
            suggestedName = "EOA log export - ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())}",
            extension = "log",
        )

        if (file == null) {
            postSideEffect(LogcatSideEffect.ShowToast(SnackBarType.Warning, "用户取消了日志导出"))
            return@intent
        }

        val data = withContext(Dispatchers.IO) {
            database.log()
        }

        val count = appLogDao.count().toFloat()

        @OptIn(OrbitExperimental::class)
        runOn<LogcatState.LoadingSuccess> {
            reduce {
                state.copy(exporting = true, exportingProgress = 0, exportingAll = count)
            }

            withContext(Dispatchers.IO) {
                val sink = file.sink().buffered()
                @OptIn(ExperimentalCoroutinesApi::class)
                data.chunked(20).collect { data -> //flush data in every 20 lines.
                    sink.writeString(
                        data.joinToString("\n") { bean ->
                            val stacktrace = bean.stacktrace?.let { "\n${it}" } ?: ""
                            with(bean) {
                                "$time ${level.name.take(1)}/$tag: $message$stacktrace"
                            }
                        }
                    )
                    sink.flush()
                    reduce {
                        state.copy(exportingProgress = state.exportingProgress + data.size)
                    }
                }
            }

            postSideEffect(LogcatSideEffect.ShowToast(SnackBarType.Success, "日志导出成功"))

            reduce {
                state.copy(exporting = false, exportingProgress = 0, exportingAll = 0f)
            }
        }
    }
}


sealed class LogcatState {
    data object Loading : LogcatState()
    data class LoadingSuccess(
        val severity: Severity?,
        val flow: Flow<PagingData<AppLog>>,

        val exporting: Boolean = false,
        val exportingProgress: Int = 0,
        val exportingAll: Float = 0f,
    ) : LogcatState()
}

sealed interface LogcatSideEffect {
    data class ShowToast(val level: SnackBarType, val message: String) : LogcatSideEffect
}
