package top.kagg886.eoa.pages.main.settings.ai.manage

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.settings.ai.manage.edit.AISettingsManagerEditRoute
import top.kagg886.eoa.pages.main.settings.ai.manage.edit.AISettingsManagerEditScreen
import top.kagg886.eoa.pages.main.settings.ai.manage.list.AISettingsManagerListRoute
import top.kagg886.eoa.pages.main.settings.ai.manage.list.AISettingsManagerListScreen

@Serializable
data object AISettingsManagerRoute


val installManagerRoute: NavGraphBuilder.() -> Unit = {
    composable<AISettingsManagerListRoute> { AISettingsManagerListScreen() }
    dialog<AISettingsManagerEditRoute>(
        dialogProperties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        AISettingsManagerEditScreen(it.toRoute())
    }
}
