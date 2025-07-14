package top.kagg886.eoa.pages.update.download

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/7/14 14:05
 * ================================================
 */

@Serializable
data class UpdateDownloadRoute(val url: String)

@Composable
expect fun UpdateDownloadScreen(route: UpdateDownloadRoute)
