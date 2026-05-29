package top.kagg886.eoa.pages.main.home.second

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.pages.main.MainRoute
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.pages.main.home.EOAHomeModule
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.settings.SettingsRoute
import top.kagg886.eoa.util.createMenuButtonAnim
import top.kagg886.eoa.util.showSnackBar
import top.kagg886.eoa.vpn.bean.CaptchaReturn
import top.kagg886.util.toFixed

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/20 15:10
 * ================================================
 */

@Serializable
data object SecondClassRoute

@Composable
fun SecondClassScreen() {
    val nav = LocalNavController.current
    val toast = LocalSnackBarHost.current
    val mainModel = mainViewModel()
    val model = viewModel(nav.getBackStackEntry(MainRoute)) { //持久化vm，确保切换页面时状态不丢失
        SecondClassModel(mainModel.database)
    }
    val state by model.collectAsState()
    HomeScreen(
        route = EOAHomeModule.SECOND,
        title = { Text("第二课堂") },
        menu = {
            val successState = state as? SecondClassState.Success
            if (successState == null) {
                SecondClassDefaultMenu()
            } else {
                val menuItemEnabled = !successState.loading
                val refreshRotation = if (successState.loading) {
                    val refreshTransition = rememberInfiniteTransition(label = "second-refresh")
                    val rotation by refreshTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "second-refresh-rotation"
                    )
                    rotation
                } else {
                    0f
                }
                var show by remember {
                    mutableStateOf(false)
                }
                IconButton(
                    onClick = {
                        show = !show
                    }
                ) {
                    AnimatedContent(
                        targetState = show,
                        label = "logcat-actions",
                        transitionSpec = createMenuButtonAnim { show }
                    ) {
                        Icon(
                            imageVector = if (it) Icons.Default.Close else Icons.Default.Menu,
                            contentDescription = null
                        )
                    }
                }
                DropdownMenu(
                    expanded = show,
                    onDismissRequest = { show = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("刷新")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "刷新",
                                modifier = Modifier.rotate(refreshRotation)
                            )
                        },
                        enabled = menuItemEnabled,
                        onClick = {
                            model.login()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("退出二课")
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, "test")
                        },
                        enabled = menuItemEnabled,
                        onClick = {
                            model.exit()
                        }
                    )
                }
            }
        }
    ) {

        model.collectSideEffect {
            when (it) {
                is SecondClassSideEffect.Toast -> toast.showSnackBar(it.level, it.message)
            }
        }

        SecondClassScreenContent(
            state = state,
            onLoginButtonClicked = { vpn, tw ->
                model.login(vpn, tw,true)
            }
        )
    }

}

@Composable
private fun SecondClassDefaultMenu() {
    val nav = LocalNavController.current
    IconButton(
        onClick = {
            nav.navigate(SettingsRoute)
        },
    ) {
        Icon(Icons.Default.AccountBox, contentDescription = "返回")
    }
}

