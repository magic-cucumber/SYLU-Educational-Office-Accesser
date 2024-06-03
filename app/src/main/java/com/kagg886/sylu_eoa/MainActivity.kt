package com.kagg886.sylu_eoa

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kagg886.sylu_eoa.screen.LoginScreen
import com.kagg886.sylu_eoa.screen.MainScreen
import com.kagg886.sylu_eoa.screen.page.AboutPage
import com.kagg886.sylu_eoa.ui.componment.Loading
import com.kagg886.sylu_eoa.ui.componment.MaskAnimModel
import com.kagg886.sylu_eoa.ui.componment.MaskBox
import com.kagg886.sylu_eoa.ui.model.LoadingState
import com.kagg886.sylu_eoa.ui.model.impl.AppOnlineConfigViewModel
import com.kagg886.sylu_eoa.ui.model.impl.SyluUserViewModel
import com.kagg886.sylu_eoa.ui.theme.SYLU_EOATheme
import com.kagg886.sylu_eoa.ui.theme.Typography
import com.kagg886.sylu_eoa.util.NightMode
import com.kagg886.sylu_eoa.util.Promise
import com.kagg886.sylu_eoa.util.ReadAboutOnFirst
import com.kagg886.utils.createLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.IOException
import okio.use
import java.io.File
import java.io.FileOutputStream

private val log = createLogger("MainActivity")

val LocalThemeTypo = compositionLocalOf<MutableState<Typography>> {
    error("LocalThemeTypo not provided")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            //0跟随系统，1为强制日间，2为强制夜间
            val nightMode by getApp().getConfig(NightMode).collectAsState(0)
            val isNightSystem = isSystemInDarkTheme()


            var isDark by remember { //延迟变换
                mutableStateOf(false)
            }

            LaunchedEffect(key1 = nightMode) {
//                delay(1)
                isDark = when (nightMode) {
                    2 -> true
                    1 -> false
                    0 -> isNightSystem
                    else -> throw IllegalStateException("bad theme code")
                }
            }


            var s: Boolean by remember {
                mutableStateOf(true)
            }

            LaunchedEffect(key1 = Unit) {
                s = getApp().getConfig(ReadAboutOnFirst).first()
            }

            if (!s) {
                AlertDialog(onDismissRequest = {
                    s = true
                    getApp().updateConfig(ReadAboutOnFirst, true)
                }, confirmButton = {
                    Button(onClick = {
                        s = true
                        getApp().updateConfig(ReadAboutOnFirst, true)
                    }) {
                        Text(text = "已读")
                    }
                }, text = {
                    AboutPage()
                })
            }

            CompositionLocalProvider(LocalThemeTypo provides remember {
                mutableStateOf(Typography)
            }) {
                SYLU_EOATheme(
                    darkTheme = isDark,
                    typo = LocalThemeTypo.current.value
                ) {
                    MaskBox(
                        animTime = 1000L,
                        maskComplete = {
                        },
                        animFinish = {},
                    ) { emit ->
                        //非延迟变换
                        LaunchedEffect(key1 = nightMode) {
                            val d = getApp().resources.displayMetrics
                            emit(MaskAnimModel.EXPEND, d.widthPixels.toFloat() / 2, d.heightPixels.toFloat() / 2)
                        }
                        CheckUpdate()
                        Main()
                    }

                }
            }
        }
    }
}

