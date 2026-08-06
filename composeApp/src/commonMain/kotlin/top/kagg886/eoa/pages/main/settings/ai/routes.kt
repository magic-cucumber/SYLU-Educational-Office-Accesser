package top.kagg886.eoa.pages.main.settings.ai

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.settings.ai.edit.AISettingsEditRoute
import top.kagg886.eoa.pages.main.settings.ai.edit.AISettingsEditScreen
import top.kagg886.eoa.pages.main.settings.ai.list.AISettingsListRoute
import top.kagg886.eoa.pages.main.settings.ai.list.AISettingsScreen

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/6/30 17:50
 * ================================================
 */

@Serializable
data object AISettingsRoute

val installAISettingsRoute: NavGraphBuilder.() -> Unit = {
    composable<AISettingsListRoute> { AISettingsScreen() }
    dialog<AISettingsEditRoute>(
        dialogProperties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        AISettingsEditScreen(
            it.toRoute()
        )
    }
}
