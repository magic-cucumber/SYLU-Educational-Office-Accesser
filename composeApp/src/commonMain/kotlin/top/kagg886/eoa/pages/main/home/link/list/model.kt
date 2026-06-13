package top.kagg886.eoa.pages.main.home.link.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.eoa.pages.main.home.link.Link
import top.kagg886.util.asKtorLogger
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.http.HttpClient

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/2 10:04
 * ================================================
 */
class LinkListModel : ViewModel(), ContainerHost<LinkListState, LinkListEffect> {
    private val logger = "LinkModel".asTaggedLogger
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
    override val container: Container<LinkListState, LinkListEffect> = container(LinkListState.Loading) {
        val exists = AppInitializeMMKV.link

        viewModelScope.launch block@{
            val latest = try {
                client.get("https://gitee.com/kagg886/sylu-educational-office-accesser/raw/master-4.0/runtime/link.json")
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
        client.close()
    }
}


sealed interface LinkListState {
    data object Loading : LinkListState
    data class Error(val message: String) : LinkListState
    data class Success(val link: List<Link>) : LinkListState
}

sealed interface LinkListEffect
