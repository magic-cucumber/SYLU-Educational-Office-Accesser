package top.kagg886.eoa.pages.welcome

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppInitializeMMKV

class WelcomeViewModel : ViewModel(), ContainerHost<WelcomeViewModelState, WelcomeSideEffect> {
    override val container: Container<WelcomeViewModelState, WelcomeSideEffect> =
        container(WelcomeViewModelState.Empty) {
            if (AppInitializeMMKV.initialize) {
                completeWelcome().join()
                return@container
            }
            reduce {
                WelcomeViewModelState.Welcome()
            }
        }

    fun completeWelcome() = intent {
        AppInitializeMMKV.initialize = true
        postSideEffect(WelcomeSideEffect.NavigateToLogin)
    }

    @OptIn(OrbitExperimental::class)
    fun showDonationDialog() = intent {
       runOn<WelcomeViewModelState.Welcome> {
           reduce {
               state.copy(showDonationDialog = true)
           }
       }
    }

    @OptIn(OrbitExperimental::class)
    fun hideDonationDialog() = intent {
        runOn<WelcomeViewModelState.Welcome> {
            reduce {
                state.copy(showDonationDialog = false)
            }
        }
    }
}

sealed interface WelcomeViewModelState {
    data object Empty: WelcomeViewModelState
    data class Welcome(val showDonationDialog: Boolean = false) : WelcomeViewModelState
}

sealed interface WelcomeSideEffect {
    data object NavigateToLogin : WelcomeSideEffect
    data class NavigateToURL(val url: String) : WelcomeSideEffect
}
