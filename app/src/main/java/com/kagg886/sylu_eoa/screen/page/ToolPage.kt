package com.kagg886.sylu_eoa.screen.page

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.kagg886.sylu_eoa.currentActivity
import com.kagg886.sylu_eoa.getApp
import com.kagg886.sylu_eoa.screen.LocalNavController
import com.kagg886.sylu_eoa.toast
import com.kagg886.sylu_eoa.ui.componment.ErrorPage
import com.kagg886.sylu_eoa.ui.componment.Loading
import com.kagg886.sylu_eoa.ui.model.LoadingState
import com.kagg886.sylu_eoa.ui.model.impl.GPAViewModel
import com.kagg886.sylu_eoa.ui.model.impl.ProfileViewModel
import com.kagg886.sylu_eoa.ui.model.impl.SyluUserViewModel
import com.kagg886.sylu_eoa.ui.theme.Typography
import com.kagg886.utils.createLogger
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


@Composable
fun ToolPage() {
    SettingsGroup(title = {
        Column {
            Text(text = "在线工具")
            Text(text = "首页右上角标志停止转动后且不为感叹号才能使用灰色功能。", style = Typography.titleMedium)
        }
    }) {
        val model: SyluUserViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
        val load by model.syncStatus.collectAsState()
        HorizontalDivider()
        ImageSigner()

        HorizontalDivider()
        BigInnovation()

        HorizontalDivider()
        AutoRelated(load)

        HorizontalDivider()
        SecondClassData(load)

        HorizontalDivider()
        LibraryReverser(load)
    }
}

@Composable
fun AutoRelated(load: LoadingState) {
    val scope = rememberCoroutineScope {
        Dispatchers.IO
    }
    var dialog by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val userViewModel: SyluUserViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)

    val user by userViewModel.data.collectAsState()

    if (dialog) {
        AlertDialog(onDismissRequest = { }, confirmButton = { /*TODO*/ }, title = {
            Text(text = "填写评价中")
        }, text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { progress })
                Text(text = String.format("%.2f", progress * 100))
            }
        })
    }
    SettingsMenuLink(title = {
        Text(text = "期末自动评价工具")
    }, subtitle = {
        Text(text = "填写期末强制要求的评价")
    }, modifier = Modifier.height(75.dp), icon = {
        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "")
    }, enabled = load == LoadingState.SUCCESS) {
        scope.launch {
            dialog = true
            kotlin.runCatching {
                val list = user!!.getAllUnRelatedItem()
                val all = list.size
                var cur = 0
                for (item in list) {
                    val submit = user!!.getRelatedQuestions(item).apply {
                        for (q in this) {
                            if (q.isNoLabelMode) {
                                q.labelValue = "10"
                                return@apply
                            }
                            q.select(q.choices[0])
                        }
                    }
                    user!!.submitRelatedQuestions(submit)
//                    user!!.submitRelatedQuestions(submit,SubmitType.SAVE)
                    cur++
                    progress = cur.toFloat() / all
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    Toast.makeText(currentActivity(), "评价提交失败！", Toast.LENGTH_SHORT).show()
                }
            }
            dialog = false
        }
    }
}

@Composable
fun LibraryReverser(load: LoadingState) {
    val nav = LocalNavController.current
    SettingsMenuLink(title = {
        Text(text = "图书馆自助工具")
    }, subtitle = {
        Text(text = "预约图书馆以及签到")
    }, modifier = Modifier.height(75.dp), icon = {
        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "")
    }, enabled = load == LoadingState.SUCCESS) {
        nav.navigate("LibraryReverser")
    }
}

@Composable
private fun SecondClassData(load: LoadingState) {
    val nav = LocalNavController.current
    SettingsMenuLink(title = {
        Text(text = "第二课堂")
    }, subtitle = {
        Text(text = "查看第二课堂学分")
    }, modifier = Modifier.height(75.dp), icon = {
        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "")
    }, enabled = load == LoadingState.SUCCESS) {
        nav.navigate("SecondClass")
    }
}

