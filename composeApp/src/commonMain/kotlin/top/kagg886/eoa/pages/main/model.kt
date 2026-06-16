package top.kagg886.eoa.pages.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.delay
import kotlin.time.Instant
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSecondClassMMKV
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.*
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.pages.rootViewModel
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.sylu_eoa.api.v2.InvalidCredentialsException
import top.kagg886.sylu_eoa.api.v2.RetryLimitException
import top.kagg886.util.asTaggedLogger
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@Composable
fun mainViewModelOrNull(): MainRouteViewModel? {
    val nav = LocalNavController.current

    val state by nav.currentBackStackEntryAsState()
    val parentEntry = remember(state) {
        runCatching { nav.getBackStackEntry(MainRoute) }.getOrNull() // 嵌套图 route
    }

    if (parentEntry == null) {
        return null
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
        val lastSyncUnSuccess = syncDao.getLastSyncSuccess()?.not() ?: true

        //上次同步未成功 或 距离上次同步超过一定时间 时，开始同步
        if ((Clock.System.now() - lastSyncTime > AppSettingsMMKV.syncDuration) || lastSyncUnSuccess) {
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
                progress = MainRouteViewState.SyncProcessProgress.ProcessingUserData
            )
        }
        logger.i("开始同步")
        postSideEffect(MainRouteViewEffect.Toast(type = SnackBarType.Info, message = "开始同步"))
        val result = runCatching {
            @OptIn(OrbitExperimental::class)
            runOn<MainRouteViewState.SyncProcess> {
                with(AppLoginPropertiesMMKV.client) {
                    AppSyncMMKV.profile = getUserProfile()
                    logger.i("成功同步用户信息")

                    reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingSchoolCalendar) }
                    AppSyncMMKV.calender = getSchoolCalender()
                    logger.i("成功同步校历信息")


                    reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingExamData(-1, -1)) }
                    database.examDao().let {
                        it.clear()
                        val items = getExamList()
                        for ((i, item) in items.withIndex()) {
                            reduce {
                                state.copy(
                                    progress = MainRouteViewState.SyncProcessProgress.ProcessingExamData(
                                        i,
                                        items.size
                                    )
                                )
                            }
                            val details = getExamInfo(item)
                            it.insert(item.toEntity(details))
                        }
                    }
                    logger.i("成功同步考试信息")

                    reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingGPAData(-1, -1)) }
                    val gpa = database.gpaDao()
                    database.gpaSummaryDao().let {
                        it.clear()
                        val items = getGPAScores()
                        for ((i, item) in items.withIndex()) {
                            reduce {
                                state.copy(
                                    progress = MainRouteViewState.SyncProcessProgress.ProcessingGPAData(
                                        i,
                                        items.size
                                    )
                                )
                            }
                            val gpaSummaryId = it.insert(item.toEntity())
                            for (item in getGPAScoreList(item)) {
                                gpa.insert(item.toEntity(gpaSummaryId))
                            }
                        }
                    }
                    logger.i("成功同步GPA信息")

                    database.noticeDao().let { dao ->
                        dao.clear()

                        reduce {
                            state.copy(
                                progress = MainRouteViewState.SyncProcessProgress.ProcessingSystemNotice(
                                    true
                                )
                            )
                        }
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

                        reduce {
                            state.copy(
                                progress = MainRouteViewState.SyncProcessProgress.ProcessingSystemNotice(
                                    false
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

                    reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingTermData) }
                    val oldPicker = AppSyncMMKV.picker
                    AppSyncMMKV.picker = getAllAvailableTerms()
                    logger.i("成功同步学期信息")

                    reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingCourseData) }
                    val courseDao = database.courseDao()
                    val recordDao = database.courseRecordDao()
                    val courseExtendDao = database.courseExtendDao()
                    oldPicker?.default?.asTerm()?.run {
                        courseDao.clear(xnm, xqm)
                        courseExtendDao.clear(xnm, xqm)
                    }

                    val (science, tables) = getClassTable(AppSyncMMKV.picker!!.default)

                    courseExtendDao.insertAll(
                        science.flatMap {
                            it.rangeAllTerm.map { weekNumber ->
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
                                    isExaminable = i.classType == "考试",
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
        val ex = when(val ex = result.exceptionOrNull()!!) {
            is RetryLimitException -> ex.cause!!
            else -> ex
        }

        if (ex is InvalidCredentialsException) {
            postSideEffect(
                MainRouteViewEffect.Toast(
                    type = SnackBarType.Error,
                    message = "登录凭证已过期！请重新登录"
                )
            )
            clear0()
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
        syncDao.markSync(SyncRecordEntity(updatedStamp = lastSyncTime ?: 0, success = false))
        reduce {
            MainRouteViewState.SyncFailed(
                haveDirtyData,
                ex.message ?: "未知错误"
            )
        }
    }

    private suspend fun clear0() {
        AppLoginPropertiesMMKV.clear()
        AppSyncMMKV.clear()
        AppSecondClassMMKV.clear()

        database.examDao().clear()
        database.gpaSummaryDao().clear()
        database.gpaDao().clear()
        database.courseDao().clearAll()
        database.courseExtendDao().clearAll()
        database.examDao().clear()
        database.secondClassDao().clear()
        syncDao.clear()
    }

    fun logout() = intent {
        logger.i("开始登出")
        clear0()
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
    data class SyncProcess(val haveDirtyData: Boolean = false, val progress: SyncProcessProgress) :
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


    sealed interface SyncProcessProgress {
        /*
         * 正在同步用户信息
         */
        data object ProcessingUserData : SyncProcessProgress

        /*
         * 正在同步校历信息
         */
        data object ProcessingSchoolCalendar : SyncProcessProgress

        /*
         * 正在同步考试信息
         */
        data class ProcessingExamData(val current: Int, val all: Int) : SyncProcessProgress

        /*
         * 正在同步 GPA 信息
         */
        data class ProcessingGPAData(val current: Int, val all: Int) : SyncProcessProgress

        /*
         * 正在同步系统通知
         */
        data class ProcessingSystemNotice(val readable: Boolean) : SyncProcessProgress

        /*
         * 正在同步学期信息
         */
        data object ProcessingTermData : SyncProcessProgress

        /*
         * 正在同步课程信息
         */
        data object ProcessingCourseData : SyncProcessProgress

    }
}

sealed interface MainRouteViewEffect {
    data object NavigateToLogin : MainRouteViewEffect
    data class Toast(
        val type: SnackBarType,
        val message: String
    ) : MainRouteViewEffect
}
