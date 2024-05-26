package com.kagg886.sylu_eoa.screen.page

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kagg886.sylu_eoa.App
import com.kagg886.sylu_eoa.api.v2.SyluUser
import com.kagg886.sylu_eoa.api.tw.SecondClassData
import com.kagg886.sylu_eoa.api.tw.SecondClassDataSummary
import com.kagg886.sylu_eoa.api.tw.TWUser
import com.kagg886.sylu_eoa.api.tw.getTWUser
import com.kagg886.sylu_eoa.openURL
import com.kagg886.sylu_eoa.screen.LocalNavController
import com.kagg886.sylu_eoa.ui.componment.ComposeRadarView
import com.kagg886.sylu_eoa.ui.componment.Loading
import com.kagg886.sylu_eoa.ui.componment.RadarScore
import com.kagg886.sylu_eoa.ui.model.impl.SyluUserViewModel
import com.kagg886.sylu_eoa.ui.theme.Typography
import com.kagg886.sylu_eoa.util.DayExpired
import com.kagg886.sylu_eoa.util.SECClassBean
import com.kagg886.sylu_eoa.util.SECClassBeanExpire
import com.kagg886.sylu_eoa.util.SECClassPass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

typealias SecClassResult = Map<SecondClassDataSummary, List<SecondClassData>>

@Composable
fun SecondClassPage() {
    val model: SecondClassPageViewModel = viewModel()
    val state by model.state.collectAsState()

    val userModel: SyluUserViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val user by userModel.data.collectAsState()

    when (state) {
        SecondClassPageUiState.Default -> {
            model.dispatch(SecondClassPageAction.LoadingData(user!!))
        }

        SecondClassPageUiState.Loading -> {
            Loading()
        }

        is SecondClassPageUiState.RequireLoginWindow -> {
            if (state is SecondClassPageUiState.LoginFailed) {
                AlertDialog(
                    onDismissRequest = { },
                    confirmButton = {
                        TextButton(onClick = {
                            model.dispatch(SecondClassPageAction.LoadingData(user!!))
                        }) {
                            Text(text = "确定")
                        }
                    },
                    title = { Text(text = "登录失败") },
                    text = {
                        Text(text = (state as SecondClassPageUiState.LoginFailed).cause)
                    })
                return
            }

            var pass by remember(state) {
                mutableStateOf((state as SecondClassPageUiState.RequireLoginWindow).pass)
            }
            var dialog by remember(state) {
                mutableStateOf(true)
            }
            val nav = LocalNavController.current
            if (dialog) {
                AlertDialog(onDismissRequest = {
                    if (state is SecondClassPageUiState.NeedLogin) {
                        nav.popBackStack()
                    }
                    dialog = false
                }, confirmButton = {
                    TextButton(onClick = {
                        model.dispatch(SecondClassPageAction.StartLogin(user!!, pass))
                    }) {
                        Text(text = "登录")
                    }
                }, title = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "登录到团委网")
                        var help by remember {
                            mutableStateOf(false)
                        }
                        if (help) {
                            AlertDialog(
                                onDismissRequest = { help = false },
                                title = {
                                    Text(text = "密码帮助")
                                },
                                text = {
                                    val night = isSystemInDarkTheme()
                                    val text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = if (night) Color.White else Color.Black)) {
                                            withStyle(style = ParagraphStyle(textIndent = TextIndent(20.sp))) {
                                                append("该板块数据来自于：")
                                                pushStringAnnotation(
                                                    tag = "tag",
                                                    annotation = "团委网"
                                                )
                                                withStyle(
                                                    style = SpanStyle(
                                                        color = Color(0xFF0E9FF2),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) {
                                                    append("团委网")
                                                }
                                                pop()
                                                append(", 需要校园网环境才能成功连接。")
                                                appendLine()
                                                append("初始密码组合如下：")
                                                appendLine()
                                                append(
                                                    """
                                                        1. SYLU+身份证后六位+!@#
                                                        2. 学号
                                                        3. 身份证后六位
                                                    """.trimIndent()
                                                )
                                                appendLine()
                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append("遗忘密码请寻找本班团支书。")
                                                }
                                            }
                                        }
                                    }
                                    val ctx = LocalContext.current
                                    ClickableText(text = text, onClick = {
                                        text.getStringAnnotations(
                                            tag = "tag", start = it,
                                            end = it
                                        ).firstOrNull()?.let { _ ->
                                            ctx.openURL("http://xg.sylu.edu.cn/SyluTW/Sys/UserLogin.aspx")
                                        }
                                    })
                                },
                                confirmButton = {
                                    TextButton(onClick = { help = false }) {
                                        Text(text = "确认")
                                    }
                                })
                        }
                        IconButton(onClick = { help = true }) {
                            Icon(imageVector = Icons.Outlined.Info, contentDescription = "")
                        }
                    }
                }, text = {
                    OutlinedTextField(value = pass, onValueChange = { pass = it })
                })
            }
        }

        is SecondClassPageUiState.LoadingSuccess -> {
            var width by remember {
                mutableStateOf((-1).dp)
            }
            val density = LocalDensity.current

            var dialog by remember {
                mutableStateOf<Pair<SecondClassDataSummary, List<SecondClassData>>?>(null)
            }

            if (dialog != null) {
                AlertDialog(
                    onDismissRequest = { dialog = null }, confirmButton = {
                        TextButton(onClick = {
                            dialog = null
                        }) {
                            Text(text = "确定")
                        }
                    },
                    title = {
                        Text(text = "${dialog!!.first.id}详情")
                    },
                    text = {
                        LazyColumn(content = {
                            items(dialog?.second ?: emptyList()) {
                                var expand by remember {
                                    mutableStateOf(false)
                                }

                                ListItem(headlineContent = {
                                    val source by remember {
                                        mutableStateOf(MutableInteractionSource())
                                    }
                                    Text(
                                        text = it.name,
                                        maxLines = if (expand) Integer.MAX_VALUE else 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable(interactionSource = source, indication = null) {
                                            expand = !expand
                                        })
                                }, supportingContent = {
                                    Text(text = "${it.score}")
                                }, overlineContent = {
                                    Text(text = it.time.replace("00:00:00", ""))
                                })
                                HorizontalDivider()
                            }
                        })
                    }
                )
            }
            Column {
                Column(
                    modifier = Modifier
                        .weight(0.2f)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "第二课堂注意事项",
                        style = Typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = "单击文字条目得分详情，\n单击条目文字以显示全部")
                }
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxSize()
                        .onSizeChanged {
                            width = with(density) {
                                return@with it.width.toDp()
                            }
                        }, contentAlignment = Alignment.Center
                ) {
                    if (width.value > 0) {
                        val result = (state as SecondClassPageUiState.LoadingSuccess).result
                        ComposeRadarView(data = result.map { it ->
                            return@map RadarScore(it.key.id, it.value.sumOf { it.score }, it.key.max)
                        }.filter { it.text != "All" }, modifier = Modifier
                            .size(width)
                            .padding(10.dp)
                        ) { score ->
                            dialog = result.filter { it.key.id == score.text }.toList()[0]
                        }
                    }
                }

            }
        }
    }
}


