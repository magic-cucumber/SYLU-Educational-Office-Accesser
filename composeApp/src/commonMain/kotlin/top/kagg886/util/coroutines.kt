package top.kagg886.util

import kotlinx.coroutines.cancel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.selects.select

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/6/11 09:00
 * ================================================
 */

suspend fun <T> race(vararg pending: Deferred<T>): T {
    require(pending.isNotEmpty()) { "pendings must not be empty" }

    val remaining = pending.toMutableList()
    var firstFailure: Throwable? = null

    while (remaining.isNotEmpty()) {
        val deferred = select {
            remaining.forEach { deferred ->
                deferred.onJoin { deferred }
            }
        }

        remaining.remove(deferred)

        try {
            val result = deferred.await()
            remaining.forEach { it.cancel() }
            return result
        } catch (e: Throwable) {
            firstFailure?.addSuppressed(e) ?: run {
                firstFailure = e
            }
        }
    }

    throw firstFailure ?: error("all pendings completed without result")
}
