package top.kagg886.util

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * 将数字格式化为固定小数位字符串。
 *
 * 示例：
 * 1.234.toFixed(2) -> "1.23"
 * 1.239.toFixed(2) -> "1.24"
 * (-1.239).toFixed(2) -> "-1.24"
 * 1.toFixed(3) -> "1.000"
 */
fun Number.toFixed(digits: Int): String {
    // 小数位数不能小于 0
    require(digits >= 0) {
        "digits must be non-negative"
    }

    // 统一转为 Double 处理
    val value = this.toDouble()

    // 记录符号，后续使用绝对值计算
    val negative = value < 0

    // 10^digits
    // 例如 digits = 2 时 factor = 100
    val factor = 10.0.pow(digits)

    // 四舍五入：
    //
    // 1. 先乘以 factor 放大
    // 2. +0.5 后 floor 实现四舍五入
    // 3. 转为 Long 避免后续浮点误差
    //
    // 示例：
    // 1.239 * 100 = 123.9
    // 123.9 + 0.5 = 124.4
    // floor(124.4) = 124
    val scaled = floor(abs(value) * factor + 0.5).toLong()

    // 整数部分
    //
    // 例如：
    // 124 / 100 = 1
    val integerPart = scaled / factor.toLong()

    // 小数部分
    //
    // 例如：
    // 124 % 100 = 24
    val decimalPart = scaled % factor.toLong()

    return buildString {
        // 补负号
        if (negative) {
            append('-')
        }

        // 写入整数部分
        append(integerPart)

        // 如果需要小数部分
        if (digits > 0) {
            append('.')

            // 小数部分左侧补零
            //
            // 例如：
            // 5 -> "05"
            append(
                decimalPart
                    .toString()
                    .padStart(digits, '0')
            )
        }
    }
}
