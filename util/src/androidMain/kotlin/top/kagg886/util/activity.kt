package top.kagg886.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/6/13 13:32
 * ================================================
 */

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
