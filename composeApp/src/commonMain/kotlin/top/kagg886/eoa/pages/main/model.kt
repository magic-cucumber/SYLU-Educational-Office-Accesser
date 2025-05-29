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
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.logger
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
    val database = databaseBuilder().build()

    override val container: Container<MainRouteViewState, MainRouteViewEffect> = container(MainRouteViewState.Empty) {
        logger.i("上次同步时间：${AppSyncMMKV.lastSync}")
        startSync().join()
    }

    fun startSync() = intent {
        if (Clock.System.now() - AppSyncMMKV.lastSync > 3.days) {
            startSyncForce()
            return@intent
        }
        reduce {
            MainRouteViewState.SyncSuccess
        }
    }

    fun startSyncForce() = intent {
        reduce {
            MainRouteViewState.SyncProcess
        }
        logger.i("开始同步")
        postSideEffect(MainRouteViewEffect.Toast(type = SnackBarType.Info, message = "开始同步"))
        val result = runCatching {
            with(AppLoginPropertiesMMKV.client) {
                AppSyncMMKV.profile = getUserProfile()
                logger.i("成功同步用户信息")
                AppSyncMMKV.picker = getAllAvailableTerms()
                logger.i("成功同步学期信息")
                AppSyncMMKV.calender = getSchoolCalender()
                logger.i("成功同步校历信息")

                database.examDao().let {
                    it.clear()
                    for (item in getExamList()) {
                        it.insert(item.toEntity())
                    }
                }
                logger.i("成功同步考试信息")

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

                logger.i("成功同步GPA信息")

                val courseDao = database.courseDao()
                val recordDao = database.courseRecordDao()
                courseDao.clear()
                for (i in getClassTable(AppSyncMMKV.picker!!.default)) {
                    val bindId = courseDao.insert(
                        item = CourseEntity(
                            name = i.name,
                            teacherName = i.teacher,
                            classroomName = i.room,
                            credits = i.score.toFloat(),
                            isDegreeRequired = i.isDegreeProgram,
                        )
                    )
                    val dayNumber = i.dayInWeek
                    i.rangeAllTerm.forEach { weekNumber ->
                        i.rangeEveryDay.forEach { lessonNumber ->
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
                logger.i("成功同步课程信息")
            }
        }

        if (result.isSuccess) {
            postSideEffect(MainRouteViewEffect.Toast(type = SnackBarType.Success, message = "同步完毕！"))
            logger.i("同步完毕！")
            AppSyncMMKV.lastSync = Clock.System.now()
            reduce {
                MainRouteViewState.SyncSuccess
            }
            return@intent
        }
        postSideEffect(MainRouteViewEffect.Toast(type = SnackBarType.Error, message = "同步失败！详情请参阅日志"))

        logger.e("同步失败！", result.exceptionOrNull())
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

sealed interface MainRouteViewEffect {
    data class Toast(
        val type: SnackBarType,
        val message: String
    ) : MainRouteViewEffect
}
