package top.kagg886.eoa.pages.main.logcat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import co.touchlab.kermit.Severity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.backend.database.dao.AppLogDao
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.logger

class LogcatModel(private val appLogDao: AppLogDao) : ViewModel(), ContainerHost<LogcatState, LogcatSideEffect> {
    override val container: Container<LogcatState, LogcatSideEffect> = container(LogcatState.Loading) { all().join() }

    fun all(level: Severity? = Severity.Info) = intent {
        val data = Pager(config = PagingConfig(10)) { appLogDao.getLogsByPage(level?.ordinal) }.flow.cachedIn(viewModelScope)

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
        val data = withContext(Dispatchers.IO) {
            appLogDao.getLogs()
        }
        val file = FileKit.openFileSaver(
            suggestedName = "EOA log export - ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())}",
            extension = "log",
        )

        if (file == null) {
            postSideEffect(LogcatSideEffect.ShowToast(SnackBarType.Warning, "用户取消了日志导出"))
            return@intent
        }

        withContext(Dispatchers.IO) {
            file.writeString(data.joinToString("\n") { bean ->
                val stacktrace = bean.stacktrace?.let { "\n${it}" } ?: ""
                with(bean) {
                    "$time ${level.name.take(1)}/$tag: $message$stacktrace"
                }
            })
        }

        postSideEffect(LogcatSideEffect.ShowToast(SnackBarType.Success, "日志导出成功"))
    }
}


sealed class LogcatState {
    data object Loading : LogcatState()
    data class LoadingSuccess(
        val severity: Severity?,
        val flow: Flow<PagingData<AppLog>>
    ) : LogcatState()
}

sealed interface LogcatSideEffect {
    data class ShowToast(val level: SnackBarType, val message: String) : LogcatSideEffect
}
