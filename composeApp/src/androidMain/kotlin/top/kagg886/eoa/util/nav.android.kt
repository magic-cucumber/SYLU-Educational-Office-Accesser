package top.kagg886.eoa.util

import android.net.Uri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController

actual fun NavHostController.handleDeepLink(uri: String) {
    // avoid [NavUri::class] not found
    // f**k jb
    handleDeepLink(NavDeepLinkRequest.Builder.fromUri(Uri.parse(uri)).build())
}
