package top.kagg886.eoa.pages.main.home.gym

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room3.withReadTransaction
import androidx.room3.withWriteTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppGymMMKV
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.toEntity
import top.kagg886.backend.database.dao.toItem
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.gym.GymClient
import top.kagg886.eoa.gym.GymDetail
import top.kagg886.eoa.gym.GymDetailHistory
import top.kagg886.eoa.gym.GymSummary
import top.kagg886.eoa.pages.main.MainRoute
import top.kagg886.eoa.pages.main.mainViewModelOrNull
import top.kagg886.eoa.util.BaseViewModel
import top.kagg886.eoa.util.SnackBarType
import kotlin.time.Duration.Companion.seconds

@Composable
fun gymViewModel(): GymModel? {
    val mainModel = mainViewModelOrNull() ?: return null
    val nav = LocalNavController.current
    return viewModel(nav.getBackStackEntry(MainRoute)) { GymModel(mainModel.database) }
}

class GymModel(
    private val database: AppDatabase,
) : BaseViewModel<GymState, GymSideEffect>(name = "GymModel", initial = GymState.Initial()) {
    private val gymDao = database.gymDao()
    private val summaryDao = database.gymSummaryDao()
    private val detailDao = database.gymDetailDao()
    private val mutex = Mutex()

    override suspend fun Syntax<GymState, GymSideEffect>.init() {
        if (AppGymMMKV.password.isNotBlank()) {
            val cache = database.withReadTransaction {
                val details = detailDao.all().groupBy { it.summaryId }
                val value = summaryDao.all().associate { summary ->
                    summary.toItem() to details[summary.id].orEmpty().map { it.toItem() }
                }
                val histories = gymDao.all()
                    .groupBy { GymState.QueryKey(it.summaryId, it.detailId) }
                    .mapValues { (_, items) -> items.map { it.toItem() } }
                GymState.Success(value = value, histories = histories)
            }
            if (cache.value.isNotEmpty()) {
                reduce { cache }
            }
        }
        login().join()
    }

    fun login(password: String = AppGymMMKV.password.ifBlank { DEFAULT_PASSWORD }) = intent {
        if (!mutex.tryLock()) return@intent
        try {
            refreshData(password)
        } finally {
            mutex.unlock()
        }
    }

    fun refresh() = login()

    @OptIn(OrbitExperimental::class)
    fun navigateToFilter() = intent {
        runOn<GymState.Success> {
            postSideEffect(GymSideEffect.NavigateToFilter)
        }
    }

    @OptIn(OrbitExperimental::class)
    fun filterSummary(index: Int) = intent {
        runOn<GymState.Success> {
            if (index !in state.value.keys.indices || index == state.currentSummaryIndex) return@runOn
            reduce { state.copy(currentSummaryIndex = index, lazyListState = LazyListState()) }
        }
    }

    fun retryLogin() = intent {
        if (!mutex.tryLock()) return@intent
        try {
            if (state !is GymState.Failed) return@intent
            val password = requestPassword() ?: return@intent
            reduce { GymState.Initial() }
            refreshData(password)
        } finally {
            mutex.unlock()
        }
    }

    @OptIn(OrbitExperimental::class)
    private suspend fun Syntax<GymState, GymSideEffect>.refreshData(password: String) {
        runOn<GymState.Success> {
            reduce { state.copy(loading = true) }
        }
        val data = try {
            val data = loginAndGetData(password) ?: return
            // 三类数据全部获取成功后再替换，任意一步失败都保留旧缓存。
            database.withWriteTransaction {
                summaryDao.clear()
                data.value.entries.forEachIndexed { index, (summary, details) ->
                    summaryDao.insert(summary.toEntity(index))
                    detailDao.insertAll(details.mapIndexed { order, item ->
                        item.toEntity(
                            summary.id,
                            order
                        )
                    })
                    details.forEach { detail ->
                        val key = GymState.QueryKey(summary.id, detail.id)
                        gymDao.insertAll(data.histories.getValue(key).mapIndexed { order, item ->
                            item.toEntity(summary.id, detail.id, order)
                        })
                    }
                }
            }
            data
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            logger.e("无法获取体育测试数据", e)
            postSideEffect(GymSideEffect.Toast(SnackBarType.Error, "数据获取失败，详情请查看日志"))
            if (state !is GymState.Success) {
                reduce { GymState.Failed("无法获取数据，轻点页面可重试") }
            }
            runOn<GymState.Success> {
                reduce { state.copy(loading = false) }
            }
            return
        }

        // 刷新保留当前学年；首次进入或原学年已移除时选择第一项。
        val previous = state as? GymState.Success
        val selectedId = previous?.value?.keys?.elementAtOrNull(previous.currentSummaryIndex)?.id
        val index = data.value.keys.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        reduce {
            data.copy(
                currentSummaryIndex = index,
                lazyListState = previous?.lazyListState ?: LazyListState(),
            )
        }
    }

    private suspend fun Syntax<GymState, GymSideEffect>.loginAndGetData(
        initialPassword: String,
    ): GymState.Success? {
        var password = initialPassword
        while (true) {
            GymClient(AppLoginPropertiesMMKV.username).use { client ->
                try {
                    withTimeout(30.seconds) { client.login(password) }
                } catch (e: Exception) {
                    if (e is CancellationException && e !is TimeoutCancellationException) throw e
                    logger.e("体育测试登录失败", e)
                    val newPassword = requestPassword()
                    if (newPassword == null) {
                        reduce {
                            (state as? GymState.Success)?.takeIf { it.value.isNotEmpty() }
                                ?.copy(loading = false) ?: GymState.Failed("已取消登录，点击页面重新登录")
                        }
                        return null
                    }
                    password = newPassword
                    // 关闭当前客户端后，使用新密码重试。
                    return@use
                }

                val value = withTimeout(30.seconds) { client.list() }.associateWith { summary ->
                    withTimeout(30.seconds) { client.detail(summary.id) }
                }
                val histories = mutableMapOf<GymState.QueryKey, List<GymDetailHistory>>()
                for ((summary, details) in value) {
                    for ((id) in details) {
                        histories[GymState.QueryKey(summary.id, id)] = client.getHistory(summary.id, id)
                    }
                }
                AppGymMMKV.password = password
                return GymState.Success(value = value, histories = histories)
            }
        }
    }

    private suspend fun GymClient.getHistory(
        summaryId: String,
        detailId: String
    ): List<GymDetailHistory> {
        val first = withTimeout(30.seconds) { history(summaryId, detailId) }
        val result = if (first.data.size >= first.total) first else {
            withTimeout(30.seconds) { history(summaryId, detailId, size = first.total) }
        }
        check(result.data.size == result.total) { "体育测试历史记录未完整返回" }
        return result.data
    }

    private suspend fun Syntax<GymState, GymSideEffect>.requestPassword(): String? {
        val deferred = CompletableDeferred<String?>()
        reduce {
            when (val current = state) {
                is GymState.Initial -> current.copy(loginRequest = deferred)
                is GymState.Success -> current.copy(loginRequest = deferred)
                is GymState.Failed -> current.copy(loginRequest = deferred)
            }
        }
        try {
            postSideEffect(GymSideEffect.NavigateToLogin)
            return deferred.await()?.takeIf { it.isNotBlank() }
        } finally {
            reduce {
                when (val current = state) {
                    is GymState.Initial -> current.copy(loginRequest = null)
                    is GymState.Success -> current.copy(loginRequest = null)
                    is GymState.Failed -> current.copy(loginRequest = null)
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_PASSWORD = "111111"
    }
}


sealed interface GymState {
    data class QueryKey(val summaryId: String, val detailId: String)

    val loginRequest: CompletableDeferred<String?>?

    data class Initial(override val loginRequest: CompletableDeferred<String?>? = null) : GymState

    data class Failed(
        val message: String,
        override val loginRequest: CompletableDeferred<String?>? = null,
    ) : GymState

    data class Success(
        val value: Map<GymSummary, List<GymDetail>>,
        val histories: Map<QueryKey, List<GymDetailHistory>>,
        val currentSummaryIndex: Int = 0,
        val lazyListState: LazyListState = LazyListState(),
        val loading: Boolean = false,
        val history: Pair<QueryKey, List<GymDetailHistory>>? = null,
        override val loginRequest: CompletableDeferred<String?>? = null,
    ) : GymState
}

sealed interface GymSideEffect {
    data object NavigateToFilter : GymSideEffect
    data object NavigateToLogin : GymSideEffect
    data class Toast(val level: SnackBarType = SnackBarType.Info, val message: String) :
        GymSideEffect
}
