package top.kagg886.eoa.pages.main.home.exam.export

import top.kagg886.eoa.util.BaseViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.compose.collectAsState
import top.kagg886.backend.config.AppExportMMKV
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.sylu_eoa.api.v2.bean.ExamExportOptions
import top.kagg886.sylu_eoa.api.v2.bean.ExamExportOptions.SelectColumn
import top.kagg886.sylu_eoa.api.v2.bean.TERM_ALL_PICKER
import top.kagg886.sylu_eoa.api.v2.bean.Term
import kotlin.time.Duration.Companion.seconds

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/10/16 14:28
 * ================================================
 */

class ExamExportViewModel(private val xnm: String,private val xqm: String) :
    BaseViewModel<ExamExportState, ExamExportSideEffect>(
        name = "ExamExportViewModel",
        initial = ExamExportState.Config(
            term = Term(xnm, xqm),
            format = ExamExportOptions.Format.XLS,
            columns = AppExportMMKV.columns,
            selectedColumns = AppExportMMKV.selected
        )
    ) {
    override suspend fun Syntax<ExamExportState, ExamExportSideEffect>.init() {
        viewModelScope.launch {
            container.stateFlow.collect { state ->
                AppExportMMKV.selected = state.selectedColumns
                AppExportMMKV.columns = state.columns
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun toggleColumn(column: SelectColumn) = intent {
        runOn<ExamExportState.Config> {
            val newSelected = if (column in state.selectedColumns) {
                state.selectedColumns - column
            } else {
                state.selectedColumns + column
            }
            reduce {
                state.copy(selectedColumns = newSelected)
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun setFormat(format: ExamExportOptions.Format) = intent {
        runOn<ExamExportState.Config> {
            reduce {
                state.copy(format = format)
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun selectAll() = intent {
        runOn<ExamExportState.Config> {
            reduce {
                state.copy(selectedColumns = state.columns.toSet())
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun deselectAll() = intent {
        runOn<ExamExportState.Config> {
            reduce {
                state.copy(selectedColumns = emptySet())
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun reorderColumns(fromIndex: Int, toIndex: Int) = intent {
        runOn<ExamExportState.Config> {
            val newColumns = state.columns.toMutableList()
            val item = newColumns.removeAt(fromIndex)
            newColumns.add(toIndex, item)
            reduce {
                state.copy(columns = newColumns)
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun exportExam() = intent {
        runOn<ExamExportState> {
            logger.d("start exam export: $state")
            // 切换到导出状态
            reduce {
                ExamExportState.Exporting(
                    term = state.term,
                    format = state.format,
                    columns = state.columns,
                    selectedColumns = state.selectedColumns
                )
            }

            try {
                val exportOptions = ExamExportOptions(
                    format = state.format,
                    select = state.columns.filter { it in state.selectedColumns }
                )
                val byt = AppLoginPropertiesMMKV.client.getExamExportSink(state.term, exportOptions)

                val term = AppSyncMMKV.picker?.list?.find { it.asTerm() == state.term }
                    ?: if (state.term == TERM_ALL_PICKER.asTerm()) TERM_ALL_PICKER else throw CancellationException("无法找到学年学期对应的代号")
                val pFile = FileKit.openFileSaver(
                    suggestedName = "成绩导出 - $term.xlsx",
                    defaultExtension = "xlsx",
                )

                if (pFile == null) {
                    throw CancellationException("用户取消导出")
                }

                pFile.write(byt)

                postSideEffect(ExamExportSideEffect.NavigateBack)
            } catch (e: Exception) {
                // 导出失败，显示错误并回到配置状态
                logger.w("failed to export exam.", e)
                postSideEffect(ExamExportSideEffect.ShowError(e.message ?: "导出失败"))
            } finally {
                reduce {
                    ExamExportState.Config(
                        term = state.term,
                        format = state.format,
                        columns = state.columns,
                        selectedColumns = state.selectedColumns
                    )
                }
            }
        }
    }

    fun cancel() = intent {
        postSideEffect(ExamExportSideEffect.NavigateBack)
    }

    fun reset() = intent {
        AppExportMMKV.reset()
        reduce {
            ExamExportState.Config(
                term = Term(xnm, xqm),
                format = ExamExportOptions.Format.XLS,
                columns = AppExportMMKV.columns,
                selectedColumns = AppExportMMKV.selected
            )
        }
    }
}

sealed interface ExamExportState {
    val term: Term
    val format: ExamExportOptions.Format
    val columns: List<SelectColumn>
    val selectedColumns: Set<SelectColumn>

    data class Config(
        override val term: Term,
        override val format: ExamExportOptions.Format,
        override val columns: List<SelectColumn>,
        override val selectedColumns: Set<SelectColumn>
    ) : ExamExportState

    data class Exporting(
        override val term: Term,
        override val format: ExamExportOptions.Format,
        override val columns: List<SelectColumn>,
        override val selectedColumns: Set<SelectColumn>
    ) : ExamExportState
}

sealed interface ExamExportSideEffect {
    data object NavigateBack : ExamExportSideEffect
    data class ShowError(val message: String) : ExamExportSideEffect
}
