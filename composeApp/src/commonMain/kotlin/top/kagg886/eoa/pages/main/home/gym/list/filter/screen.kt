package top.kagg886.eoa.pages.main.home.gym.list.filter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.eoa.component.drawer.DrawerSheetPageScaffold
import top.kagg886.eoa.component.drawer.DrawerSheetPopupDirection
import top.kagg886.eoa.gym.GymSummary
import top.kagg886.eoa.pages.main.home.gym.GymScreen
import top.kagg886.eoa.pages.main.home.gym.GymState
import top.kagg886.eoa.pages.main.home.gym.gymViewModel
import top.kagg886.eoa.pages.main.home.gym.list.asDisplayName

@Serializable
data object GymListFilterRoute

@Composable
fun GymListFilterScreen() = GymScreen {
    val model = gymViewModel() ?: return@GymScreen
    val state by model.collectAsState()
    val successState = state as? GymState.Success
    DrawerSheetPageScaffold(direction = DrawerSheetPopupDirection.RIGHT) {
        GymListScreenDrawer(
            selector = successState?.value?.keys?.toList().orEmpty(),
            currentSummaryIndex = successState?.currentSummaryIndex ?: 0,
            onSummaryChanged = { model.filterSummary(it) },
        )
    }
}

@Composable
private fun GymListScreenDrawer(
    selector: List<GymSummary>,
    currentSummaryIndex: Int,
    onSummaryChanged: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("选择学期", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        selector.forEachIndexed { index, summary ->
            NavigationDrawerItem(
                label = { Text(summary.asDisplayName()) },
                selected = index == currentSummaryIndex,
                onClick = { onSummaryChanged(index) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
