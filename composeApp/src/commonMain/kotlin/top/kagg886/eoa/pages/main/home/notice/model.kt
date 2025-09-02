package top.kagg886.eoa.pages.main.home.notice

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.SystemNoticeEntity
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.sylu_eoa.api.v2.InvalidCredentialsException
import top.kagg886.util.asTaggedLogger
import kotlin.time.Duration.Companion.seconds

class SystemNoticeModel(
    private val syncState: MainRouteViewState,
    database: AppDatabase
) : ViewModel(), ContainerHost<SystemNoticeState, SystemNoticeSideEffect> {
    private val noticeDao = database.noticeDao()
    private val logger = "SystemNoticeModel".asTaggedLogger

    override val container: Container<SystemNoticeState, SystemNoticeSideEffect> =
        container(SystemNoticeState.Loading) {
            if (syncState is MainRouteViewState.SyncFailed) {
                // 非首次同步则展示脏数据
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return@container
                }
                // 否则提示同步失败
                reduce {
                    SystemNoticeState.Failed(syncState.message, false)
                }
                return@container
            }

            // 正在同步则展示加载中
            if (syncState is MainRouteViewState.SyncProcess) {
                // 如果有脏数据则展示
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return@container
                }
                // 否则展示加载中
                reduce {
                    SystemNoticeState.Loading
                }
                return@container
            }

            // 同步成功则展示数据
            if (syncState is MainRouteViewState.SyncSuccess) {
                setDataUnsafe().join()
                return@container
            }

            // 空状态时直接设置数据
            if (syncState is MainRouteViewState.Empty) {
                setDataUnsafe().join()
                return@container
            }
        }

    private fun setDataUnsafe(includeAll: Boolean = false) = intent {
        try {
            val notices = noticeDao.all(includeAll)

            if (notices.isEmpty()) {
                reduce {
                    SystemNoticeState.FailedButSuccess(
                        msg = "暂无系统通知",
                        includeAll = includeAll
                    )
                }
                return@intent
            }

            reduce {
                SystemNoticeState.Success(
                    notices = notices,
                    includeAll = includeAll
                )
            }
        } catch (e: Exception) {
            reduce {
                SystemNoticeState.Failed("获取通知失败: ${e.message}", includeAll)
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun toggleIncludeAll() = intent {
        runOn<SystemNoticeState.HaveIncludeAllSettings> {
            setDataUnsafe(includeAll = !state.includeAll)
        }
    }

    @OptIn(OrbitExperimental::class)
    fun markAsRead(noticeId: SystemNoticeEntity) = intent {
        runOn<SystemNoticeState.Success> {
            val success = try {
                AppLoginPropertiesMMKV.client.markNoticeReadable(noticeId.id)
            } catch (e: Exception) {
                if (e is InvalidCredentialsException) {
                    postSideEffect(
                        SystemNoticeSideEffect.Toast(
                            type = SnackBarType.Error,
                            message = "登录已失效，请重新登录"
                        )
                    )
//                    delay(3.seconds)
                    postSideEffect(SystemNoticeSideEffect.NavigateToLogin)
                    return@runOn
                }
                logger.e("标记通知失败: ${e.message}", e)
                postSideEffect(
                    SystemNoticeSideEffect.Toast(
                        type = SnackBarType.Error,
                        message = "标记通知失败，请检查日志"
                    )
                )
                false
            }
            if (!success) {
                postSideEffect(
                    SystemNoticeSideEffect.Toast(
                        type = SnackBarType.Warning,
                        message = "由于未知原因，标记通知失败"
                    )
                )
                return@runOn
            }
            noticeDao.markAsRead(noticeId.id)

            reduce {
                state.copy(
                    notices = state.notices.map { if (it.id == noticeId.id) it.copy(isRead = true) else it }
                )
            }

            postSideEffect(
                SystemNoticeSideEffect.Toast(
                    type = SnackBarType.Info,
                    message = "已标记为已读"
                )
            )
        }
    }
}

sealed interface SystemNoticeState {
    /**
     * 初始状态
     */
    data object Loading : SystemNoticeState

    sealed interface HaveIncludeAllSettings : SystemNoticeState {
        val includeAll: Boolean
    }

    /**
     * 同步成功
     */
    data class Success(
        val notices: List<SystemNoticeEntity>,
        override val includeAll: Boolean,
    ) : SystemNoticeState, HaveIncludeAllSettings

    /**
     * 同步失败
     */
    data class Failed(
        val msg: String,
        override val includeAll: Boolean,
    ) : SystemNoticeState, HaveIncludeAllSettings

    /**
     * 同步成功，但是有情况导致无法显示通知
     * 例如暂无通知
     */
    data class FailedButSuccess(
        val msg: String,
        override val includeAll: Boolean,
    ) : SystemNoticeState, HaveIncludeAllSettings
}

sealed interface SystemNoticeSideEffect {
    data object NavigateToLogin : SystemNoticeSideEffect

    data class Toast(
        val type: SnackBarType,
        val message: String,
    ) : SystemNoticeSideEffect
}

