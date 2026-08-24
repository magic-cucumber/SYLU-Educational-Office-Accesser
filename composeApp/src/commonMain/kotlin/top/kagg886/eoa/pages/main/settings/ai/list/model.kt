package top.kagg886.eoa.pages.main.settings.ai.list

import top.kagg886.eoa.util.BaseViewModel
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.LLMProviderEntity

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/6/30 17:52
 * ================================================
 */

class AISettingsListModel(database: AppDatabase) : BaseViewModel<AISettingsListState, AISettingsListSideEffect>(name = "AISettingsListModel", initial = AISettingsListState.Loading) {
    private val dao = database.llmProviderDao()

    override suspend fun Syntax<AISettingsListState, AISettingsListSideEffect>.init() {
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
