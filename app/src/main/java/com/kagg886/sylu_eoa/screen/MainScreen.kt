package com.kagg886.sylu_eoa.screen

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kagg886.sylu_eoa.util.PageConfig


val LocalNavController = compositionLocalOf<NavHostController> {
    error("NavController not provided")
}
val LocalFABProvider = compositionLocalOf<MutableState<(@Composable () -> Unit)?>> {
    error("LocalFABProvider not provided")
}

val LocalTopBar = compositionLocalOf<MutableState<(@Composable () -> Unit)?>> {
    error("LocalTopBar not provided")
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun MainScreen() {

    val nav = rememberNavController()

    val reg by nav.currentBackStackEntryAsState()

    CompositionLocalProvider(
        LocalFABProvider provides remember {
            mutableStateOf(null)
        },
        LocalTopBar provides remember {
            mutableStateOf(null)
        },
    ) {
        Scaffold(bottomBar = {
            NavigationBar {
                PageConfig.nav.forEach { entry ->
                    val select = entry.router == (reg?.destination?.route ?: PageConfig.DEFAULT_ROUTER)
                    NavigationBarItem(
                        icon = {
                            Icon(painter = painterResource(entry.icon), "")
                        },
                        label = {
                            Text(entry.title)
                        },
                        selected = select,
                        onClick = {
                            if (!select) {
                                nav.navigate(entry.router)
                            }
                        },
                        alwaysShowLabel = false
                    )
                }
            }
        }, topBar = {
            val top by LocalTopBar.current

            val state = remember(top) {
                top.hashCode()
            }
            AnimatedContent(
                targetState = state,
                label = "topbar",
                transitionSpec = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down
                    ) togetherWith slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down
                    )
                }
            ) { _ ->
                top?.invoke()
            }
        }, floatingActionButton = {
            var showing by remember {
                mutableStateOf(false)
            }
            val fab = LocalFABProvider.current
            LaunchedEffect(key1 = LocalFABProvider.current.value, block = {
                showing = fab.value != null
            })
            AnimatedVisibility(visible = showing,
                enter = fadeIn() + slideInHorizontally { it / 2 },
                exit = fadeOut() + slideOutHorizontally { it / 2 }
            ) {
                if (fab.value != null) {
                    fab.value!!()
                } else {
                    FloatingActionButton(onClick = { /*TODO*/ }) {

                    }
                }
            }
        }) {
            CompositionLocalProvider(
                LocalNavController provides nav,
            ) {
                NavHost(
                    navController = nav,
                    startDestination = PageConfig.DEFAULT_ROUTER,
                    modifier = Modifier
                        .padding(it)
                        .fillMaxSize(),

                    enterTransition = {
                        fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(1))
                    }
                ) {
                    PageConfig.allPage.forEach { entry ->
                        composable(entry.router) {
                            entry.widget()
                        }
                    }
                }
            }
        }
    }

}