package top.kagg886.eoa.pages.main

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import top.kagg886.eoa.util.BaseViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.room3.withWriteTransaction
import com.dokar.sonner.TextToastAction
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.annotation.OrbitExperimental
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
import top.kagg886.util.asKtorLogger
import top.kagg886.util.http.HttpClient
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

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
    return viewModel(parentEntry, key = rootModel.toString()) {
        MainRouteViewModel(rootModel.database)
    }
}


class MainRouteViewModel(val database: AppDatabase) : BaseViewModel<MainRouteViewState, MainRouteViewEffect>(name = "MainRouteViewModel", initial = MainRouteViewState.Empty) {
    private val syncDao = database.syncRecordDao()
    private val llmProviderDao = database.llmProviderDao()

    val llmExecutors: StateFlow<Map<LLMProviderEntity, MultiLLMPromptExecutor>> =
        llmProviderDao.allFlow()
            .map { providers ->
                providers.associateWith {
                    MultiLLMPromptExecutor(
                        OpenAILLMClient(
                            apiKey = it.modelKey,
                            settings = OpenAIClientSettings(baseUrl = it.baseUrl),
                            httpClientFactory = KtorKoogHttpClient.Factory(
                                baseClient = HttpClient {
                                    install(Logging) {
                                        logger = this@MainRouteViewModel.logger.asKtorLogger
                                        level = LogLevel.ALL
                                    }
                                }
                            )
                        )
                    )
                }
            }
            //previous, current
            .runningFold(emptyMap<LLMProviderEntity, MultiLLMPromptExecutor>() to emptyMap<LLMProviderEntity, MultiLLMPromptExecutor>()) { acc, value ->
                acc.second.forEach { it.value.close(); }
                acc.second to value
            }
            //take current
            .map { it.second }
            .onEach { it.onEach { (_, v) -> addCloseable(v) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    override suspend fun Syntax<MainRouteViewState, MainRouteViewEffect>.init() {
            val time = try {
                syncDao.getLastSyncTime()
            } catch (e: Exception) {
                logger.w("获取同步时间出错：", e)
                reduce {
                    MainRouteViewState.SyncFailed(false, "数据库损坏，请删除数据库后重试")
                }
                return
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
        var overview = syncDao.getLastUnSuccessOverview()
        if (overview == null) {
            val overviewId = syncDao.insertOverview(
                SyncOverviewEntity(
                    updatedStamp = lastSyncTime ?: 0,
                    success = false
                )
            ).toInt()
            overview = SyncOverviewEntity(
                id = overviewId,
                updatedStamp = lastSyncTime ?: 0,
                success = false
            )
        }
        val overviewId = overview.id ?: run {
            postSideEffect(MainRouteViewEffect.Toast(type = SnackBarType.Error, message = "无法生成检查点，请重试"))
            return@intent
        }
        var checkpoint = syncDao.getCheckpointByOverviewId(overviewId) ?: run {
            val checkpointId = syncDao.upsertCheckpoint(SyncCheckpointEntity(overviewId = overviewId)).toInt()
            SyncCheckpointEntity(id = checkpointId, overviewId = overviewId)
        }

        suspend fun updateCheckpoint(block: (SyncCheckpointEntity) -> SyncCheckpointEntity) {
            checkpoint = block(checkpoint).copy(updatedStamp = Clock.System.now().toEpochMilliseconds())
            syncDao.updateCheckpoint(checkpoint)
        }

        val result = runCatching {
            @OptIn(OrbitExperimental::class)
            runOn<MainRouteViewState.SyncProcess> {
                with(AppLoginPropertiesMMKV.client) {
                    // 该阶段已经完成时直接跳过，避免断点续传重复请求和重复覆盖本地缓存。
                    if (!checkpoint.profileSuccess) {
                        AppSyncMMKV.profile = getUserProfile()
                        updateCheckpoint { it.copy(profileSuccess = true) }
                        logger.i("成功同步用户信息")
                    }

                    reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingSchoolCalendar) }
                    // 校历是固定数量数据，只有请求并写入 MMKV 后才标记完成。
                    if (!checkpoint.calendarSuccess) {
                        AppSyncMMKV.calender = getSchoolCalender()
                        updateCheckpoint { it.copy(calendarSuccess = true) }
                        logger.i("成功同步校历信息")
                    }


                    reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingExamData(-1, -1)) }
                    // 考试详情数量不固定：第一次进入时把列表写入 payload，恢复时只处理 payload 中剩余条目。
                    if (!checkpoint.examSuccess) {
                        val examDao = database.examDao()
                        if (checkpoint.examPayload == null) {
                            val items = getExamList()
                            database.withWriteTransaction {
                                examDao.clear()
                                updateCheckpoint { it.copy(examPayload = ExamSyncPayload(items)) }
                            }
                        }
                        while (!checkpoint.examPayload!!.remains.isEmpty()) {
                            val payload = checkpoint.examPayload!!
                            val items = payload.remains
                            val item = items.first()
                            reduce {
                                state.copy(
                                    progress = MainRouteViewState.SyncProcessProgress.ProcessingExamData(
                                        payload.total - items.size,
                                        payload.total
                                    )
                                )
                            }
                            val details = getExamInfo(item)
                            // 落库和 payload 移除必须在同一个事务内，保证崩溃后不会重复插入或漏插。
                            database.withWriteTransaction {
                                examDao.insert(item.toEntity(details))
                                updateCheckpoint {
                                    it.copy(examPayload = payload.copy(remains = items.drop(1)))
                                }
                            }
                        }
                        updateCheckpoint { it.copy(examSuccess = true, examPayload = null) }
                        logger.i("成功同步考试信息")
                    } else {
                        logger.i("考试信息已同步，跳过")
                    }

                    reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingGPAData(-1, -1)) }
                    // GPA 详情数量不固定：payload 保存剩余 summary，每个 summary 详情落库后再从 payload 删除。
                    if (!checkpoint.gpaSuccess) {
                        val gpa = database.gpaDao()
                        val gpaSummary = database.gpaSummaryDao()
                        if (checkpoint.gpaPayload == null) {
                            val items = getGPAScores()
                            database.withWriteTransaction {
                                gpa.clear()
                                gpaSummary.clear()
                                updateCheckpoint { it.copy(gpaPayload = GPASyncPayload(items)) }
                            }
                        }
                        while (!checkpoint.gpaPayload!!.remains.isEmpty()) {
                            val payload = checkpoint.gpaPayload!!
                            val items = payload.remains
                            val item = items.first()
                            reduce {
                                state.copy(
                                    progress = MainRouteViewState.SyncProcessProgress.ProcessingGPAData(
                                        payload.total - items.size,
                                        payload.total
                                    )
                                )
                            }
                            val details = getGPAScoreList(item)
                            // summary、score 和 payload 移除同事务提交，保证恢复时不会出现孤儿或重复数据。
                            database.withWriteTransaction {
                                val gpaSummaryId = gpaSummary.insert(item.toEntity())
                                for (detail in details) {
                                    gpa.insert(detail.toEntity(gpaSummaryId))
                                }
                                updateCheckpoint {
                                    it.copy(gpaPayload = payload.copy(remains = items.drop(1)))
                                }
                            }
                        }
                        updateCheckpoint { it.copy(gpaSuccess = true, gpaPayload = null) }
                        logger.i("成功同步GPA信息")
                    } else {
                        logger.i("GPA信息已同步，跳过")
                    }

                    // 通知可整体重建：未完成时清表重拉，完成后续传直接跳过。
                    if (!checkpoint.noticeSuccess) {
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
                        updateCheckpoint { it.copy(noticeSuccess = true) }
                        logger.i("成功同步系统通知")
                    } else {
                        logger.i("系统通知已同步，跳过")
                    }

                    reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingTermData) }
                    var oldPicker = AppSyncMMKV.picker
                    // 学期数据会被课程同步依赖；只有写入 picker 后才允许后续课程阶段运行。
                    if (!checkpoint.termSuccess) {
                        oldPicker = AppSyncMMKV.picker
                        AppSyncMMKV.picker = getAllAvailableTerms()
                        updateCheckpoint { it.copy(termSuccess = true) }
                        logger.i("成功同步学期信息")
                    } else {
                        logger.i("学期信息已同步，跳过")
                    }

                    reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingCourseData) }
                    // 课程可按当前默认学期整体重建；完成标记前失败，下次会重新清理并重拉。
                    if (!checkpoint.courseSuccess) {
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
                        updateCheckpoint { it.copy(courseSuccess = true) }
                        logger.i("成功同步课程信息")
                    } else {
                        logger.i("课程信息已同步，跳过")
                    }
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
            val completed = overview.copy(
                updatedStamp = Clock.System.now().toEpochMilliseconds(),
                success = true
            )
            syncDao.updateOverview(completed)
            syncDao.updateCheckpoint(
                checkpoint.copy(
                    updatedStamp = completed.updatedStamp,
                    examPayload = null,
                    gpaPayload = null
                )
            )
            reduce {
                MainRouteViewState.SyncSuccess(Clock.System.now())
            }
            return@intent
        }
        val ex = when (val ex = result.exceptionOrNull()!!) {
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
            MainRouteViewEffect.SyncErrorToast
        )
        logger.e("同步失败！", ex)
        syncDao.updateOverview(overview.copy(updatedStamp = lastSyncTime ?: 0, success = false))
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

    fun MainRouteViewState.toViewModelKey() = when (this) {
        Empty -> toString()
        else -> this::class.toString()
    }
}

sealed interface MainRouteViewEffect {
    data object NavigateToLogin : MainRouteViewEffect
    data class Toast(
        val type: SnackBarType,
        val message: String,
        val action: TextToastAction? = null,
    ) : MainRouteViewEffect

    data object SyncErrorToast : MainRouteViewEffect
}
