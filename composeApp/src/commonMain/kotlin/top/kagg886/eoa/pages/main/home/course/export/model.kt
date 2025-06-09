package top.kagg886.eoa.pages.main.home.course.export

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.database.AppDatabase
import top.kagg886.eoa.util.SnackBarType
import kotlin.time.Duration.Companion.seconds

class CourseExportModel(
    database: AppDatabase
) : ViewModel(), ContainerHost<CourseExportState, CourseExportSideEffect> {
    private val dao = database.courseDao()
    override val container =
        container<CourseExportState, CourseExportSideEffect>(CourseExportState("正在导出...")) {
            exportICS().join()
        }

    fun exportICS() = intent {

        delay(1.seconds)
        postSideEffect(CourseExportSideEffect.NavigateBack("导出成功"))
    }
}

data class CourseExportState(
    val message: String
)

sealed interface CourseExportSideEffect {
    data class NavigateBack(val msg: String, val type: SnackBarType = SnackBarType.Info) :
        CourseExportSideEffect
}