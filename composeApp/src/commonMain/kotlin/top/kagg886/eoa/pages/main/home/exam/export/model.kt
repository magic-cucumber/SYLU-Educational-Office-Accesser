package top.kagg886.eoa.pages.main.home.exam.export

import androidx.lifecycle.ViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.sylu_eoa.api.v2.bean.ExamExportOptions
import top.kagg886.sylu_eoa.api.v2.bean.ExamExportOptions.SelectColumn
import top.kagg886.sylu_eoa.api.v2.bean.TERM_ALL_PICKER
import top.kagg886.sylu_eoa.api.v2.bean.Term
import top.kagg886.util.asTaggedLogger
import kotlin.time.Duration.Companion.seconds

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/10/16 14:28
 * ================================================
 */

private val ALL_SELECT_COLUMNS = listOf(
    // 核心身份信息
    SelectColumn.XH(),        // 学号
    SelectColumn.XM(),        // 姓名

    // 核心成绩信息
    SelectColumn.KCMC(),      // 课程名称
    SelectColumn.CJ(),        // 成绩
    SelectColumn.XF(),        // 学分
    SelectColumn.JD(),        // 绩点
    SelectColumn.XFJD(),      // 学分绩点

    // 时间定位信息
    SelectColumn.XNMMC(),     // 学年名称
    SelectColumn.XQMMC(),     // 学期名称

    // 课程基本信息
    SelectColumn.KCH(),       // 课程号
    SelectColumn.KCXZMC(),    // 课程性质名称
    SelectColumn.KCLBMC(),    // 课程类别名称
    SelectColumn.KCGSMC(),    // 课程归属名称

    // 教学组织信息
    SelectColumn.JXBMC(),     // 教学班名称
    SelectColumn.JSXM(),      // 教师姓名
    SelectColumn.XSBJMC(),    // 学生班级名称

    // 辅助说明信息
    SelectColumn.KSXZ(),      // 考试性质
    SelectColumn.KHFMC(),     // 考核方式名称
    SelectColumn.CJBZ(),      // 成绩备注
    SelectColumn.CJSFZF(),    // 成绩是否作废
    SelectColumn.SFXWKC(),    // 是否学位课程
    SelectColumn.KKBBMC(),    // 开课班别名称
    SelectColumn.KCBJ(),      // 课程备注
)

class ExamExportViewModel(xnm: String, xqm: String) : ViewModel(),
    ContainerHost<ExamExportState, ExamExportSideEffect> {
    private val logger = "ExamExportViewModel".asTaggedLogger

    override val container: Container<ExamExportState, ExamExportSideEffect> =
        container(
            ExamExportState.Config(
                term = Term(xnm, xqm),
                format = ExamExportOptions.Format.XLS,
                columns = ALL_SELECT_COLUMNS,
                selectedColumns = ALL_SELECT_COLUMNS.take(9).toSet()
            )
        )

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
