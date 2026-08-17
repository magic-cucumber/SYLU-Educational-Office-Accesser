package top.kagg886.eoa.pages.welcome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.pages.rootViewModel

@Composable
fun welcomeModelOrNull(): WelcomeViewModel? {
    val nav = LocalNavController.current

    val state by nav.currentBackStackEntryAsState()
    val parentEntry = remember(state) {
        runCatching { nav.getBackStackEntry(WelcomeRoute) }.getOrNull() // 嵌套图 route
    }

    if (parentEntry == null) {
        return null
    }

    val rootModel = rootViewModel()
    return viewModel(parentEntry, key = rootModel.toString()) {
        WelcomeViewModel()
    }
}


class WelcomeViewModel : ViewModel(), OrbitContainerHost<WelcomeViewModelState, WelcomeViewModelState, WelcomeSideEffect> {
    override val container: OrbitContainer<WelcomeViewModelState, WelcomeViewModelState, WelcomeSideEffect> =
        orbitContainer(WelcomeViewModelState.Empty) {
            if (AppInitializeMMKV.initialize) {
                completeWelcomeWithoutTutorial().join()
                return@orbitContainer
            }
            reduce {
                WelcomeViewModelState.Welcome
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
}

sealed interface WelcomeViewModelState {
    data object Empty: WelcomeViewModelState
    data object Welcome : WelcomeViewModelState
}

sealed interface WelcomeSideEffect {
    data object NavigateToLogin : WelcomeSideEffect
    data class NavigateToURL(val url: String) : WelcomeSideEffect
}
