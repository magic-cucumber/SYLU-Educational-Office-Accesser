package top.kagg886.eoa.pages.main.home.gpa

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.GPAEntity
import top.kagg886.backend.database.dao.GPASummaryEntity
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute
import top.kagg886.eoa.pages.main.mainViewModel

@Serializable
data object GPARoute

@Composable
fun GPAScreen() = HomeScreen(
    route = NavigationRoute.GPA,
    title = { Text(text = "绩点") },
) {
    val mainModel = mainViewModel()
    val syncState by mainModel.collectAsState()

    val model = viewModel<GPAViewModel>(key = syncState.toString()) {
        GPAViewModel(syncState, mainModel.database)
    }
    model.collectSideEffect {

    }
    val state by model.collectAsState()

    GPAScreenContent(state)
}

@Composable
private fun GPAScreenContent(
    state: GPAState
) = when (state) {
    is GPAState.Failed -> {
        ErrorPage(
            modifier = Modifier.fillMaxSize(),
            title = { Text(text = "同步失败") },
            message = { Text(text = state.msg) },
        )
    }

    is GPAState.FailedButSuccess -> {
        ErrorPage(
            modifier = Modifier.fillMaxSize(),
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "提示",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            message = {
                Text(
                    text = state.msg,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

            }
        )
    }
    else -> {
        GPAContent(state as? GPAState.Success)
    }
}

@Composable
private fun GPAContent(
    state: GPAState.Success?
) {
    if (state == null) {
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.gpa.toList()) { (summary, scores) ->
            GPASummaryCard(
                summary = summary,
                scores = scores
            )
        }
    }
}

@Composable
private fun GPASummaryCard(
    summary: GPASummaryEntity,
    scores: List<GPAEntity>
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Summary header
            ListItem(
                headlineContent = {
                    Text(
                        text = summary.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                supportingContent = {
                    Text(
                        text = "绩点: ${summary.score}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    val rotate by animateFloatAsState(
                        targetValue =  if (expanded) 180f else 0f
                    )
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "收起" else "展开",
                            modifier =  Modifier.rotate(rotate)
                        )
                    }
                }
            )

            // Detailed scores (expandable with animation)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 300),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 300),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(durationMillis = 200))
            ) {
                Column {
                    scores.forEach { score ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = score.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = score.score,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            },
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
