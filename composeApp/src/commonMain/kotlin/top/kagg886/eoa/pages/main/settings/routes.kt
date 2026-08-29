package top.kagg886.eoa.pages.main.settings

import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.settings.advanced.AdvancedSettingsRoute
import top.kagg886.eoa.pages.main.settings.advanced.AdvancedSettingsScreen
import top.kagg886.eoa.pages.main.settings.ai.AISettingsRoute
import top.kagg886.eoa.pages.main.settings.ai.installAISettingsRoute
import top.kagg886.eoa.pages.main.settings.ai.list.AISettingsListRoute
import top.kagg886.eoa.pages.main.settings.appearance.AppearanceSettingsRoute
import top.kagg886.eoa.pages.main.settings.appearance.AppearanceSettingsScreen
import top.kagg886.eoa.pages.main.settings.feedback.FeedbackRoute
import top.kagg886.eoa.pages.main.settings.feedback.FeedbackScreen
import top.kagg886.eoa.pages.main.settings.list.SettingListRoute
import top.kagg886.eoa.pages.main.settings.list.SettingListScreen
import top.kagg886.eoa.pages.main.settings.logout_confirm.LogoutConfirmRoute
import top.kagg886.eoa.pages.main.settings.logout_confirm.LogoutConfirmScreen
import top.kagg886.eoa.pages.main.settings.profile.SettingsProfile
import top.kagg886.eoa.pages.main.settings.profile.SettingsProfileScreen
import top.kagg886.eoa.pages.main.settings.sync.SyncSettingsRoute
import top.kagg886.eoa.pages.main.settings.sync.SyncSettingsScreen

@Serializable
data object SettingsRoute

val installSettingsGraph: NavGraphBuilder.() -> Unit = {
    composable<SettingListRoute> { SettingListScreen() }
    composable<AppearanceSettingsRoute> { AppearanceSettingsScreen() }
    navigation<AISettingsRoute>(startDestination = AISettingsListRoute, builder = installAISettingsRoute)
    composable<SyncSettingsRoute> { SyncSettingsScreen() }
    composable<AdvancedSettingsRoute> { AdvancedSettingsScreen() }
    dialog<SettingsProfile>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { SettingsProfileScreen() }
    dialog<LogoutConfirmRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { LogoutConfirmScreen() }
    dialog<FeedbackRoute>(dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { FeedbackScreen() }
}