@Composable
private fun CaptchaSlider(
    modifier: Modifier = Modifier,
    background: ByteArray,
    frontend: ByteArray,
    onResultResolved: (Int, Int) -> Unit
) {
    // 目标显示宽度（组件宽度）。
    val targetWidth: Dp = 280.dp
    val targetHeight: Dp = 160.dp
    val density = LocalDensity.current

    var canvasWidthPx by remember { mutableStateOf(0f) }
    var sliderWidthPx by remember { mutableStateOf(0f) }
    var sliderPosPx by remember { mutableStateOf(0f) }
    val maxSliderPx = (canvasWidthPx - sliderWidthPx).coerceAtLeast(0f)

    Column(modifier) {
        // 预览区域
        Box(
            modifier = Modifier
                .width(targetWidth)
                .height(targetHeight)
                .onGloballyPositioned { canvasWidthPx = it.size.width.toFloat() },
        ) {
            AsyncImage(
                model = background,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )

            val sliderPosDp = with(density) { sliderPosPx.toDp() }

            AsyncImage(
                model = frontend,
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .height(targetHeight)
                    .offset(x = sliderPosDp)
                    .onGloballyPositioned { sliderWidthPx = it.size.width.toFloat() }
            )
        }

        Spacer(Modifier.height(12.dp))

        Slider(
            value = if (maxSliderPx <= 0f) 0f else (sliderPosPx / maxSliderPx).coerceIn(0f, 1f),
            onValueChange = { ratio ->
                sliderPosPx = (ratio * maxSliderPx).coerceIn(0f, maxSliderPx)
            },
            onValueChangeFinished = {
                // 直接传组件宽度与滑块距离（组件像素）
                val canvas = canvasWidthPx.toInt()
                val move = sliderPosPx.toInt()
                onResultResolved(canvas, move)
            },
            modifier = Modifier.width(targetWidth)
        )

        Spacer(Modifier.height(8.dp))
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SecondClassScreenContent(
    state: SecondClassState,
    onLoginButtonClicked: (vpn: String, tw: String) -> Unit,
) = when (state) {
    SecondClassState.Initial -> Unit
    is SecondClassState.RequireLogin -> {
        when (state.additional) {
            null -> Unit
            is SecondClassState.RequireLogin.TOTP -> {
                var code by remember {
                    mutableStateOf("")
                }
                AlertDialog(
                    onDismissRequest = {
                        state.additional.deferred.complete(null)
                    },
                    confirmButton = {
                        TextButton(
                            enabled = code.isNotBlank(),
                            onClick = {
                                state.additional.deferred.complete(code.toInt())
                            }
                        ) {
                            Text("确认")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                state.additional.deferred.complete(null)
                            }
                        ) {
                            Text("取消")
                        }
                    },
                    title = { Text("二次验证") },
                    text = {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { value ->
                                code = value
                            },
                            singleLine = true,
                            label = { Text("请输入六位数动态口令。") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }

            is SecondClassState.RequireLogin.Captcha -> {
                val captcha = state.additional
                AlertDialog(
                    onDismissRequest = {
                        captcha.deferred.complete(null)
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                captcha.deferred.complete(null)
                            }
                        ) {
                            Text("取消")
                        }
                    },
                    title = { Text("请完成验证") },
                    text = {
                        CaptchaSlider(
                            background = captcha.background,
                            frontend = captcha.fronted,
                            onResultResolved = { all, position ->
                                captcha.deferred.complete(
                                    CaptchaReturn(all, position)
                                )
                            }
                        )
                    }
                )
            }
        }


        var vpn by remember { mutableStateOf(state.vpn) } // vpn密码
        var tw by remember { mutableStateOf(state.tw) } // 团委密码
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var vpnVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = vpn,
                        onValueChange = { vpn = it },
                        label = { Text("统一认证平台 密码") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = if (vpnVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { vpnVisible = !vpnVisible }) {
                                Icon(
                                    if (vpnVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.progress
                    )

                    var twVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = tw,
                        onValueChange = { tw = it },
                        label = { Text("团委网 密码") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = if (twVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { twVisible = !twVisible }) {
                                Icon(
                                    if (twVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.progress
                    )

                    Button(
                        onClick = { onLoginButtonClicked(vpn, tw) },
                        enabled = vpn.isNotBlank() && tw.isNotBlank() && !state.progress,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (state.progress) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("正在登录...")
                        } else {
                            Text("登录")
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "重要提示",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    val color = MaterialTheme.colorScheme.primary
                    Text(
                        buildAnnotatedString {
                            append("该功能使用校园VPN协议来实现免校园网访问二课数据。\n")
                            append("使用该功能前，请仔细回忆")

                            withLink(
                                LinkAnnotation.Url(
                                    url = "https://webvpn.sylu.edu.cn/login",
                                )
                            ) {
                                withStyle(
                                    SpanStyle(
                                        color = color,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    append("校园统一认证平台")
                                }
                            }

                            append("的密码（点击蓝色链接后，进入 '智慧理工统一身份认证登录'）。\n")
                            append("随后您需要仔细回忆团委网的密码。只有正确填写这两个密码才能正常访问二课数据。\n")
                            append("除非您修改密码，否则每次进入功能时都将自动拉取。\n")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    is SecondClassState.Success -> {
        val entries = remember(state) { state.value.entries.toList().dropLast(1) }
        val pagerState = rememberPagerState(pageCount = { entries.size })
        val scope = rememberCoroutineScope()

        Column {
            SecondaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                divider = {}
            ) {
                entries.forEachIndexed { index, (key, values) ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            val number = remember { values.sumOf { it.score } }
                            Text(
                                "${key.id}\n${number.toFixed(2)}/${key.max}",
                                color = if (number >= key.max) Color.Unspecified else MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val (_, value) = entries[page]
                LazyColumn {
                    items(value) { item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = {
                                Column {
                                    Text(item.time)
                                }
                            },
                            trailingContent = {
                                Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                    Text(
                                        "${item.score}",
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        )
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
