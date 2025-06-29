package top.kagg886.eoa.pages.welcome

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.pages.login.LoginRoute


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
        onNavigateToMain = { model.completeWelcome() }
    )
}

@Preview
@Composable
private fun WelcomeScreenContent(
    state: WelcomeViewModelState,
    onNavigateToMain: () -> Unit = {},
) = when (state) {
    WelcomeViewModelState.Welcome -> {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
//            // App logo or image
//            try {
//                Image(
//                    painter = painterResource("drawable/logo.xml"),
//                    contentDescription = "App Logo",
//                    modifier = Modifier.size(120.dp)
//                )
//            } catch (e: Exception) {
//                // Fallback if image not found
//            }

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome title
            Text(
                text = "欢迎使用 EOA 5.0",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "沈阳理工大学第三方教务助手",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Action button
            Button(
                onClick = onNavigateToMain,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("开始使用")
            }
        }

    }

    WelcomeViewModelState.Empty -> Unit
}
