package top.kagg886.eoa.pages.main.home.gym.login

import kotlinx.coroutines.CompletableDeferred
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppGymMMKV
import top.kagg886.eoa.util.BaseViewModel

class GymLoginModel(
    private val result: CompletableDeferred<String?>,
) : BaseViewModel<GymLoginState, GymLoginSideEffect>(
    name = "GymLoginModel",
    initial = GymLoginState()
) {

    override suspend fun Syntax<GymLoginState, GymLoginSideEffect>.init() {
        reduce {
            state.copy(password = AppGymMMKV.password)
        }
    }

    fun confirm(password: String) = intent {
        if (password.isBlank() || state.submitting) return@intent
        result.complete(password)
        reduce { state.copy(submitting = true) }
        postSideEffect(GymLoginSideEffect.Dismiss)
    }

    fun cancel() = intent {
        if (state.submitting) return@intent
        result.complete(null)
        reduce { state.copy(submitting = true) }
        postSideEffect(GymLoginSideEffect.Dismiss)
    }

    override fun onCleared() {
        result.complete(null)
        super.onCleared()
    }
}

data class GymLoginState(val password: String = "", val submitting: Boolean = false) {
    override fun toString() =
        "GymLoginState(hasPassword=${password.isNotBlank()}, submitting=$submitting)"
}

sealed interface GymLoginSideEffect {
    data object Dismiss : GymLoginSideEffect
}
