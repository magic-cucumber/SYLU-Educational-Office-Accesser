package top.kagg886.eoa.pages.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import sylu_eoa.composeapp.generated.resources.Res
import sylu_eoa.composeapp.generated.resources.good
import sylu_eoa.composeapp.generated.resources.icon
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.adaptive.NavigationSuiteType
import top.kagg886.eoa.pages.login.LoginRoute
import top.kagg886.eoa.util.currentLayoutType


@Serializable
data object WelcomeRoute

@Composable
fun WelcomeScreen() {
    val model = viewModel { WelcomeViewModel() }
    val state by model.collectAsState()

    val uri = LocalUriHandler.current
    val nav = LocalNavController.current
    model.collectSideEffect {
        when(it) {
            WelcomeSideEffect.NavigateToLogin -> {
                nav.navigate(LoginRoute) {
                    popUpTo(nav.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
            is WelcomeSideEffect.NavigateToURL -> {
                uri.openUri(it.url)
            }
        }
    }

    WelcomeScreenContent(
        state = state,
        onNavigateToMain = { model.completeWelcome() },
        onNavigateToMainWithoutTutorial = { model.completeWelcomeWithoutTutorial() },
        onShowDonationDialog = { model.showDonationDialog() },
        onHideDonationDialog = { model.hideDonationDialog() }
    )
}

@Preview
@Composable
private fun WelcomeScreenContent(
    state: WelcomeViewModelState = WelcomeViewModelState.Empty,
    onNavigateToMain: () -> Unit = {},
    onNavigateToMainWithoutTutorial: () -> Unit = {},
    onShowDonationDialog: () -> Unit = {},
    onHideDonationDialog: () -> Unit = {},
) = when (state) {
    is WelcomeViewModelState.Welcome -> {
        val theme = MaterialTheme.colorScheme

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo
            Card(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.icon),
                    contentDescription = "App Logo",
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome title
            Text(
                text = "欢迎使用 EOA 4.0",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = "沈阳理工大学第三方教务助手",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Features section
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                val features = listOf(
                    "📚" to "课程表",
                    "📊" to "成绩查询",
                    "📝" to "考试安排",
                    "🔗" to "校园链接",
                    "📢" to "通知提醒"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(features) { (emoji, title) ->
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .height(80.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Community links
            Text(
                text = buildAnnotatedString {
                    withLink(
                        link = LinkAnnotation.Url(
                            url = "https://qm.qq.com/q/heTEDas3Mk",
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = theme.primary,
                                    textDecoration = TextDecoration.Underline
                                ),
                                pressedStyle = SpanStyle(
                                    color = theme.primary.copy(alpha = 0.8f),
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    ) {
                        append("加入QQ群")
                    }

                    append(" · ")

                    withLink(
                        link = LinkAnnotation.Clickable(
                            tag = "donation",
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = theme.primary,
                                    textDecoration = TextDecoration.Underline
                                ),
                                pressedStyle = SpanStyle(
                                    color = theme.primary.copy(alpha = 0.8f),
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        ) {
                            onShowDonationDialog()
                        }
                    ) {
                        append("请我喝杯咖啡")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            val isPhoneLayout = currentLayoutType() == NavigationSuiteType.NavigationBar
            val buttonShape = RoundedCornerShape(28.dp)
            val buttonModifier = Modifier.height(56.dp)
            if (isPhoneLayout) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WelcomePrimaryButton(
                        onClick = onNavigateToMain,
                        modifier = buttonModifier.fillMaxWidth(),
                        shape = buttonShape,
                    )
                    WelcomeSkipTutorialButton(
                        onClick = onNavigateToMainWithoutTutorial,
                        modifier = buttonModifier.fillMaxWidth(),
                        shape = buttonShape,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WelcomePrimaryButton(
                        onClick = onNavigateToMain,
                        modifier = buttonModifier.weight(1f),
                        shape = buttonShape,
                    )
                    WelcomeSkipTutorialButton(
                        onClick = onNavigateToMainWithoutTutorial,
                        modifier = buttonModifier.weight(1f),
                        shape = buttonShape,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 赞助对话框
        if (state.showDonationDialog) {
            AlertDialog(
                onDismissRequest = onHideDonationDialog,
                confirmButton = {
                    TextButton(
                        onClick = onHideDonationDialog
                    ) {
                        Text("关闭")
                    }
                },
                icon = {
                    Icon(
                        Icons.Default.Coffee,
                        contentDescription = null,
                        tint = Color.Red
                    )
                },
                title = { Text("请我喝1杯咖啡") },
                text = {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.good),
                            contentDescription = "Donation QR Code",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            )
        }

        Unit
    }

    WelcomeViewModelState.Empty -> Unit
}

@Composable
private fun WelcomePrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = shape
    ) {
        Text(
            text = "开始使用",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WelcomeSkipTutorialButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape
    ) {
        Text(
            text = "跳过教程并开始",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
