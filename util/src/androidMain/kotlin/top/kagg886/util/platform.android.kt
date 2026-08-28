package top.kagg886.util

import android.content.res.Resources
import android.os.Build

actual val Platform.Companion.current: Platform by lazy {
    val float = with(Resources.getSystem().displayMetrics) {
        widthPixels / heightPixels.toFloat()
    }
    if (float > 1.0f) Platform.Android.Pad(Build.VERSION.SDK_INT) else Platform.Android.Phone(Build.VERSION.SDK_INT)
}
