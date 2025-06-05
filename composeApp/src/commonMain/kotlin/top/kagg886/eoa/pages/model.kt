package top.kagg886.eoa.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import co.touchlab.kermit.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.datetime.Clock
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.backend.database.databaseBuilder
import top.kagg886.eoa.LocalNavController
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.logger
import kotlin.time.Duration.Companion.days

@Composable
fun rootViewModel(): RootViewModel {
    val nav = LocalNavController.current
    val parentEntry = remember {
        nav.getBackStackEntry(RootRoute) // 嵌套图 route
    }
    return viewModel(parentEntry) {
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

    override val container: Container<RootState, RootEffect> = container(RootState.Empty) {
        appLogDao.clear((Clock.System.now() - 1.days).toEpochMilliseconds())
        "RootViewModel".asTaggedLogger.i("日志清理成功。")
    }

    fun log(severity: Severity, tag: String, message: String, throwable: Throwable?) = intent {
        appLogDao.insert(
            AppLog(
                tag = tag,
                level = severity,
                message = message,
                time = Clock.System.now().toEpochMilliseconds(),
                stacktrace = throwable?.stackTraceToString()
            )
        )
    }
}

sealed interface RootState {
    data object Empty : RootState
}

sealed interface RootEffect {

}