private val json = Json {
    allowStructuredMapKeys = true
}

private fun decodeFormStr(s: String): SecClassResult {

    return json.decodeFromString<Map<SecondClassDataSummary, List<SecondClassData>>>(s)
}

class SecondClassPageViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<SecondClassPageUiState>(SecondClassPageUiState.Default)

    val state = _state.asStateFlow()


    fun dispatch(action: SecondClassPageAction) {
        val context = getApplication<App>()
        viewModelScope.launch {
            when (action) {
                is SecondClassPageAction.LoadingData -> {
                    setUiState(SecondClassPageUiState.Loading)
                    withContext(Dispatchers.IO) {
                        val secClass = context.getConfig(SECClassBean).first()
                        val expire = context.getConfig(SECClassBeanExpire).first()

                        if (System.currentTimeMillis() > expire) {
                            if (secClass.isNotEmpty()) {
                                //预先展示数据，后台悄悄的更新
                                setUiState(SecondClassPageUiState.LoadingSuccess(decodeFormStr(secClass)))
                            }
                            //过期后尝试重新登录，成功拉取最新数据，失败展示原数据并弹窗
                            val pass = context.getConfig(SECClassPass).first()
                            val tw: TWUser = action.user.getTWUser()
                            if (pass.isEmpty()) {
                                setUiState(SecondClassPageUiState.NeedLogin)
                                return@withContext
                            }
                            try {
                                tw.login(pass)
                                val data = tw.getData()

                                val day = context.getConfig(DayExpired).first()
                                context.updateConfig(SECClassPass, pass)
                                context.updateConfig(SECClassBean, json.encodeToString(data))
                                context.updateConfig(SECClassBeanExpire, System.currentTimeMillis() + day * 864_000_00)
                                setUiState(SecondClassPageUiState.LoadingSuccess(data))
                            } catch (e: Exception) {
                                setUiState(SecondClassPageUiState.LoginFailed(pass))
                            }
                        }
                        setUiState(SecondClassPageUiState.LoadingSuccess(decodeFormStr(secClass)))

                    }
                }

                is SecondClassPageAction.StartLogin -> {
                    setUiState(SecondClassPageUiState.Loading)
                    withContext(Dispatchers.IO) {
                        try {
                            val twu = action.user.getTWUser()
                            twu.login(action.pass)
                            val data = twu.getData()
                            setUiState(SecondClassPageUiState.LoadingSuccess(data))

                            val day = context.getConfig(DayExpired).first()
                            context.updateConfig(SECClassPass, action.pass)
                            context.updateConfig(SECClassBean, json.encodeToString(data))
                            context.updateConfig(SECClassBeanExpire, System.currentTimeMillis() + day * 864_000_00)
                        } catch (e: Exception) {
                            setUiState(SecondClassPageUiState.LoginFailed(e.message ?: "未知错误"))
                        }
                    }
                }

                else -> throw IllegalStateException("not implemented")
            }
        }
    }

    private fun setUiState(u: SecondClassPageUiState) {
        _state.value = u
    }
}

sealed class SecondClassPageUiState {
    data object Default : SecondClassPageUiState()
    data object Loading : SecondClassPageUiState()

    open class RequireLoginWindow(val pass: String) : SecondClassPageUiState()

    data object NeedLogin : RequireLoginWindow("")
    data class LoginFailed(val cause: String) : RequireLoginWindow(cause)

    data class LoadingSuccess(val result: SecClassResult) : SecondClassPageUiState()
}

sealed class SecondClassPageAction {
    data class LoadingData(val user: SyluUser) : SecondClassPageAction()
    data class StartLogin(val user: SyluUser, val pass: String) : SecondClassPageAction()
}