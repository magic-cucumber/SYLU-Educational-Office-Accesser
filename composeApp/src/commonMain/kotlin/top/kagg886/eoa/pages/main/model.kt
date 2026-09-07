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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSecondClassMMKV
import top.kagg886.backend.config.AppSettingsMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.*
import top.kagg886.eoa.LocalDatabase
import top.kagg886.eoa.LocalNavController
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

    val database = LocalDatabase.current
    return viewModel(parentEntry) {
        MainRouteViewModel(database)
    }
}


class MainRouteViewModel(val database: AppDatabase) :
    BaseViewModel<MainRouteViewState, MainRouteViewEffect>(
        name = "MainRouteViewModel",
        initial = MainRouteViewState.Empty
    ) {
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

        //准备同步了
        if (state is MainRouteViewState.SyncProcess) return

        val lastSyncTime = syncDao.getLastSyncTime() ?: Instant.DISTANT_PAST
        val lastSyncFailed = syncDao.getLastSyncSuccess()?.not() ?: true

        // 上次同步未成功或超过同步间隔时，从检查点继续。
        if ((Clock.System.now() - lastSyncTime > AppSettingsMMKV.syncDuration) || lastSyncFailed) {
            startSyncForce()
        } else {
            reduce { MainRouteViewState.SyncSuccess(lastSyncTime) }
        }
    }

    fun startSyncForce() = intent {
        val lastSyncTime = try {
            syncDao.getLastSyncTime()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e

            logger.e("数据库初始化失败", e)
            postSideEffect(
                MainRouteViewEffect.Toast(
                    SnackBarType.Error,
                    "数据库出现错误！请查看日志。\n如果您认为这个错误不应该发生，可以尝试清除所有数据后重新登录。"
                )
            )
            reduce {
                MainRouteViewState.SyncFailed(
                    haveDirtyData = false,
                    message = e.message ?: "数据库初始化失败"
                )
            }
            return@intent
        }

        reduce {
            MainRouteViewState.SyncProcess(
                haveDirtyData = lastSyncTime != null,
                progress = MainRouteViewState.SyncProcessProgress.ProcessingSchoolCalendar
            )
        }

        logger.i("开始同步")
        postSideEffect(
            MainRouteViewEffect.Toast(
                type = SnackBarType.Info,
                message = "开始同步"
            )
        )

        val overview = syncDao.getLastUnSuccessOverview() ?: run {
            val item = SyncOverviewEntity(
                updatedStamp = lastSyncTime ?: Instant.DISTANT_PAST,
                success = false
            )
            item.copy(id = syncDao.insertOverview(item).toInt())
        }

        try {
            val overviewId = checkNotNull(overview.id) { "无法生成检查点，请重试" }

            val checkpoint = syncDao.getCheckpointByOverviewId(overviewId) ?: run {
                val item = SyncCheckpointEntity(overviewId = overviewId)
                item.copy(id = syncDao.upsertCheckpoint(item).toInt())
            }

            val session = SyncSession(checkpoint)

            syncCalendar(session)
            syncTerms(session)
            syncCourses(session)
            syncProfile(session)
            syncExams(session)
            syncGpa(session)
            syncNotices(session)

            val completedAt = Clock.System.now()

            database.withWriteTransaction {
                syncDao.updateCheckpoint(
                    session.checkpoint.copy(
                        updatedStamp = completedAt,
                        examPayload = null,
                        gpaPayload = null
                    )
                )
                syncDao.updateOverview(
                    overview.copy(
                        updatedStamp = completedAt,
                        success = true
                    )
                )
            }

            reduce { MainRouteViewState.SyncSuccess(completedAt) }
            logger.i("同步完毕！")

            postSideEffect(
                MainRouteViewEffect.Toast(
                    type = SnackBarType.Success,
                    message = "同步完毕！"
                )
            )
        } catch (error: Exception) {
            val ex = if (error is RetryLimitException) error.cause ?: error else error

            if (ex is CancellationException) throw ex

            if (ex is InvalidCredentialsException) {
                postSideEffect(
                    MainRouteViewEffect.Toast(
                        type = SnackBarType.Error,
                        message = "登录凭证已过期！请重新登录"
                    )
                )
                clear0()
                delay(3.seconds)
                postSideEffect(MainRouteViewEffect.NavigateToLogin)
                return@intent
            }

            logger.e("同步失败！", ex)

            syncDao.updateOverview(
                overview.copy(
                    updatedStamp = lastSyncTime ?: Instant.DISTANT_PAST,
                    success = false
                )
            )

            reduce {
                MainRouteViewState.SyncFailed(
                    lastSyncTime != null,
                    ex.message ?: "未知错误"
                )
            }
            postSideEffect(MainRouteViewEffect.SyncErrorToast)
        }
    }

    private inner class SyncSession(initial: SyncCheckpointEntity) {

        // 固定本轮同步使用的登录客户端，避免同步过程中客户端状态发生变化。
        val client = AppLoginPropertiesMMKV.client

        // 当前已成功提交的检查点。
        // 只有数据库事务成功后才会更新，保证其始终与数据库状态一致。
        var checkpoint = initial
            private set

        /**
         * 表示一次完整的同步任务。
         *
         * 在执行同步action时会：
         * - 获取检查点通过时的待写入检查点
         * - 写入数据并更新检查点
         * - 将已写入的检查点重新作为内存值
         */
        suspend fun commit(
            update: (SyncCheckpointEntity) -> SyncCheckpointEntity,
            write: suspend () -> Unit = {}
        ) {
            val pending = update(checkpoint)
            val next = database.withWriteTransaction {
                write()

                val next = pending.copy(updatedStamp = Clock.System.now())
                syncDao.updateCheckpoint(next)
                next
            }

            checkpoint = next
        }
    }

    private suspend fun syncCalendar(session: SyncSession) = subIntent {
        runOn<MainRouteViewState.SyncProcess> {
            if (session.checkpoint.calendarSuccess) return@runOn
            reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingSchoolCalendar) }
            // MMKV 与 Room 无法共用事务：先写配置再标记，中断后允许重试本步骤。
            AppSyncMMKV.calender = session.client.getSchoolCalender()
            session.commit({ it.copy(calendarSuccess = true) })
            logger.i("成功同步校历信息")
        }
    }

    private suspend fun syncTerms(session: SyncSession) = subIntent {
        runOn<MainRouteViewState.SyncProcess> {
            if (session.checkpoint.termSuccess) return@runOn
            reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingTermData) }
            AppSyncMMKV.picker = session.client.getAllAvailableTerms()
            session.commit({ it.copy(termSuccess = true) })
            logger.i("成功同步学期信息")
        }
    }

    private suspend fun syncCourses(session: SyncSession) = subIntent {
        runOn<MainRouteViewState.SyncProcess> {
            if (session.checkpoint.courseSuccess) return@runOn
            reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingCourseData) }

            val picker = checkNotNull(AppSyncMMKV.picker) { "缺少学期信息" }
            val calendar = checkNotNull(AppSyncMMKV.calender) { "缺少校历信息" }
            val term = picker.default.asTerm()

            // 网络请求失败时不清理旧课程；全部写入与完成标记一起提交。
            val (science, tables) = session.client.getClassTable(picker.default, calendar.start)
            val courseDao = database.courseDao()
            val recordDao = database.courseRecordDao()
            val extendDao = database.courseExtendDao()

            session.commit({ it.copy(courseSuccess = true) }) {
                courseDao.clear(
                    calendar.start.atTime(0, 0),
                    calendar.end.plus(1, DateTimeUnit.DAY).atTime(0, 0)
                )
                extendDao.clearAll()
                extendDao.insertAll(science.flatMap { item ->
                    item.ranges.map { week ->
                        CourseExtendEntity(
                            name = item.name,
                            teacherName = item.teacher,
                            weekNumber = week,
                            yearCode = term.xnm,
                            semesterCode = term.xqm
                        )
                    }
                })
                for (records in tables.groupBy { it.id }.values) {
                    val item = records.first()
                    val courseId = courseDao.insert(
                        CourseEntity(
                            name = item.name,
                            teacherName = item.teacher,
                            classroomName = item.room,
                            credits = item.score.toFloat(),
                            isDegreeRequired = item.isDegreeProgram,
                            isExaminable = item.classType == "考试"
                        )
                    )
                    recordDao.insertAll(records.map {
                        CourseRecordEntity(
                            courseId = courseId,
                            startTime = it.startTime,
                            endTime = it.endTime
                        )
                    })
                }
            }
            logger.i("成功同步课程信息")
        }
    }

    private suspend fun syncProfile(session: SyncSession) = subIntent {
        runOn<MainRouteViewState.SyncProcess> {
            if (session.checkpoint.profileSuccess) return@runOn
            reduce { state.copy(progress = MainRouteViewState.SyncProcessProgress.ProcessingUserData) }
            AppSyncMMKV.profile = session.client.getUserProfile()
            session.commit({ it.copy(profileSuccess = true) })
            logger.i("成功同步用户信息")
        }
    }

    private suspend fun syncExams(session: SyncSession) = subIntent {
        runOn<MainRouteViewState.SyncProcess> {
            if (session.checkpoint.examSuccess) return@runOn

            reduce {
                state.copy(
                    progress = MainRouteViewState.SyncProcessProgress.ProcessingExamData(-1, -1)
                )
            }

            val dao = database.examDao()

            // 1. 初始化考试同步任务。
            if (session.checkpoint.examPayload == null) {
                val items = session.client.getExamList()

                session.commit(
                    update = {
                        it.copy(examPayload = ExamSyncPayload(items))
                    },
                    write = dao::clear
                )
            }

            // 2. 逐条同步考试详情，每条都是独立的可恢复检查点。
            while (session.checkpoint.examPayload?.remains?.isNotEmpty() == true) {
                val payload = checkNotNull(session.checkpoint.examPayload)
                val item = payload.remains.first()

                reduce {
                    state.copy(
                        progress = MainRouteViewState.SyncProcessProgress.ProcessingExamData(
                            payload.total - payload.remains.size,
                            payload.total
                        )
                    )
                }

                val details = session.client.getExamInfo(item)

                session.commit(
                    update = {
                        it.copy(
                            examPayload = payload.copy(
                                remains = payload.remains.drop(1)
                            )
                        )
                    },
                    write = {
                        dao.insert(item.toEntity(details))
                    }
                )
            }

            // 3. 全部完成，清理恢复数据并标记成功。
            session.commit({
                it.copy(
                    examSuccess = true,
                    examPayload = null
                )
            })

            logger.i("成功同步考试信息")
        }
    }

    private suspend fun syncGpa(session: SyncSession) = subIntent {
        runOn<MainRouteViewState.SyncProcess> {
            if (session.checkpoint.gpaSuccess) return@runOn

            reduce {
                state.copy(
                    progress = MainRouteViewState.SyncProcessProgress.ProcessingGPAData(-1, -1)
                )
            }

            val dao = database.gpaDao()
            val summaryDao = database.gpaSummaryDao()

            // 1. 初始化 GPA 同步任务。
            if (session.checkpoint.gpaPayload == null) {
                val items = session.client.getGPAScores()

                session.commit(
                    update = {
                        it.copy(gpaPayload = GPASyncPayload(items))
                    },
                    write = {
                        dao.clear()
                        summaryDao.clear()
                    }
                )
            }

            // 2. 逐组同步 GPA 汇总及明细。
            while (session.checkpoint.gpaPayload?.remains?.isNotEmpty() == true) {
                val payload = checkNotNull(session.checkpoint.gpaPayload)
                val item = payload.remains.first()

                reduce {
                    state.copy(
                        progress = MainRouteViewState.SyncProcessProgress.ProcessingGPAData(
                            payload.total - payload.remains.size,
                            payload.total
                        )
                    )
                }

                val details = session.client.getGPAScoreList(item)

                // 汇总、明细以及 payload 推进作为一个可恢复子步骤原子提交。
                session.commit(
                    update = {
                        it.copy(
                            gpaPayload = payload.copy(
                                remains = payload.remains.drop(1)
                            )
                        )
                    },
                    write = {
                        val summaryId = summaryDao.insert(item.toEntity())
                        details.forEach {
                            dao.insert(it.toEntity(summaryId))
                        }
                    }
                )
            }

            // 3. 全部完成，清理恢复数据并标记成功。
            session.commit({
                it.copy(
                    gpaSuccess = true,
                    gpaPayload = null
                )
            })

            logger.i("成功同步GPA信息")
        }
    }

    private suspend fun syncNotices(session: SyncSession) = subIntent {
        runOn<MainRouteViewState.SyncProcess> {
            if (session.checkpoint.noticeSuccess) return@runOn
            reduce {
                state.copy(
                    progress = MainRouteViewState.SyncProcessProgress.ProcessingSystemNotice(
                        true
                    )
                )
            }
            val read = session.client.getNotice(true)
            reduce {
                state.copy(
                    progress = MainRouteViewState.SyncProcessProgress.ProcessingSystemNotice(
                        false
                    )
                )
            }
            val unread = session.client.getNotice(false)
            val dao = database.noticeDao()
            session.commit({ it.copy(noticeSuccess = true) }) {
                dao.clear()
                for ((items, isRead) in listOf(read to true, unread to false)) {
                    for (item in items) {
                        dao.insert(
                            SystemNoticeEntity(
                                id = item.id,
                                title = item.title,
                                content = item.content,
                                time = item.createTime,
                                isRead = isRead
                            )
                        )
                    }
                }
            }
            logger.i("成功同步系统通知")
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
