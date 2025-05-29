package top.kagg886.eoa.util

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.util.logger

@Composable
fun currentLayoutType(): NavigationSuiteType {
    val (w,h) = with(currentWindowAdaptiveInfo()) {
        windowSizeClass.windowWidthSizeClass to windowSizeClass.windowHeightSizeClass
    }

    LaunchedEffect(w,h) {
        logger.i("current window layout type: $w,$h")
    }

    //  w\h         COMPACT           MEDIUM           EXPAND
    //COMPACT    NavigationRail    NavigationRail    NavigationBar
    //MEDIUM     NavigationRail    NavigationRail   NavigationRail
    //EXPAND    NavigationDrawer   NavigationRail   NavigationRail
    return when {
        w == WindowWidthSizeClass.COMPACT && h == WindowHeightSizeClass.COMPACT -> NavigationSuiteType.NavigationBar
        w == WindowWidthSizeClass.COMPACT && h == WindowHeightSizeClass.MEDIUM -> NavigationSuiteType.NavigationRail
        w == WindowWidthSizeClass.COMPACT && h == WindowHeightSizeClass.EXPANDED -> NavigationSuiteType.NavigationRail
        w == WindowWidthSizeClass.MEDIUM && h == WindowHeightSizeClass.COMPACT -> NavigationSuiteType.NavigationRail
        w == WindowWidthSizeClass.MEDIUM && h == WindowHeightSizeClass.MEDIUM -> NavigationSuiteType.NavigationRail
        w == WindowWidthSizeClass.MEDIUM && h == WindowHeightSizeClass.EXPANDED -> NavigationSuiteType.NavigationRail
        w == WindowWidthSizeClass.EXPANDED && h == WindowHeightSizeClass.COMPACT -> NavigationSuiteType.NavigationDrawer
        w == WindowWidthSizeClass.EXPANDED && h == WindowHeightSizeClass.MEDIUM -> NavigationSuiteType.NavigationRail
        w == WindowWidthSizeClass.EXPANDED && h == WindowHeightSizeClass.EXPANDED -> NavigationSuiteType.NavigationRail
        else -> error("unreachable")
    }
}
