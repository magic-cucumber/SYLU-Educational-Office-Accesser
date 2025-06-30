package top.kagg886.eoa.pages.announcement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import kotlinx.serialization.Serializable
import top.kagg886.eoa.component.BackIconButton

@Serializable
data class AnnouncementRoute(
    val content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementScreen(content: AnnouncementRoute) {
    Surface(
        modifier = Modifier.fillMaxSize(0.8f)
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(text = "公告")
                },
                navigationIcon = {
                    BackIconButton()
                }
            )

            Markdown(
                content = content.content,
                modifier = Modifier.padding(horizontal = 16.dp).weight(1f).verticalScroll(rememberScrollState())
            )
        }
    }
}
