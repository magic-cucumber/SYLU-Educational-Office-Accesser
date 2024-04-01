package com.kagg886.sylu_eoa.screen.page

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kagg886.sylu_eoa.LocalThemeTypo
import com.kagg886.sylu_eoa.getApp
import com.kagg886.sylu_eoa.screen.LocalNavController
import com.kagg886.sylu_eoa.toast
import com.kagg886.sylu_eoa.ui.componment.Details
import com.kagg886.sylu_eoa.ui.componment.ErrorPage
import com.kagg886.sylu_eoa.ui.componment.Loading
import com.kagg886.sylu_eoa.ui.model.LoadingState
import com.kagg886.sylu_eoa.ui.model.impl.ProfileViewModel
import com.kagg886.sylu_eoa.ui.model.impl.SyluUserViewModel
import java.time.LocalDate

@Composable
fun MePage() {
    val nav = LocalNavController.current
    val avt = LocalContext.current
    val userModel: SyluUserViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val profileModel: ProfileViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)

    val user by userModel.data.collectAsState()
    val profile by profileModel.data.collectAsState()

    val state by profileModel.loading.collectAsState()
    val err by profileModel.error.collectAsState()

    when (state) {
        LoadingState.NORMAL -> {
            LaunchedEffect(key1 = Unit) {
                profileModel.loadData()
            }
        }

        LoadingState.LOADING -> {
            Loading()
        }

        LoadingState.SUCCESS -> {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .padding(top = 30.dp, bottom = 30.dp)
                ) {
                    val profile = profile!!
                    val byte = profile.avatar
                    Row(modifier = Modifier.height(120.dp)) {
                        Image(
                            bitmap = BitmapFactory.decodeByteArray(byte, 0, byte.size).asImageBitmap(),
                            contentDescription = "头像",
                        )
                        Column(modifier = Modifier.fillMaxHeight()) {
                            var dialog by remember {
                                mutableStateOf(false)
                            }

                            if (dialog) {
                                AlertDialog(onDismissRequest = { dialog = false }, confirmButton = {}, title = {
                                    Text("个人信息")
                                }, text = {
                                    Column {
                                        Details("姓名", profile.name)
                                        Details("学号", user!!.user)
                                        Details("学院", profile.collegeName)
                                        Details("专业", profile.studyName)
                                        Details("政治面貌", profile.policy)
                                        Details("电话", profile.phone)
                                        Details("邮箱", profile.email)
                                        Details("外语语种", profile.language)
                                    }
                                })
                            }
                            Text(buildAnnotatedString {
                                withStyle(ParagraphStyle(textIndent = TextIndent(10.sp))) {
                                    withStyle(SpanStyle(fontSize = 25.sp, fontWeight = FontWeight.Bold)) {
                                        append(profile.name)
                                    }
                                    append("\n")
                                    append(profile.collegeName)
                                    append("\n")
                                    append(profile.studyName)
                                }
                            })
                            TextButton(onClick = { dialog = true }) {
                                Text("查看详情")
                            }
                        }
                    }
                }

                PageItem()
            }
        }

        LoadingState.FAILED -> {
            if (err?.message == "need web") {
                LaunchedEffect(key1 = Unit) {
                    profileModel.loadDataByUser(user!!)
                }
                Loading()
                return
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ErrorPage(ex = err, modifier = Modifier.weight(0.2f)) {
                    profileModel.clearLoading()
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxSize(0.9f)
                        .weight(0.8f), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PageItem()
                }
            }

        }
    }
}

@Composable
private fun PageItem() {
    val userModel: SyluUserViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val nav = LocalNavController.current

    var exitDialog by remember {
        mutableStateOf(false)
    }

    if (exitDialog) {
        AlertDialog(onDismissRequest = {
            exitDialog = false
        }, confirmButton = {
            TextButton(onClick = {
                userModel.clearLogin()
                getApp().toast("退出登录成功!")
            }) {
                Text("确定")
            }
        }, dismissButton = {
            TextButton(onClick = {
                exitDialog = false
            }) {
                Text("取消")
            }
        }, title = {
            Text("退出登录")
        }, text = {
            Text("这么做会清空登录信息并重新登录，确定要这么做吗？")
        })
    }
    Column(modifier = Modifier.fillMaxWidth(0.9f)) {
        ListItem(headlineContent = {
            Text("退出登录")
        }, leadingContent = {
            Icon(imageVector = Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = "")
        }, modifier = Modifier.clickable {
            exitDialog = true
        })

        ListItem(headlineContent = {
            Text("工具")
        }, leadingContent = {
            Icon(imageVector = Icons.Outlined.Build, contentDescription = "")
        }, modifier = Modifier.clickable {
            nav.navigate("ToolPage")
        })

        ListItem(headlineContent = {
            Text("设置")
        }, leadingContent = {
            Icon(imageVector = Icons.Outlined.Settings, contentDescription = "")
        }, modifier = Modifier.clickable {
            nav.navigate("SettingPage")
        })

        if (LocalDate.now().run {
                dayOfMonth == 1 && monthValue == 4
            }) {
            var dialog by remember {
                mutableIntStateOf(0)
            }
            if (dialog != 0) {
                val typo = LocalThemeTypo.current
                AlertDialog(onDismissRequest = {}, confirmButton = {
                    TextButton(onClick = {
                        dialog++
                        if (dialog == 5) {
                            dialog = 0
                            typo.value = Typography(
                                bodyLarge = TextStyle(fontSize = 100.sp),
                                bodyMedium = TextStyle(fontSize = 100.sp),
                                bodySmall = TextStyle(fontSize = 100.sp),

                                titleLarge = TextStyle(fontSize = 100.sp),
                                titleMedium = TextStyle(fontSize = 100.sp),
                                titleSmall = TextStyle(fontSize = 100.sp),

                                labelSmall = TextStyle(fontSize = 100.sp),
                                labelMedium = TextStyle(fontSize = 100.sp),
                                labelLarge = TextStyle(fontSize = 100.sp)

                            )
                        }
                    }) {
                        Text(text = "我就点")
                    }
                }, dismissButton = {
                    TextButton(onClick = { dialog = 0 }) {
                        Text(text = "对不起，不点了")
                    }
                }, title = {
                    Text(text = "我都说了你不要点我。。。")
                }, text = {
                    Text(text = "你点了我${dialog}下，再点${5 - dialog}下肯定没有好果汁吃")
                })
            }
            ListItem(headlineContent = {
                Text("不要点我!")
            }, leadingContent = {
                Icon(imageVector = Icons.Outlined.Clear, contentDescription = "")
            }, modifier = Modifier.clickable {
                dialog++
            })
        }

        ListItem(headlineContent = {
            Text("关于")
        }, leadingContent = {
            Icon(imageVector = Icons.Outlined.Star, contentDescription = "")
        }, modifier = Modifier.clickable {
            nav.navigate("AboutPage")
        })
    }
}