package top.kagg886.eoa.pages.main.home.link.list

import top.kagg886.eoa.util.BaseViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.main.home.link.Link
import top.kagg886.util.asKtorLogger
import top.kagg886.util.http.HttpClient

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/2 10:04
 * ================================================
 */
class LinkListModel : BaseViewModel<LinkListState, LinkListEffect>(name = "LinkListModel", initial = LinkListState.Loading) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
            json(contentType = ContentType.Text.Plain)
        }

        install(Logging) {
            logger = this@LinkListModel.logger.asKtorLogger
            level = LogLevel.ALL
        }
    }
    override suspend fun Syntax<LinkListState, LinkListEffect>.init() {
        val exists = AppInitializeMMKV.link

        viewModelScope.launch block@{
            val latest = try {
                client.get("https://${BuildConfig.MESSAGE_GITEE_HOST}/kagg886/sylu-educational-office-accesser/raw/master-4.0/runtime/link.json")
                    .body<List<Link>>()
            } catch (e: Exception) {
                logger.e("检查友链地址失败", e)
                if (exists.isEmpty()) {
                    reduce {
                        LinkListState.Error("检查友链地址失败: ${e.message ?: "未知错误"}")
                    }
                }
                return@block
            }


            if (latest != exists) {
                AppInitializeMMKV.link = latest
                reduce { LinkListState.Success(latest) }
            }
        }

        if (exists.isNotEmpty()) {
            reduce {
                LinkListState.Success(exists)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}


sealed interface LinkListState {
    data object Loading : LinkListState
    data class Error(val message: String) : LinkListState
    data class Success(val link: List<Link>) : LinkListState
}

sealed interface LinkListEffect
