package top.kagg886.eoa.pages.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseExtendEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.backend.database.dao.SystemNoticeEntity
import top.kagg886.backend.database.dao.toEntity
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.sylu_eoa.api.v2.InvalidCredentialsException
import top.kagg886.util.asTaggedLogger
import kotlin.time.Duration.Companion.seconds

@Composable
fun mainViewModel(): MainRouteViewModel {
    val nav = LocalNavController.current
    val parentEntry = remember {
        nav.getBackStackEntry(MainRoute) // 嵌套图 route
    }
    val rootModel = rootViewModel()
    return viewModel(parentEntry) {
        MainRouteViewModel(rootModel.database)
    }
}


class MainRouteViewModel(val database: AppDatabase) : ViewModel(),
    ContainerHost<MainRouteViewState, MainRouteViewEffect> {
    private val syncDao = database.syncRecordDao()
    private val logger = "MainRouteViewModel".asTaggedLogger

    override val container: Container<MainRouteViewState, MainRouteViewEffect> =
        container(MainRouteViewState.Empty) {
            val time = try {
                syncDao.getLastSyncTime()
            } catch (e: Exception) {
                logger.w("获取同步时间出错：", e)
                reduce {
                    MainRouteViewState.SyncFailed(false, "数据库损坏，请删除数据库后重试")
                }
                return@container
            }
            logger.i("上次同步时间：${time}")
            startSync().join()
        }

    fun startSync() = intent {
        if (state is MainRouteViewState.SyncProcess) {
            return@intent
        }
        val lastSyncTime = Instant.fromEpochMilliseconds(syncDao.getLastSyncTime() ?: 0)
        if (Clock.System.now() - lastSyncTime > AppSettingsMMKV.syncDuration) {
            startSyncForce()
            return@intent
        }
        reduce {
            MainRouteViewState.SyncSuccess(lastSyncTime)
        }
    }

    fun startSyncForce() = intent {
        val lastSyncTime = syncDao.getLastSyncTime()
        val haveDirtyData = lastSyncTime != null
        reduce {
            MainRouteViewState.SyncProcess(
                haveDirtyData = haveDirtyData,
            )
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
                        val details = getExamInfo(item)
                        it.insert(item.toEntity(details))
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

                database.noticeDao().let { dao ->
                    dao.clear()
                    getNotice(true).forEach {
                        dao.insert(
                            SystemNoticeEntity(
                                id = it.id,
                                title = it.title,
                                content = it.content,
                                time = it.createTime,
                                isRead = true
                            )
                        )
                    }

                    getNotice(false).forEach {
                        dao.insert(
                            SystemNoticeEntity(
                                id = it.id,
                                title = it.title,
                                content = it.content,
                                time = it.createTime,
                                isRead = false
                            )
                        )
                    }
                }
                logger.i("成功同步系统通知")

                val courseDao = database.courseDao()
                val recordDao = database.courseRecordDao()
                val courseExtendDao = database.courseExtendDao()

                with(AppSyncMMKV.picker!!.default.asTerm()) {
                    courseDao.clear(xnm, xqm)
                }

                val (science, tables) = getClassTable(AppSyncMMKV.picker!!.default)

                courseExtendDao.insertAll(
                    science.flatMap {
                        it.rangeAllTerm.map { weekNumber->
                            with(AppSyncMMKV.picker!!.default.asTerm()) {
                                CourseExtendEntity(
                                    name = it.name,
                                    teacherName = it.teacher,
                                    weekNumber = weekNumber,
                                    yearCode = xnm,
                                    semesterCode = xqm,
                                )
                            }
                        }
                    }
                )

                for (i in tables) {
                    val bindId = courseDao.insert(
                        item = with(AppSyncMMKV.picker!!.default.asTerm()) {
                            CourseEntity(
                                name = i.name,
                                teacherName = i.teacher,
                                classroomName = i.room,
                                credits = i.score.toFloat(),
                                isDegreeRequired = i.isDegreeProgram,
                                yearCode = xnm,
                                semesterCode = xqm,
                            )
                        }
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
            postSideEffect(
                MainRouteViewEffect.Toast(
                    type = SnackBarType.Success,
                    message = "同步完毕！"
                )
            )
            logger.i("同步完毕！")
            syncDao.markSync()
            reduce {
                MainRouteViewState.SyncSuccess(Clock.System.now())
            }
            return@intent
        }
        val ex = result.exceptionOrNull()

        if (ex is InvalidCredentialsException) {
            postSideEffect(
                MainRouteViewEffect.Toast(
                    type = SnackBarType.Error,
                    message = "登录凭证已过期！请重新登录"
                )
            )
            clear()
            delay(3.seconds)
            postSideEffect(
                MainRouteViewEffect.NavigateToLogin
            )
            return@intent
        }
        postSideEffect(
            MainRouteViewEffect.Toast(
                type = SnackBarType.Error,
                message = "同步失败！详情请参阅日志"
            )
        )
        logger.e("同步失败！", ex)
        reduce {
            MainRouteViewState.SyncFailed(
                haveDirtyData,
                result.exceptionOrNull()!!.message ?: "未知错误"
            )
        }
    }

    private suspend fun clear() {
        AppLoginPropertiesMMKV.clear()
        AppSyncMMKV.clear()
        database.examDao().clear()
        database.gpaSummaryDao().clear()
        database.gpaDao().clear()
        database.courseDao().clearAll()
        database.examDao().clear()
        syncDao.clear()
    }

    fun logout() = intent {
        logger.i("开始登出")
        clear()
        postSideEffect(
            MainRouteViewEffect.Toast(
                type = SnackBarType.Success,
                message = "登出成功！"
            )
        )
        delay(3.seconds)
        postSideEffect(
            MainRouteViewEffect.NavigateToLogin
        )
    }

    fun toast(type: SnackBarType, message: String) = intent {
        postSideEffect(MainRouteViewEffect.Toast(type, message))
    }
}


sealed interface MainRouteViewState {
    /**
     * 初始状态
     */
    data object Empty : MainRouteViewState

    /**
     * 正在同步
     * @param haveDirtyData 是否在之前同步过
     */
    data class SyncProcess(val haveDirtyData: Boolean = false) :
        MainRouteViewState

    /**
     * 同步成功
     */
    data class SyncSuccess(val lastUpdateTime: Instant) : MainRouteViewState

    /**
     * 同步失败
     * @param haveDirtyData 是否在之前同步过
     * @param message 失败信息
     */
    data class SyncFailed(val haveDirtyData: Boolean = false, val message: String) :
        MainRouteViewState
}

sealed interface MainRouteViewEffect {
    data object NavigateToLogin : MainRouteViewEffect
    data class Toast(
        val type: SnackBarType,
        val message: String
    ) : MainRouteViewEffect
}
