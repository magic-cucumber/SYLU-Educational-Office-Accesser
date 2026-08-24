package top.kagg886.eoa.pages.main.settings.ai.edit

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.StructureFixingParser
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.structure.StructuredRequestConfig
import ai.koog.prompt.structure.json.JsonStructure
import ai.koog.prompt.structure.json.generator.StandardJsonSchemaGenerator
import ai.koog.prompt.text.text
import top.kagg886.eoa.util.BaseViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import okio.ByteString.Companion.decodeBase64
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.LLMProviderEntity
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.asKtorLogger
import top.kagg886.util.http.HttpClient
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LLMProviderEditModel(
    database: AppDatabase,
    private val uuid: String?,
) : BaseViewModel<LLMProviderEditState, LLMProviderEditSideEffect>(name = "LLMProviderEditModel", initial = LLMProviderEditState.Loading) {
    private val dao = database.llmProviderDao()

    override suspend fun Syntax<LLMProviderEditState, LLMProviderEditSideEffect>.init() {
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

        val agent = MultiLLMPromptExecutor(
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
            ),
        )

        addCloseable(agent)


        val model = OpenAIModels.Chat.GPT4o.copy(id = item.modelName)

        val fixingParser = StructureFixingParser(
            model = model,
            retries = 1
        )

        suspend fun clean() {
            agent.close()
            runOn<LLMProviderEditState.Success> {
                reduce { state.copy(confirming = false) }
            }
        }

        val base = try {
            agent.execute(
                prompt = prompt("llm-provider-basic-validation") {
                    user("我正在测试模型是否运行正常，返回李白的一首诗来确认模型正常。")
                },
                model = model,
            )
            true
        } catch (e: Exception) {
            logger.e("无法验证AI配置是否有效", e)
            postSideEffect(
                LLMProviderEditSideEffect.Toast(
                    SnackBarType.Error,
                    "无法验证当前配置是否有效，请查看系统日志"
                )
            )
            clean()
            false
        }

        if (!base) {
            return@intent
        }

        val imageValidateTask = viewModelScope.async {
            try {
                agent.execute(
                    prompt = prompt("llm-provider-image-validation") {
                        user {
                            text("请检查随消息发送的图片: validation.png，并告诉我图片的人物是谁")
                            image(
                                AttachmentSource.Image(
                                    content = AttachmentContent.Binary.Bytes(
                                        data = Base64.decode(
                                            "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"
                                        )
                                    ),
                                    format = "png",
                                    mimeType = "image/png",
                                    fileName = "validation.png"
                                )
                            )
                        }
                    },
                    model = model,
                )
                true
            } catch (e: Exception) {
                logger.e("无法验证模型是否支持图片", e)
                false
            }
        }

        val nativeJsonValidateTask = viewModelScope.async {
            @Serializable
            data class TestCode(
                val name: String,
                val age: Int
            )

            try {
                agent.executeStructured<TestCode>(
                    prompt = prompt("llm-provider-native-json-validation") {
                        user("我正在验证模型输出json的能力，请根据上下文自行编造合适的json")
                    },
                    model = model,
                    config = StructuredRequestConfig(
                        default = StructuredRequest.Native(
                            structure = JsonStructure.create<TestCode>(
                                schemaGenerator = StandardJsonSchemaGenerator.Default
                            )
                        )
                    ),
                    fixingParser = fixingParser
                )
                true
            } catch (e: Exception) {
                logger.e("无法验证模型是否支持原生json", e)
                false
            }
        }


        val target = item.copy(
            uuid = item.uuid.ifBlank { Uuid.random().toString() },
            supportMultimodal = imageValidateTask.await(),
            supportNativeJsonOutput = nativeJsonValidateTask.await(),
        )

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
