package top.kagg886.eoa.util.internal

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/19 23:57
 * ================================================
 */

internal val chars = "ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678".toCharArray()
internal fun random(length: Int) = (1..length).map { chars.random() }.joinToString("")
