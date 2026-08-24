package top.kagg886.eoa.pages.main.settings.list

import top.kagg886.eoa.util.BaseViewModel
import kotlin.time.Instant
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.sylu_eoa.api.v2.bean.UserProfile

class SettingsModel(
    private val syncState: MainRouteViewState,
    database: AppDatabase
) : BaseViewModel<SettingsState, SettingsEffect>(name = "SettingsModel", initial = SettingsState.Loading) {
    private val syncDao = database.syncRecordDao()
    override suspend fun Syntax<SettingsState, SettingsEffect>.init() {
            if (syncState is MainRouteViewState.SyncFailed) {
                // 非首次同步则展示脏数据
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return
                }
                // 否则提示同步失败
                reduce {
                    SettingsState.Failed(syncState.message)
                }
                return
            }

            // 正在同步则展示加载中
            if (syncState is MainRouteViewState.SyncProcess) {
                // 如果有脏数据则展示
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return
                }
                // 否则展示加载中
                reduce {
                    SettingsState.Loading
                }
                return
            }

            // 同步成功则展示数据
            if (syncState is MainRouteViewState.SyncSuccess) {
                setDataUnsafe().join()
                return
            }
    }

    private fun setDataUnsafe() = intent {
        val profile = AppSyncMMKV.profile!!
        val last = syncDao.getLastSyncTime()!!.let { Instant.fromEpochMilliseconds(it) }
        reduce {
            SettingsState.Success(
                stuId = AppLoginPropertiesMMKV.username,
                profile = profile,
                lastUpdateTime = last
            )
        }
    }
}

sealed interface SettingsState {
    data object Loading : SettingsState
    data class Failed(val msg: String) : SettingsState
    data class Success(
        val stuId: String,
        val profile: UserProfile,
        val lastUpdateTime: Instant,
    ) : SettingsState
}

sealed interface SettingsEffect {
}
