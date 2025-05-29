package top.kagg886.eoa.pages.main.home.summary

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute
import top.kagg886.eoa.pages.main.mainViewModel

@Serializable
data object SummaryRoute

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SummaryScreen() = HomeScreen(NavigationRoute.SUMMARY) {
    val mainViewModel = mainViewModel()
    val syncState by mainViewModel.collectAsState()
    val model = viewModel<SummaryModel>(key = syncState.toString()) {
        SummaryModel(syncState,mainViewModel.database)
    }
    model.collectSideEffect {
        when(it){
            //TODO 没有任何effect
            else -> {}
        }
    }
    val state by model.collectAsState()
    SummaryContent(
        state = state
    )
}

@Composable
private fun SummaryContent(state: SummaryState) {
    when (state) {
        is SummaryState.Loading -> {

        }
        is SummaryState.Success -> {
            Text(state.plan.toString())
        }
        is SummaryState.Failed -> {}
        is SummaryState.FailedButSuccess -> {}
    }
}
