package top.kagg886.eoa.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ErrorPage(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
    },
    title: @Composable  () -> Unit,
    message: @Composable  () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Spacer(modifier = Modifier.height(16.dp))

        ProvideTextStyle(
            MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            )
        ) {
            title()
        }

        Spacer(modifier = Modifier.height(8.dp))
        ProvideTextStyle(
            MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center
            )
        ) {
            message()
        }
    }

}


@Preview(showBackground = true)
@Composable
fun PreviewErrorPage() {
    ErrorPage(
        title = { Text("发生错误") },
        message = { Text("请稍后重试。") },
        modifier = Modifier.fillMaxSize()
    )
}
