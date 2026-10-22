@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package top.kagg886.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual fun openAppSystemSettings() {
    UIApplication.sharedApplication.openURL(NSURL(string = UIApplicationOpenSettingsURLString))
}
