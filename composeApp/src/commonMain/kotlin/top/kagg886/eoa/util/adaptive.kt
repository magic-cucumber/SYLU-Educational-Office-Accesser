package top.kagg886.eoa.util

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import top.kagg886.util.asTaggedLogger

@Composable
fun currentLayoutType(): NavigationSuiteType {
    val (windowWidth, windowHeight) = with(LocalDensity.current) {
        val size = LocalWindowInfo.current.containerSize
        val width = size.width.toDp()
        val height = size.height.toDp()
        width to height
    }

    LaunchedEffect(windowWidth, windowHeight) {
        "adaptive.kt".asTaggedLogger.d("当前窗口大小：$windowWidth x $windowHeight")
    }

    return if (windowWidth >= 1200.dp) NavigationSuiteType.NavigationDrawer else NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
}

/** Whether navigation is placed along the bottom rather than the side. */
val NavigationSuiteType.isNavigationBar: Boolean
    get() = this == NavigationSuiteType.ShortNavigationBarCompact ||
            this == NavigationSuiteType.ShortNavigationBarMedium ||
            this == NavigationSuiteType.NavigationBar
