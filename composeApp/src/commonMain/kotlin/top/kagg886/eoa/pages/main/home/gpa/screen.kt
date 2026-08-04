package top.kagg886.eoa.pages.main.home.gpa

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.backend.database.dao.GPAEntity
import top.kagg886.backend.database.dao.GPASummaryEntity
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.pages.main.MainRouteViewState.Empty.toViewModelKey
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.mainViewModelOrNull

@Serializable
data object GPARoute

@Composable
fun GPAScreen() = HomeScreen(
    route = EOAHomeModule.GPA,
    title = { Text(text = "绩点") },
) {
    val mainModel = mainViewModelOrNull() ?: return@HomeScreen
    val syncState by mainModel.collectAsState()

    val model = viewModel<GPAViewModel>(key = syncState.toViewModelKey()) {
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

    if (state.gpa.isEmpty()) {
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
                    text = "没有大创学分！",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            message = {
                Text(
                    text = "或许您需要再努努力？",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        )
     }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        items(
            items = state.gpa.toList(),
            key = { (summary, _) -> summary.id ?: summary.name }
        ) { (summary, scores) ->
            GPASummaryGroup(
                summary = summary,
                scores = scores
            )
        }
    }
}

@Composable
private fun GPASummaryGroup(
    summary: GPASummaryEntity,
    scores: List<GPAEntity>
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 汇总头部，整行可点击展开/收起
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "绩点: ${summary.score}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val rotate by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "expand-arrow"
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotate)
            )
        }

        // 明细条目，展开/收起与箭头旋转使用同一弹性曲线
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                expandFrom = Alignment.Top
            ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = shrinkVertically(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                shrinkTowards = Alignment.Top
            ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 12.dp)
            ) {
                if (scores.isEmpty()) {
                    Text(
                        text = "暂无条目",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    scores.forEach { score ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = score.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = score.score,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()
    }
}
