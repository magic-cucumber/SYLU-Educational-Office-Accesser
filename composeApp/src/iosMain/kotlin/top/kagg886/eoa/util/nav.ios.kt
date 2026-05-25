package top.kagg886.eoa.util

import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController
import androidx.navigation.NavUri

actual fun NavHostController.handleDeepLink(uri: String) {
    handleDeepLink(NavDeepLinkRequest.Builder.fromUri(NavUri(uri)).build())
}
