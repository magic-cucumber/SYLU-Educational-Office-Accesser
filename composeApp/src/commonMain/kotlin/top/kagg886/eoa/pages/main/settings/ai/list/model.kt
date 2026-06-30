package top.kagg886.eoa.pages.main.settings.ai.list

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.LLMProviderEntity

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/6/30 17:52
 * ================================================
 */

class AISettingsListModel(database: AppDatabase) : ViewModel(), ContainerHost<AISettingsListState, AISettingsListSideEffect> {
    private val dao = database.llmProviderDao()

    override val container: Container<AISettingsListState, AISettingsListSideEffect> =
        container(AISettingsListState.Loading) {
            dao.allFlow().collect { providers ->
                reduce {
                    AISettingsListState.Success(providers)
                }
            }
        }

    fun delete(item: LLMProviderEntity) = intent {
        dao.delete(item)
        postSideEffect(AISettingsListSideEffect.Toast("成功删除大模型"))
    }
}

sealed interface AISettingsListState {
    data object Loading : AISettingsListState
    data class Success(val providers: List<LLMProviderEntity>) : AISettingsListState
}

sealed interface AISettingsListSideEffect {
    data class Toast(val message: String) : AISettingsListSideEffect
}
