package top.kagg886.eoa.pages.main.settings.list

import androidx.lifecycle.ViewModel
import kotlin.time.Instant
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppLoginPropertiesMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.eoa.pages.main.MainRouteViewState
import top.kagg886.sylu_eoa.api.v2.bean.UserProfile

class SettingsModel(
    private val syncState: MainRouteViewState,
    database: AppDatabase
) : ViewModel(), ContainerHost<SettingsState, SettingsEffect> {
    private val syncDao = database.syncRecordDao()
    override val container: Container<SettingsState, SettingsEffect> =
        container(SettingsState.Loading) {
            if (syncState is MainRouteViewState.SyncFailed) {
                // 非首次同步则展示脏数据
                if (syncState.haveDirtyData) {
                    setDataUnsafe().join()
                    return@container
                }
                // 否则提示同步失败
                reduce {
                    SettingsState.Failed(syncState.message)
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
                    SettingsState.Loading
                }
                return@container
            }

            // 同步成功则展示数据
            if (syncState is MainRouteViewState.SyncSuccess) {
                setDataUnsafe().join()
                return@container
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
