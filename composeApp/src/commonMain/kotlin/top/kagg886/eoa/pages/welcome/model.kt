package top.kagg886.eoa.pages.welcome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import top.kagg886.eoa.util.BaseViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.config.AppInitializeMMKV
import top.kagg886.backend.config.AppLoginPropertiesMMKV
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


class WelcomeViewModel : BaseViewModel<WelcomeViewModelState, WelcomeSideEffect>(name = "WelcomeViewModel", initial = WelcomeViewModelState.Empty) {
    override suspend fun Syntax<WelcomeViewModelState, WelcomeSideEffect>.init() {
            if (AppLoginPropertiesMMKV.username.isNotEmpty() && AppLoginPropertiesMMKV.password.isNotEmpty() && AppInitializeMMKV.initialize) {
                postSideEffect(WelcomeSideEffect.NavigateToMain)
                return
            }
            if (AppInitializeMMKV.initialize) {
                completeWelcomeWithoutTutorial().join()
                return
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
    data object NavigateToMain : WelcomeSideEffect
    data class NavigateToURL(val url: String) : WelcomeSideEffect
}
