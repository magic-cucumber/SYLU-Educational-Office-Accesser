package top.kagg886.eoa.pages.main.home.exam.statistic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.util.toFixed

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/6 15:01
 * ================================================
 */

@Serializable
data class ExamStatisticRoute(val year: String, val term: String)

@Composable
fun ExamStatisticScreen(route: ExamStatisticRoute) = DialogPageScaffold(
    title = {
        Text("绩点统计")
    },
    confirmButton = {
        val nav = LocalNavController.current
        TextButton(
            onClick = { nav.popBackStack() }
        ) {
            Text("关闭")
        }
    }
) {
    val mainModel = mainViewModelOrNull() ?: return@DialogPageScaffold

    val model = viewModel {
        ExamStatisticModel(mainModel.database, route.year, route.term)
    }

    val state by model.collectAsState()

    ExamStatisticContent(state)
}

@Composable
private fun ExamStatisticContent(state: ExamStatisticState) = when (state) {
    is ExamStatisticState.Success -> ExamStatisticSuccess(state)

    else -> Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ExamStatisticSuccess(state: ExamStatisticState.Success) {
    val hasCurrentTerm = state.avgScore != null && state.avgPoint != null && state.avgScoreMultiPoint != null
    var selection by remember { mutableIntStateOf(if (hasCurrentTerm) 0 else 1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selection == 0,
                onClick = { selection = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                enabled = hasCurrentTerm,
                icon = {},
                label = { Text("当前学期") }
            )
            SegmentedButton(
                selected = selection == 1,
                onClick = { selection = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {},
                label = { Text("全部学期") }
            )
        }

        val (avgScore, avgPoint, avgScoreMultiPoint) = if (selection == 0 && hasCurrentTerm) {
            Triple(state.avgScore, state.avgPoint, state.avgScoreMultiPoint)
        } else {
            Triple(state.allAvgScore, state.allAvgPoint, state.allAvgScoreMultiPoint)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            StatisticItem(label = "平均学分", value = avgScore.toFixed(2))
            HorizontalDivider()
            StatisticItem(label = "平均绩点", value = avgPoint.toFixed(2))
            HorizontalDivider()
            StatisticItem(label = "学分绩点", value = avgScoreMultiPoint.toFixed(2))
        }

        ReferenceText()
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun ReferenceText() {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
    )

    Text(
        text = buildAnnotatedString {
            append("数据仅供参考，真实数据请以 ")
            withLink(
                LinkAnnotation.Url(
                    url = "https://jxw.${BuildConfig.MESSAGE_API_ENDPOINT}/xsxy/xsxyqk_cxXsxyqkIndex.html?gnmkdm=N105515&layout=default",
                    styles = linkStyle
                )
            ) {
                append("教务系统")
            }
            append(" 为准")
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
