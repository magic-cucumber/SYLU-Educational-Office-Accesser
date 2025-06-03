package top.kagg886.eoa.pages.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import kotlinx.serialization.Serializable
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.config.BuildConfig

@Serializable
data class UpdateRoute(
    val version: String,
    val content: String,
    val link: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(route: UpdateRoute) {
    Surface(Modifier.fillMaxSize(0.8f)) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(text = "发现新版本：${BuildConfig.APP_VERSION_NAME} --> ${route.version}")
                },
            )

            Markdown(
                content = route.content,
                imageTransformer = Coil3ImageTransformerImpl,
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            )

            val link = LocalUriHandler.current
            val nav = LocalNavController.current
            BottomAppBar(
                actions = {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            nav.popBackStack()
                        },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            link.openUri(route.link)
                        },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text("打开链接")
                    }
                }
            )
        }
    }
}