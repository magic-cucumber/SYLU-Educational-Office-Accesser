package top.kagg886.eoa.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.pages.main.MainRoute

/**
 * 添加了安全检查的返回按钮。
 * 如果当前页面是导航栈的第一个页面，则不会进行任何操作。
 */
@Composable
fun BackIconButton(
    nav: NavHostController = LocalNavController.current
) {
    val entryList by nav.currentBackStack.collectAsState()
    val currentEntry by nav.currentBackStackEntryFlow.collectAsState(null)

    val backButtonEnabled by remember(entryList, currentEntry) {
        derivedStateOf {
            entryList.size > 1 && currentEntry != null
        }
    }

    IconButton(
        onClick = {
            nav.popBackStack(MainRoute,false)
        },
        enabled = backButtonEnabled
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回"
        )
    }
}