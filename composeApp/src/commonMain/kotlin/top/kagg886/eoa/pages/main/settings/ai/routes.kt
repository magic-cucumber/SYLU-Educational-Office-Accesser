package top.kagg886.eoa.pages.main.settings.ai

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.settings.ai.manage.AISettingsManagerRoute
import top.kagg886.eoa.pages.main.settings.ai.manage.installManagerRoute
import top.kagg886.eoa.pages.main.settings.ai.manage.list.AISettingsManagerListRoute
import top.kagg886.eoa.pages.main.settings.ai.summary.AISettingsSummaryRoute
import top.kagg886.eoa.pages.main.settings.ai.summary.AISettingsSummaryScreen

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/6/30 17:50
 * ================================================
 */

@Serializable
data object AISettingsRoute

val installAISettingsRoute: NavGraphBuilder.() -> Unit = {
    composable<AISettingsSummaryRoute> { AISettingsSummaryScreen() }
    navigation<AISettingsManagerRoute>(
        startDestination = AISettingsManagerListRoute,
        builder = installManagerRoute
    )
}
