package top.kagg886.eoa.pages.welcome

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.orbitContainer
import top.kagg886.backend.config.AppInitializeMMKV

class WelcomeViewModel : ViewModel(), OrbitContainerHost<WelcomeViewModelState, WelcomeViewModelState, WelcomeSideEffect> {
    override val container: OrbitContainer<WelcomeViewModelState, WelcomeViewModelState, WelcomeSideEffect> =
        orbitContainer(WelcomeViewModelState.Empty) {
            if (AppInitializeMMKV.initialize) {
                completeWelcomeWithoutTutorial().join()
                return@orbitContainer
            }
            reduce {
                WelcomeViewModelState.Welcome()
            }
        }

    fun completeWelcome() = intent {
        AppInitializeMMKV.initialize = true
        postSideEffect(WelcomeSideEffect.NavigateToLogin)
    }

    fun completeWelcomeWithoutTutorial() = intent {
        AppInitializeMMKV.tutorialSummary = false
        AppInitializeMMKV.tutorialCourseList = false
        AppInitializeMMKV.tutorialCourseManage = false
        AppInitializeMMKV.tutorialExamList = false
        AppInitializeMMKV.tutorialSecondClassLogin = false
        AppInitializeMMKV.tutorialAISettings = false
        completeWelcome().join()
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