@Composable
fun CheckUpdate() {
    val updateModel: AppOnlineConfigViewModel = viewModel()

    val state by updateModel.loading.collectAsState()
    val data by updateModel.data.collectAsState()
    val err by updateModel.error.collectAsState()
    val broadcast by updateModel.broadcast.collectAsState()
    val oldCode by remember {
        mutableStateOf(getApp().packageManager.getPackageInfo(getApp().packageName, 0).versionName)
    }

    when (state) {
        LoadingState.NORMAL -> {
            updateModel.loadData()
        }

        LoadingState.SUCCESS -> {

            var open1 by remember {
                mutableStateOf(true)
            }
            if (broadcast.isNotEmpty() && open1) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        TextButton(onClick = {
                            open1 = false
                        }) {
                            Text("确定")
                        }
                    }, title = {
                        Text("有新公告！")
                    }, text = {
                        Text(broadcast)
                    }
                )
            }

            var downloadDialog by remember {
                mutableStateOf("")
            }

            if (downloadDialog.isNotEmpty()) {
                var promise by remember {
                    mutableStateOf<Promise<Unit, Boolean>?>(null)
                }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                    promise!!.resolve(result.resultCode == Activity.RESULT_OK)
                    // 这里可以处理返回结果，但对于REQUEST_INSTALL_PACKAGES通常需要引导用户至设置页
                }
                promise = Promise {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        this.data = Uri.parse("package:${getApp().packageName}")
                    }
                    launcher.launch(intent)
                }

                //下载弹窗
                var download by remember {
                    mutableFloatStateOf(0f)
                }
                LaunchedEffect(key1 = downloadDialog) {
                    withContext(Dispatchers.IO) {
                        val response = OkHttpClient.Builder().build().newCall(
                            Request.Builder().url(downloadDialog).build()
                        ).execute()

                        val body = response.body!!
                        val size = body.contentLength()
                        var use = 0
                        FileOutputStream(File(getApp().externalCacheDir, "update.apk").apply {
                            if (exists()) {
                                delete()
                            }
                            createNewFile()
                        }).use { outStream ->
                            body.byteStream().use { inStream ->
                                val byt = ByteArray(8192)
                                var len: Int
                                while (inStream.read(byt, 0, byt.size).also { len = it } != -1) {
                                    outStream.write(byt, 0, len)
                                    use += len
                                    download = use.toFloat() / size.toFloat()
                                }
                            }
                        }
                        downloadDialog = ""
                        while (!getApp().packageManager.canRequestPackageInstalls()) {
                            promise!!.startForResult()
                        }
                        val uri = FileProvider.getUriForFile(getApp(),"${getApp().packageName}.fileprovider",File(getApp().externalCacheDir, "update.apk"))

                        getApp().startActivity(Intent(Intent.ACTION_VIEW).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            setDataAndType(uri,"application/vnd.android.package-archive")
                        })
                    }
                }
                AlertDialog(onDismissRequest = {}, confirmButton = {}, title = { Text(text = "下载中") }, text = {
                    LinearProgressIndicator(progress = {
                        download
                    })
                })
            }

            var open by remember {
                mutableStateOf(true)
            }
            if (data!!.name != oldCode && open) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        TextButton(onClick = {
                            open = false
                            downloadDialog = data!!.assets.filter {
                                it.name == "app-release.apk"
                            }[0].browser_download_url
                        }) {
                            Text("下载")
                        }
                    }, dismissButton = {
                        TextButton(onClick = {
                            open = false
                        }) {
                            Text("不更新")
                        }
                    }, title = {
                        Text("有更新！${data!!.name}")
                    }, text = {
                        Text(data!!.body)
                    }
                )
            }
        }

        LoadingState.FAILED -> {
            LaunchedEffect(key1 = Unit) {
                log.w("更新检查失败", err)
                getApp().toast("更新检查失败...")
            }
        }
    }
}

@Composable
fun Main() {
    val syluUserViewModel: SyluUserViewModel = viewModel(LocalContext.current as ViewModelStoreOwner)

    val loading by syluUserViewModel.loading.collectAsState()
    val err by syluUserViewModel.error.collectAsState()

    when (loading) {
        //开始加载
        LoadingState.NORMAL -> {
            LaunchedEffect(key1 = Unit) {
                syluUserViewModel.loadData()
            }
        }

        //加载中
        LoadingState.LOADING -> {
            Loading()
        }

        //成功登录
        LoadingState.SUCCESS -> {
            MainScreen()
        }
        //登录失败或从未登录
        LoadingState.FAILED -> {
            if (err?.message != "未登录") {
                var dialog by remember {
                    mutableStateOf(true)
                }

                if (err is IOException) {
                    syluUserViewModel.setSkipCheckLogin(true)
                    syluUserViewModel.clearLoading()
                    LocalContext.current.toast("网络连接失败，自动开启离线模式!")
                    return
                }

                if (dialog) {
                    AlertDialog(onDismissRequest = {
                        dialog = false
                    }, confirmButton = {}, title = {
                        Text("登录失败")
                    }, text = {
                        Text(err?.stackTraceToString() ?: "未知异常")
                    })
                }
            }
            log.w("登录检查失败", err)
            LoginScreen()
        }
    }
}