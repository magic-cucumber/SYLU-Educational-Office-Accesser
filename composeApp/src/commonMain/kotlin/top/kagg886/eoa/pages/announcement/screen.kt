package top.kagg886.eoa.pages.announcement

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Announcement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import kotlinx.serialization.Serializable
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.dialog.DialogPageScaffold

@Serializable
data class AnnouncementRoute(
    val content: String
)

@Composable
fun AnnouncementScreen(content: AnnouncementRoute) {
    val nav = LocalNavController.current
    DialogPageScaffold(
        title = { Text("公告") },
        icon = { Icon(Icons.AutoMirrored.Filled.Announcement, "") },
        confirmButton = {
            TextButton(
                onClick = {
                    nav.popBackStack()
                }
            ) {
                Text("确定")
            }
        }
    ) {
        Markdown(
            content = content.content,
            modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
        )
    }
}
