package top.kagg886.eoa.pages.main.home.link

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.ExpandableText
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.HomeScreen

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/2 10:04
 * ================================================
 */

@Serializable
data object LinkRoute

@Composable
fun LinkScreen() = HomeScreen(
    route = EOAHomeModule.LINK,
    fabIcon = { Icon(Icons.Default.Edit, "") },
    fabText = { Text("编辑友链") },
    fabOnClick = {}
) {
    val model = viewModel {
        LinkModel()
    }

    val state by model.collectAsState()

    LinkScreenContent(
        state = state,
    )
}

@Composable
fun LinkScreenContent(
    state: LinkState,
) {
    when (state) {
        is LinkState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is LinkState.Error -> ErrorPage(
            title = {
                Text("友链加载失败")
            },
            message = {
                Text(state.message)
            }
        )

        is LinkState.Success -> LazyColumn(Modifier.fillMaxSize()) {
            items(state.link) {
                val uri = LocalUriHandler.current
                ListItem(
                    headlineContent = { Text(it.name) },
                    supportingContent = {
                        var expand by remember {
                            mutableStateOf(false)
                        }
                        ExpandableText(
                            text = it.description,
                            maxLines = 3,
                            isExpanded = expand,
                            onExpandChange = { expand = !expand }
                        )
                    },
                    overlineContent = { Text(it.url) },
                    modifier = Modifier.fillMaxSize().clickable {
                        uri.openUri(it.url)
                    },
                )
            }
        }
    }
}
