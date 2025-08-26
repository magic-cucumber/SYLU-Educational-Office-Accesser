package top.kagg886.eoa.util

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.ktor.utils.io.*
import io.ktor.utils.io.locks.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import top.kagg886.backend.database.dao.AppLog
import top.kagg886.backend.database.dao.AppLogDao
import kotlin.time.Clock

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/23 09:18
 * ================================================
 */

@OptIn(InternalAPI::class)
private val lock = reentrantLock()

@OptIn(InternalAPI::class)
fun registerKermitLoggerIfExists(appLogDao: AppLogDao): Unit = lock.withLock {
    if (Logger.mutableConfig.logWriterList.indexOfFirst { it is DatabaseLogger } != -1) return@withLock
    Logger.addLogWriter(DatabaseLogger(appLogDao))
}

private class DatabaseLogger(private val appLogDao: AppLogDao) : LogWriter() {
    private val scope = CoroutineScope(Dispatchers.IO)
    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?
    ) {
        scope.launch {
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

}
