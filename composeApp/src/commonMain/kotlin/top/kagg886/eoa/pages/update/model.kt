package top.kagg886.eoa.pages.update

import androidx.lifecycle.ViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.util.SnackBarType

class UpdateModel : ViewModel(), ContainerHost<UpdateState, UpdateEvent> {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }
    override val container = container<UpdateState, UpdateEvent>(UpdateState.None) {
        checkUpdate().join()
    }


    fun checkUpdate() = intent {
        val info = try {
            client.get("https://gitee.com/api/v5/repos/kagg886/sylu-educational-office-accesser/releases/latest")
                .body<UpdateInfo>()
        } catch (e: Exception) {
            postSideEffect(UpdateEvent.Toast(SnackBarType.Error, "检查更新失败，请检查网络连接。"))
            return@intent
        }
        if (info.name != BuildConfig.APP_VERSION_NAME) {
            postSideEffect(UpdateEvent.NavigateToUpdatePage(info))
        } else {
            postSideEffect(UpdateEvent.Toast(SnackBarType.Success, "已是最新版本"))
        }
    }
}


sealed interface UpdateState {
    data object None : UpdateState
}

sealed interface UpdateEvent {
    data class NavigateToUpdatePage(val data: UpdateInfo): UpdateEvent
    data class Toast(val type: SnackBarType, val msg: String) : UpdateEvent
}