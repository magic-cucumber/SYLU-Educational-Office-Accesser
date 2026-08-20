package top.kagg886.eoa.pages.welcome.privacy

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import sylu_eoa.composeapp.generated.resources.Res
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.component.GuideScaffold
import top.kagg886.eoa.component.GuideScaffoldState
import top.kagg886.eoa.component.Markdown
import top.kagg886.eoa.component.rememberGuideScaffoldState
import top.kagg886.eoa.pages.welcome.WelcomeScreen
import top.kagg886.eoa.pages.welcome.done.WelcomeDoneRoute

@Serializable
data object WelcomePrivacyRoute

private enum class PolicyTab(
    val title: String,
    val resourcePath: String,
) {
    User("用户协议", "drawable/user.md"),
    Privacy("隐私政策", "drawable/privacy.md"),
}

@Composable
fun WelcomePrivacyScreen() {
    val nav = LocalNavController.current

    WelcomeScreen {
        var selectedTab by rememberSaveable { mutableStateOf(PolicyTab.User) }
        var markdown by remember { mutableStateOf(emptyMap<PolicyTab, String>()) }
        val guideState = rememberGuideScaffoldState()

        LaunchedEffect(selectedTab) {
            animate(
                initialValue = guideState.progress,
                targetValue = 0f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            ) { value, _ ->
                guideState.progress = value
            }
        }

        LaunchedEffect(selectedTab) {
            if (markdown[selectedTab] == null) {
                markdown = markdown + (selectedTab to Res.readBytes(selectedTab.resourcePath).decodeToString())
            }
        }

        GuideScaffold(
            state = guideState,
            subTitle = { Text("在开始使用前，请了解这些内容") },
            title = { Text("隐私政策与用户协议") },
            backButton = { BackIconButton() },
            confirmButton = {
                Button(onClick = { nav.navigate(WelcomeDoneRoute) }) {
                    Text("同意并继续")
                }
            },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SecondaryTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PolicyTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.title) },
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            val direction = if (targetState.ordinal > initialState.ordinal) {
                                AnimatedContentTransitionScope.SlideDirection.Start
                            } else {
                                AnimatedContentTransitionScope.SlideDirection.End
                            }
                            slideIntoContainer(direction) togetherWith slideOutOfContainer(direction)
                        },
                        label = "privacy policy content",
                    ) { tab ->
                        val content = markdown[tab]
                        if (content == null) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            val scrollState = rememberScrollState()
                            Markdown(
                                content = content,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(
                                        rememberGuideNestedScrollConnection(
                                            scrollState = scrollState,
                                            guideState = guideState,
                                        ),
                                    )
                                    .verticalScroll(scrollState)
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberGuideNestedScrollConnection(
    scrollState: ScrollState,
    guideState: GuideScaffoldState,
): NestedScrollConnection {
    return remember(scrollState, guideState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val titleHeight = guideState.expandedTitleHeightPx
                if (titleHeight <= 0) {
                    return Offset.Zero
                }

                val scrollProgress = scrollState.value.toFloat() / titleHeight
                when {
                    consumed.y < 0f -> {
                        guideState.progress = scrollProgress
                    }

                    consumed.y > 0f && scrollState.value <= titleHeight -> {
                        guideState.progress = scrollProgress
                    }
                }
                return Offset.Zero
            }
        }
    }
}
