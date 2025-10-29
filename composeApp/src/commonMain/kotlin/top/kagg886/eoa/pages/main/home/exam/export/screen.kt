package top.kagg886.eoa.pages.main.home.exam.export

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamedrejeb.compose.dnd.reorder.ReorderContainer
import com.mohamedrejeb.compose.dnd.reorder.ReorderableItem
import com.mohamedrejeb.compose.dnd.reorder.rememberReorderState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.eoa.util.showSnackBar
import top.kagg886.sylu_eoa.api.v2.bean.ExamExportOptions
import top.kagg886.sylu_eoa.api.v2.bean.ExamExportOptions.SelectColumn

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/10/16 14:28
 * ================================================
 */

@Serializable
data class ExamExportRoute(val xnm: String, val xqm: String)

@Composable
fun ExamExportScreen(params: ExamExportRoute) {
    val model: ExamExportViewModel = viewModel { ExamExportViewModel(params.xnm, params.xqm) }
    val state by model.collectAsState()
    val nav = LocalNavController.current
    val snackbar = LocalSnackBarHost.current

    model.collectSideEffect { effect ->
        when (effect) {
            is ExamExportSideEffect.NavigateBack -> nav.popBackStack()
            is ExamExportSideEffect.ShowError -> snackbar.showSnackBar(type = SnackBarType.Error,"导出失败！",effect.message)
        }
    }

    ConfigContent(
        state = state,
        isExporting = state is ExamExportState.Exporting,
        onFormatChange = model::setFormat,
        onToggleColumn = model::toggleColumn,
        onSelectAll = model::selectAll,
        onDeselectAll = model::deselectAll,
        onReorder = model::reorderColumns,
        onExport = model::exportExam,
        onCancel = model::cancel,
        onReset = model::reset
    )
}

@Composable
private fun ConfigContent(
    state: ExamExportState,
    isExporting: Boolean,
    onFormatChange: (ExamExportOptions.Format) -> Unit,
    onToggleColumn: (SelectColumn) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val reorderState = rememberReorderState<SelectColumn>()
    val lazyListState = rememberLazyListState()

    DialogPageScaffold(
        title = { Text("导出考试成绩", fontWeight = FontWeight.Bold) },
        confirmButton = {
            TextButton(
                onClick = onExport,
                enabled = state.selectedColumns.isNotEmpty() && !isExporting,
            ) {
                AnimatedContent(
                    targetState = isExporting,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    }
                ) { exporting ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (exporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text("导出中...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("导出")
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("取消")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
            ) {
                // 导出格式选择
                Text(
                    text = "导出格式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExamExportOptions.Format.entries.forEach { format ->
                        FilterChip(
                            enabled = !isExporting && format == ExamExportOptions.Format.XLS, //TODO 暂时只支持xls
                            selected = state.format == format,
                            onClick = { onFormatChange(format) },
                            label = { Text(format.name) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 列选择区域标题和操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择导出字段 (${state.selectedColumns.size}/${state.columns.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onSelectAll, enabled = !isExporting) {
                            Text("全选")
                        }
                        TextButton(onClick = onDeselectAll, enabled = !isExporting) {
                            Text("全不选")
                        }
                        TextButton(onClick = onReset, enabled = !isExporting) {
                            Text("还原")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 可拖拽排序的列表
                ReorderContainer(
                    state = reorderState,
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.columns, key = { it.hashCode() }) { column ->
                            ReorderableItem(
                                enabled = !isExporting,
                                state = reorderState,
                                key = column.hashCode(),
                                data = column,
                                onDrop = { fromState ->
                                    val fromIndex = state.columns.indexOf(fromState.data)
                                    val toIndex = state.columns.indexOf(column)
                                    if (fromIndex != -1 && toIndex != -1) {
                                        onReorder(fromIndex, toIndex)
                                    }
                                },
                                onDragEnter = { dragState ->
                                    val fromIndex = state.columns.indexOf(dragState.data)
                                    val toIndex = state.columns.indexOf(column)
                                    if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                        onReorder(fromIndex, toIndex)
                                        scope.launch {
                                            handleLazyListScroll(
                                                lazyListState = lazyListState,
                                                dropIndex = toIndex
                                            )
                                        }
                                    }
                                },
                                draggableContent = {
                                    ColumnItem(
                                        column = column,
                                        enabled = !isExporting,
                                        isSelected = column in state.selectedColumns,
                                        onToggle = { onToggleColumn(column) },
                                        isDragShadow = true
                                    )
                                }
                            ) {
                                ColumnItem(
                                    column = column,
                                    enabled = !isExporting,
                                    isSelected = column in state.selectedColumns,
                                    onToggle = { onToggleColumn(column) },
                                    modifier = Modifier.graphicsLayer {
                                        alpha = if (isDragging) 0f else 1f
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ColumnItem(
    column: SelectColumn,
    enabled: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    isDragShadow: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isDragShadow) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (isDragShadow) 4.dp else 1.dp
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = column.remark,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            },
            supportingContent = {
                Text(text = column.getFieldName())
            },
            leadingContent = {
                Checkbox(
                    checked = isSelected,
                    enabled = enabled,
                    onCheckedChange = { onToggle() }
                )
            }
        )
    }
}

// 扩展函数：获取字段名用于UI显示
private fun SelectColumn.getFieldName(): String = this::class.simpleName!!.lowercase()

suspend fun handleLazyListScroll(
    lazyListState: LazyListState,
    dropIndex: Int,
): Unit = coroutineScope {
    val firstVisibleItemIndex = lazyListState.firstVisibleItemIndex
    val firstVisibleItemScrollOffset = lazyListState.firstVisibleItemScrollOffset

    // Workaround to fix scroll issue when dragging the first item
    if (dropIndex == 0 || dropIndex == 1) {
        launch {
            lazyListState.scrollToItem(firstVisibleItemIndex, firstVisibleItemScrollOffset)
        }
    }

    // Animate scroll when entering the first or last item
    val lastVisibleItemIndex =
        lazyListState.firstVisibleItemIndex + lazyListState.layoutInfo.visibleItemsInfo.lastIndex

    val firstVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@coroutineScope
    val scrollAmount = firstVisibleItem.size * 2f

    if (dropIndex <= firstVisibleItemIndex + 1) {
        launch {
            lazyListState.animateScrollBy(-scrollAmount)
        }
    } else if (dropIndex == lastVisibleItemIndex) {
        launch {
            lazyListState.animateScrollBy(scrollAmount)
        }
    }
}
