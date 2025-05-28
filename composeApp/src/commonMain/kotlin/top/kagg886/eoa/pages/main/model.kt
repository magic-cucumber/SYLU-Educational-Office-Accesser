package top.kagg886.eoa.pages.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.Clock
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.backend.database.dao.toEntity
import top.kagg886.backend.database.databaseBuilder
import top.kagg886.eoa.LocalNavController
import kotlin.time.Duration.Companion.days

@Composable
fun mainViewModel(): MainRouteViewModel {
    val nav = LocalNavController.current
    val parentEntry = remember {
        nav.getBackStackEntry(MainRoute) // 嵌套图 route
    }
    return viewModel(parentEntry) {
        MainRouteViewModel()
    }
}


class MainRouteViewModel : ViewModel(), ContainerHost<MainRouteViewState, MainRouteViewEffect> {
    private val database = databaseBuilder().build()


    override val container: Container<MainRouteViewState, MainRouteViewEffect> = container(MainRouteViewState.Empty) {
        if (Clock.System.now() - AppSyncMMKV.lastSync > 1.days) {
            startSync()
            return@container
        }
        reduce {
            MainRouteViewState.SyncSuccess
        }
    }

    fun startSync() = intent {
        reduce {
            MainRouteViewState.SyncProcess
        }

        val result = runCatching {
            with(AppLoginPropertiesMMKV.client) {
                AppSyncMMKV.profile = getUserProfile()
                AppSyncMMKV.picker = getAllAvailableTerms()
                AppSyncMMKV.calender = getSchoolCalender()

                database.examDao().let {
                    it.clear()
                    for (item in getExamList()) {
                        it.insert(item.toEntity())
                    }
                }

                val gpa = database.gpaDao()
                database.gpaSummaryDao().let {
                    it.clear()
                    for (item in getGPAScores()) {
                        val gpaSummaryId = it.insert(item.toEntity())

                        for (item in getGPAScoreList(item)) {
                            gpa.insert(item.toEntity(gpaSummaryId))
                        }
                    }
                }

                val courseDao = database.courseDao()
                val recordDao = database.courseRecordDao()
                courseDao.clear()
                for (i in getClassTable(AppSyncMMKV.picker!!.default)) {
                    val bindId = courseDao.insert(
                        item = CourseEntity(
                            teacherName = i.teacher,
                            classroomName = i.room,
                            credits = i.score.toFloat(),
                            isDegreeRequired = i.isDegreeProgram,
                        )
                    )
                    val dayNumber = i.dayInWeek
                    i.rangeAllTerm.forEach { weekNumber->
                        i.rangeEveryDay.forEach { lessonNumber->
                            recordDao.insert(
                                CourseRecordEntity(
                                    courseId = bindId,
                                    weekNumber = weekNumber,
                                    dayOfWeek = dayNumber.toInt(),
                                    periodOfDay = lessonNumber
                                )
                            )
                        }
                    }
                }

            }
        }

        if (result.isSuccess) {
            reduce {
                MainRouteViewState.SyncSuccess
            }
            return@intent
        }
        reduce {
            MainRouteViewState.SyncFailed(result.exceptionOrNull()!!.message ?: "未知错误")
        }
    }
}


sealed interface MainRouteViewState {
    data object Empty : MainRouteViewState
    data object SyncProcess : MainRouteViewState
    data object SyncSuccess : MainRouteViewState
    data class SyncFailed(val message: String) : MainRouteViewState
}

sealed interface MainRouteViewEffect
