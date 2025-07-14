package top.kagg886.eoa.pages.update.detail

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import kotlinx.serialization.Serializable
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.config.BuildConfig
import top.kagg886.eoa.pages.update.download.UpdateDownloadRoute

@Serializable
data class UpdateDetailRoute(
    val version: String,
    val content: String,
    val link: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(route: UpdateDetailRoute) {
    val nav = LocalNavController.current

    DialogPageScaffold(
        title = {
            Text(text = "发现新版本：${BuildConfig.APP_VERSION_NAME} --> ${route.version}")
        },
        icon = {
            Icon(Icons.Default.Update, "")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    nav.navigate(UpdateDownloadRoute(route.link))
                },
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text("打开链接")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    nav.popBackStack()
                },
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text("取消")
            }

        }
    ) {
        Markdown(
            content = route.content,
            imageTransformer = Coil3ImageTransformerImpl,
            modifier = Modifier.verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        )
    }
}
