package top.kagg886.eoa.util

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import top.kagg886.eoa.component.adaptive.NavigationSuiteScaffoldDefaults
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.util.logger

@Composable
fun currentLayoutType(): NavigationSuiteType {
    val (windowWidth,windowHeight) = with(LocalDensity.current) {
        val size = LocalWindowInfo.current.containerSize
        val width = size.width.toDp()
        val height = size.height.toDp()
        width to height
    }

    LaunchedEffect(windowWidth,windowHeight) {
        logger.d("当前窗口大小：$windowWidth x $windowHeight")
    }

    val layoutType = if (windowWidth >= 1200.dp) {
        NavigationSuiteType.NavigationDrawer
    } else {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
            currentWindowAdaptiveInfo()
        )
    }

    LaunchedEffect(layoutType) {
        logger.d("当前窗口布局：$layoutType")
    }

    return layoutType
}