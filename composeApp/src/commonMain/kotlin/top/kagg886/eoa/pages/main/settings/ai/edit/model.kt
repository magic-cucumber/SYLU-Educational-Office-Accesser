package top.kagg886.eoa.pages.main.settings.ai.edit

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.LLMProviderEntity
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LLMProviderEditModel(
    database: AppDatabase,
    uuid: String?,
) : ViewModel(), ContainerHost<LLMProviderEditState, LLMProviderEditSideEffect> {
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
        val target = if (item.uuid.isBlank()) {
            item.copy(uuid = Uuid.random().toString())
        } else {
            item
        }
        dao.insert(target)
        postSideEffect(LLMProviderEditSideEffect.Toast("成功${if (item.uuid.isBlank()) "新建" else "编辑"}大模型"))
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
    data class Toast(val message: String) : LLMProviderEditSideEffect
    data object NavigateBack : LLMProviderEditSideEffect
}
