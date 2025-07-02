package top.kagg886.eoa.pages.main.home.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.util.asTaggedLogger

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/2 10:04
 * ================================================
 */
class LinkModel : ViewModel(), ContainerHost<LinkState, LinkEffect> {
    private val logger = "LinkModel".asTaggedLogger
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }
    override val container: Container<LinkState, LinkEffect> = container(LinkState.Loading) {
        val exists = AppInitializeMMKV.link

        viewModelScope.launch block@{
            val latest = try {
                client.get("https://gitee.com/kagg886/sylu-educational-office-accesser/raw/master-4.0/runtime/link.json")
                    .body<List<Link>>()
            } catch (e: Exception) {
                logger.e("检查友链地址失败", e)
                if (exists.isEmpty()) {
                    reduce {
                        LinkState.Error("检查友链地址失败: ${e.message ?: "未知错误"}")
                    }
                }
                return@block
            }


            if (latest != exists) {
                AppInitializeMMKV.link = latest
                reduce { LinkState.Success(latest) }
            }
        }

        if (exists.isNotEmpty()) {
            reduce {
                LinkState.Success(exists)
            }
        }
    }

    override fun onCleared() {
        client.close()
    }
}


sealed interface LinkState {
    data object Loading : LinkState
    data class Error(val message: String) : LinkState
    data class Success(val link: List<Link>) : LinkState
}

sealed interface LinkEffect
