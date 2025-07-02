package top.kagg886.eoa.pages.main.home.link

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mikepenz.markdown.m3.Markdown
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.eoa.component.ErrorPage
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
fun LinkScreen() = HomeScreen(route = EOAHomeModule.LINK) {
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

        is LinkState.Success -> Markdown(
            content = state.link,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        )
    }
}
