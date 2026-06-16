package top.kagg886.eoa.pages.main.home.exam.statistic

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.util.toFixed

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/6 15:01
 * ================================================
 */

@Serializable
data class ExamStatisticRoute(val year: String,val term: String)

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
        ExamStatisticModel(mainModel.database,route.year,route.term)
    }

    val state by model.collectAsState()

    ExamStatisticContent(state)
}

@Composable
private fun ExamStatisticContent(state: ExamStatisticState) = when (state) {
    is ExamStatisticState.Success -> {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 添加醒目的提示信息
            CautionCard()

            // 当前学期统计
            if (state.avgScore != null && state.avgPoint != null && state.avgScoreMultiPoint != null) {
                StatisticTable(
                    title = "当前学期统计",
                    avgScore = state.avgScore,
                    avgPoint = state.avgPoint,
                    avgScoreMultiPoint = state.avgScoreMultiPoint
                )
            }

            // 总体统计
            StatisticTable(
                title = "总体统计",
                avgScore = state.allAvgScore,
                avgPoint = state.allAvgPoint,
                avgScoreMultiPoint = state.allAvgScoreMultiPoint
            )
        }
    }

    else -> {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("加载中...")
        }
    }
}

@Composable
private fun StatisticTable(
    title: String,
    avgScore: Double,
    avgPoint: Double,
    avgScoreMultiPoint: Double
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // 使用ListItem创建表格效果
            StatisticListItem(
                label = "平均学分",
                value = avgScore.toFixed(2)
            )

            StatisticListItem(
                label = "平均绩点",
                value = avgPoint.toFixed(2)
            )

            StatisticListItem(
                label = "学分绩点",
                value = avgScoreMultiPoint.toFixed(2)
            )
        }
    }
}

@Composable
private fun StatisticListItem(
    label: String,
    value: String
) {
    ListItem(
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CautionCard() {
    val url = "https://jxw.sylu.edu.cn/xsxy/xsxyqk_cxXsxyqkIndex.html?gnmkdm=N105515&layout=default"

    val annotatedText = buildAnnotatedString {
        append("该数据仅做参考，真实数据请查看 ")

        withLink(LinkAnnotation.Url(url)) {
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("教务系统")
            }
        }
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFFF9800),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color(0xFFFFF8E1).copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "警告",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
