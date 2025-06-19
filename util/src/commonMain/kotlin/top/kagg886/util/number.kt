package top.kagg886.util

import kotlin.math.pow
import kotlin.math.roundToLong

fun Number.toFixed(digits: Int): String {
    require(digits >= 0) { "digits must be non-negative" }
    val factor = 10.0.pow(digits)
    val rounded = when (this) {
        is Double -> (this * factor).roundToLong() / factor
        is Float -> (this * factor).roundToLong() / factor
        is Int, is Long, is Short, is Byte -> (this.toDouble() * factor).roundToLong() / factor
        else -> throw IllegalArgumentException("Unsupported number type: ${this::class}")
    }

    // 手动格式化为字符串并填充小数位
    val parts = rounded.toString().split(".")
    val decimal = parts.getOrNull(1)?.padEnd(digits, '0') ?: "0".repeat(digits)
    return if (digits == 0) parts[0] else "${parts[0]}.$decimal"
}
