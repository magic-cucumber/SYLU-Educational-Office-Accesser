package io.github.oshai.kotlinlogging
import co.touchlab.kermit.Logger
import top.kagg886.util.asTaggedLogger

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/2 10:37
 * ================================================
 */
object KotlinLogging {
    fun logger(func: () -> Unit): KLogger = logger(func::class.qualifiedName ?: "Koog")

    fun logger(name: String): KLogger = name.asTaggedLogger
}

typealias KLogger = Logger


inline fun KLogger.info(block: () -> String) = i(message = block)
inline fun KLogger.debug(block: ()-> String) = d(message = block)
inline fun KLogger.error(exception: Throwable? = null,block: () -> String) = e(message = block, throwable = exception)
inline fun KLogger.warn(exception: Throwable? = null,block: () -> String) = w(message = block,throwable = exception)
