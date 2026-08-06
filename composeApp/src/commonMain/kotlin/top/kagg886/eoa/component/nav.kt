package top.kagg886.eoa.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import top.kagg886.eoa.LocalNavController

/**
 * 添加了安全检查的返回按钮。
 * 如果当前页面是导航栈的第一个页面，则不会进行任何操作。
 */
@Composable
fun BackIconButton(
    nav: NavHostController = LocalNavController.current,
    icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回"
        )
    },
    modifier: Modifier = Modifier,
    onBackPressed: (NavHostController) -> Unit = { it.popBackStack() }
) {
    val entryList by nav.currentBackStack.collectAsState()
    val currentEntry by nav.currentBackStackEntryFlow.collectAsState(null)

    val backButtonEnabled by remember(entryList, currentEntry) {
        derivedStateOf {
            entryList.size > 1 && currentEntry != null
        }
    }

    IconButton(
        onClick = { onBackPressed(nav) },
        enabled = backButtonEnabled,
        modifier = modifier,
        content = icon
    )
}
