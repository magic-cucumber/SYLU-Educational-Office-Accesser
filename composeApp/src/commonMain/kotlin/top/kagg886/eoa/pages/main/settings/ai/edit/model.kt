package top.kagg886.eoa.pages.main.settings.ai.edit

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import androidx.lifecycle.ViewModel
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.LLMProviderEntity
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.asKtorLogger
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.http.HttpClient
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LLMProviderEditModel(
    database: AppDatabase,
    uuid: String?,
) : ViewModel(), ContainerHost<LLMProviderEditState, LLMProviderEditSideEffect> {
    private val logger = "LLMProviderEditModel".asTaggedLogger
    private val dao = database.llmProviderDao()

    override val container: Container<LLMProviderEditState, LLMProviderEditSideEffect> =
        container(LLMProviderEditState.Loading) {
            val provider = uuid?.let { targetUuid ->
                dao.all().firstOrNull { it.uuid == targetUuid }
            } ?: emptyProvider()

            reduce {
                LLMProviderEditState.Success(provider, false)
            }
        }

    @OptIn(ExperimentalUuidApi::class, OrbitExperimental::class)
    fun save(item: LLMProviderEntity) = intent {
        runOn<LLMProviderEditState.Success> {
            reduce { state.copy(confirming = true) }
        }

        //verify AI

        val agent = AIAgent(
            promptExecutor = MultiLLMPromptExecutor(
                OpenAILLMClient(
                    apiKey = item.modelKey,
                    settings = OpenAIClientSettings(baseUrl = item.baseUrl),
                    httpClientFactory = KtorKoogHttpClient.Factory(
                        baseClient = HttpClient {
                            install(Logging) {
                                logger = this@LLMProviderEditModel.logger.asKtorLogger
                                level = LogLevel.ALL
                            }
                        }
                    )
                )
            ),
            agentConfig = AIAgentConfig(
                model = OpenAIModels.Chat.GPT4o.copy(id = item.modelName),
                prompt = prompt("prompt") {},
                maxAgentIterations = 2
            )
        )

        val success = try {
            agent.run("请回复且仅回复true来证明本客户端和服务器连通。")
            true
        } catch (e: Exception) {
            logger.e("无法验证AI配置是否有效", e)
            false
        } finally {
            agent.close()
        }

        if (!success) {
            postSideEffect(
                LLMProviderEditSideEffect.Toast(
                    SnackBarType.Error,
                    "无法验证当前配置是否有效，请查看系统日志"
                )
            )
            runOn<LLMProviderEditState.Success> {
                reduce { state.copy(confirming = false) }
            }
            return@intent
        }

        val target = if (item.uuid.isBlank()) {
            item.copy(uuid = Uuid.random().toString())
        } else {
            item
        }
        dao.insert(target)
        postSideEffect(
            LLMProviderEditSideEffect.Toast(
                SnackBarType.Success,
                "成功${if (item.uuid.isBlank()) "新建" else "编辑"}大模型"
            )
        )
        delay(3.seconds)
        postSideEffect(LLMProviderEditSideEffect.NavigateBack)
    }
}

private fun emptyProvider() = LLMProviderEntity(
    uuid = "",
    modelName = "",
    modelKey = "",
    baseUrl = "",
    supportMultimodal = false,
    supportNativeJsonOutput = false,
    modelRemark = "",
    modelDescription = "",
)

sealed interface LLMProviderEditState {
    data object Loading : LLMProviderEditState
    data class Success(val provider: LLMProviderEntity, val confirming: Boolean) : LLMProviderEditState
}

sealed interface LLMProviderEditSideEffect {
    data class Toast(val type: SnackBarType, val message: String) : LLMProviderEditSideEffect
    data object NavigateBack : LLMProviderEditSideEffect
}