@Composable
private fun BigInnovation() {
    val userModel: SyluUserViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val user by userModel.data.collectAsState()
    var dialog by remember {
        mutableStateOf(false)
    }

    val gpaViewModel: GPAViewModel = viewModel()

    val state by gpaViewModel.loading.collectAsState()
    val gpa by gpaViewModel.data.collectAsState()

    if (dialog) {
        AlertDialog(onDismissRequest = { dialog = false },
            confirmButton = {},
            title = { Text(text = "大创学分详情") },
            text = {
                when (state) {
                    LoadingState.NORMAL -> {
                        gpaViewModel.loadData()
                    }

                    LoadingState.LOADING -> {
                        Loading(fullScreen = false)
                    }

                    LoadingState.SUCCESS -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(gpa!!.keys.toList()) {
                                var expand by remember {
                                    mutableStateOf(false)
                                }
                                Row {
                                    Spacer(modifier = Modifier.width(15.dp))
                                    Column {
                                        ListItem(headlineContent = { Text(text = it) }, trailingContent = {
                                            Icon(
                                                imageVector = if (expand) Icons.Outlined.KeyboardArrowDown else Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                                contentDescription = ""
                                            )
                                        }, modifier = Modifier.clickable { expand = !expand })
                                        if (expand) {
                                            LazyColumn(modifier = Modifier.height((75 * gpa!![it]!!.size).dp)) {
                                                items(gpa!![it]!!) {
                                                    ListItem(headlineContent = { Text(text = it.name) },
                                                        supportingContent = {
                                                            Text(text = it.score)
                                                        })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LoadingState.FAILED -> {
                        val err by gpaViewModel.error.collectAsState()
                        if (err?.message == "need web") {
                            LaunchedEffect(key1 = Unit) {
                                gpaViewModel.fetchUser(user!!)
                            }
                            Loading()
                            return@AlertDialog
                        }
                        ErrorPage(ex = err) {
                            gpaViewModel.clearLoading()
                        }
                    }
                }
            })
    }

    SettingsMenuLink(title = {
        Text(text = "大创学分")
    }, subtitle = {
        Text(text = "查看你获得的大创学分")
    }, modifier = Modifier.height(75.dp), icon = {
        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "")
    }) {
        dialog = true
    }

}

@Composable
private fun ImageSigner() {
    val log by remember {
        mutableStateOf(createLogger("ImageSigner"))
    }
    val uris = remember {
        mutableListOf<Uri>()
    }

    var get by remember {
        mutableIntStateOf(0)
    }
    var show by remember {
        mutableStateOf(false)
    }

    val ctx = LocalContext.current

    val profileModel: ProfileViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val userModel: SyluUserViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)

    val loading by profileModel.loading.collectAsState()
    val data by profileModel.data.collectAsState()
    val user by userModel.data.collectAsState()

    var dialogTitle by remember(uris.size) {
        mutableStateOf("共选择:${uris.size}张图片")
    }

    var job: Job? = null

    //恢复初始状态的函数
    val reset = {
        uris.clear()
        get = 0
        show = false
        dialogTitle = "共选择:${uris.size}张图片"
        if (job != null) { //未完成的话进行取消操作
            if (job!!.isCompleted.not()) {
                log.i("取消图片生成")
                job!!.cancel()
            }
        }
        job = null
    }

    if (show) {
        AlertDialog(onDismissRequest = {
            reset()
        }, confirmButton = {
            if (get == 0) {
                TextButton(onClick = {
                    val scope = CoroutineScope(Dispatchers.IO)
                    //写入字符
                    job = scope.launch {
                        val bitmaps = uris.map {
                            scope.async {
                                val mutableBitmap =
                                    BitmapFactory.decodeStream(ctx.contentResolver.openInputStream(it)).run {
                                        val i = copy(config, true)
                                        recycle()
                                        i
                                    }
                                mutableBitmap.drawText("${user!!.user}\n${data!!.name}")
                                get++
                                if (this@launch.isActive.not()) {
                                    log.i("取消图片生成:在图片生成步骤")
                                    getApp().toast("操作被取消!")
                                    cancel("取消图片生成")
                                    reset()
                                }
                                return@async mutableBitmap
                            }
                        }.awaitAll()


                        //compress or send
                        val file = when (bitmaps.size) {
                            1 -> {
                                val file = File(ctx.externalCacheDir, UUID.randomUUID().toString() + ".png").apply {
                                    createNewFile()
                                }
                                if (this@launch.isActive.not()) {
                                    log.i("取消图片生成:在单图片写出步骤")
                                    getApp().toast("操作被取消!")
                                    file.delete()
                                    reset()
                                    return@launch
                                }
                                file.outputStream().use {
                                    bitmaps[0].compress(Bitmap.CompressFormat.PNG, 80, it)
                                }

                                file
                            }

                            else -> {
                                dialogTitle = "开始压缩图片..."
                                get = 0
                                val file = File(ctx.externalCacheDir, UUID.randomUUID().toString() + ".zip").apply {
                                    createNewFile()
                                }

                                ZipOutputStream(file.outputStream()).use { zip ->
                                    bitmaps.forEachIndexed { index, bitmap ->
                                        if (this@launch.isActive.not()) {
                                            log.i("取消图片生成:在压缩步骤")
                                            getApp().toast("操作被取消!")
                                            file.delete()
                                            reset()
                                            return@launch
                                        }
                                        zip.putNextEntry(ZipEntry("${index}+.png"))
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 80, zip)
                                        get++
                                    }
                                }
                                file
                            }
                        }
                        if (this@launch.isActive.not()) {
                            log.i("取消图片生成:在发送步骤")
                            getApp().toast("操作被取消!")
                            reset()
                            return@launch
                        }
                        val intent = Intent("android.intent.action.SEND")
                        intent.putExtra(
                            "android.intent.extra.STREAM",
                            FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                        )
                        intent.setType("*/*")
                        ctx.startActivity(intent)

                        getApp().toast("处理完成!")
                        reset()
                    }
                }) {
                    Text(text = "开始工作")
                }
            }
        }, title = {
            Text(text = dialogTitle)
        }, text = {
            LinearProgressIndicator(progress = {
                get.toFloat() / uris.size.toFloat()
            })
        })
    }

    val state = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents(), onResult = {
        uris.addAll(it)
        if (uris.size == 0) {
            return@rememberLauncherForActivityResult
        }
        show = true
    })
    SettingsMenuLink(title = {
        Text(text = "照片签名")
    }, subtitle = {
        Text(text = "为照片批量p入学号姓名，交报告必备。")
    }, modifier = Modifier.height(75.dp), icon = {
        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "")
    }) {
        state.launch("image/*")
    }
}

private fun Bitmap.drawText(text: String) {
    //准备画笔类
    val textPaint = Paint()
    textPaint.color = Color.BLACK
    textPaint.textSize = (width * 0.1f).coerceAtMost(height * 0.1f)

    val bgPaint = Paint()
    bgPaint.color = Color.WHITE

    val canvas = Canvas(this)
    val lines = text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    var y = 10f
    for (line in lines) {
        val len = textPaint.measureText(line)
        y += textPaint.textSize

        val left = (width - len) / 2
        canvas.drawRect(left, y - textPaint.textSize, left + len, y, bgPaint)
        //居中
        canvas.drawText(line, (width - len) / 2.0f, y, textPaint)
    }
}