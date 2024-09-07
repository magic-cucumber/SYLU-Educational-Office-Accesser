package com.kagg886.sylu_eoa.screen.page

import android.widget.Toast
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kagg886.sylu_eoa.R
import com.kagg886.sylu_eoa.api.v2.bean.ClassUnit
import com.kagg886.sylu_eoa.api.v2.bean.findClassByWeek
import com.kagg886.sylu_eoa.currentActivity
import com.kagg886.sylu_eoa.screen.LocalTopBar
import com.kagg886.sylu_eoa.ui.componment.ClassDialog
import com.kagg886.sylu_eoa.ui.componment.ErrorPage
import com.kagg886.sylu_eoa.ui.componment.Loading
import com.kagg886.sylu_eoa.ui.model.LoadingState
import com.kagg886.sylu_eoa.ui.model.impl.ClassTableViewModel
import com.kagg886.sylu_eoa.ui.model.impl.SchoolCalenderViewModel
import com.kagg886.sylu_eoa.ui.model.impl.SyluUserViewModel
import com.kagg886.sylu_eoa.ui.theme.Typography
import com.pushpal.jetlime.*
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.LocalTime

fun getTips(): Pair<String, String> {
    val now = LocalTime.now()

    return when {
        now < LocalTime.of(3, 0) -> "半夜了！" to "zzz...zzz...zzz...？" //0:00-3:00
        now < LocalTime.of(6, 0) -> "凌晨！" to "大学生不可能起的这么早！你究竟是谁？" //3:00-6:00
        now < LocalTime.of(11, 0) -> "早上好" to "愿世上没有早八！" //6:00-11:00
        now < LocalTime.of(13, 0) -> "中午好" to "干饭人万岁！" //11:00-13:00
        now < LocalTime.of(17, 0) -> "下午好" to "午睡有助于恢复精力！有课的除外（逃）" //13:00-17:00
        now < LocalTime.of(21, 0) -> "晚上好" to "今天要通宵玩游戏吗？"//17:00-21:00
        else -> "半夜了！" to "该睡觉啦！熬夜是不好的！" //21:00-0:00
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage() {
    var top by LocalTopBar.current
    val model: SyluUserViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val sync by model.syncStatus.collectAsState()
    val user by model.data.collectAsState()
    LaunchedEffect(key1 = Unit, block = {
        top = {
            TopAppBar(title = {
                Text(text = "首页")
            }, actions = {
                val anf = rememberInfiniteTransition(label = "anim")
                val float by anf.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(500)),
                    label = "qwq"
                )
                val scope = rememberCoroutineScope()
                IconButton(onClick = {
                    scope.launch {
                        if (sync == LoadingState.LOADING) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(currentActivity(), "请等待上一次拉取数据完成后再试！", Toast.LENGTH_LONG)
                                    .show()
                            }
                            return@launch
                        }
                        if (user != null) {
                            model.loadAllData(user!!, true)
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(currentActivity(), "请等待首次拉取数据完成后再试！", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (sync == LoadingState.FAILED) Icons.Outlined.Warning else Icons.Outlined.Refresh,
                        contentDescription = "loading",
                        modifier = if (sync == LoadingState.LOADING) Modifier.rotate(float) else Modifier
                    )
                }
            })
        }
    })
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f, fill = true)
                .padding(start = 25.dp)
        ) {
            val (a, b) = getTips()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Email, contentDescription = "")
                Text(a, style = Typography.titleLarge)
            }
            Text(b, modifier = Modifier.padding(top = 20.dp))
        }
        Box(modifier = Modifier.weight(0.8f, fill = true), contentAlignment = Alignment.TopCenter) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight()
            ) {
                ClassSummary()
            }
        }
    }
}

@Composable
fun ClassSummary() {
    val tableModel: ClassTableViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val userModel: SyluUserViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val calenderViewModel: SchoolCalenderViewModel =
        viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)

    val data by tableModel.data.collectAsState()
    val user by userModel.data.collectAsState()
    val state by tableModel.loading.collectAsState()
    val state1 by calenderViewModel.loading.collectAsState()
    val err by tableModel.error.collectAsState()

    when (state) {
        LoadingState.NORMAL -> {
            LaunchedEffect(key1 = Unit) {
                calenderViewModel.loadData(user!!)
                tableModel.loadData()
            }
        }

        LoadingState.LOADING -> {
            Loading()
        }

        LoadingState.SUCCESS -> {
            if (state1 == LoadingState.SUCCESS) {
                TimeLineTable(data!!)
            } else {
                Loading()
            }
        }

        LoadingState.FAILED -> {
            if (err?.message == "need web") {
                LaunchedEffect(key1 = Unit) {
                    tableModel.loadDataByUser(user!!)
                }
                Loading()
                return
            }
            val err by tableModel.error.collectAsState()
            ErrorPage(ex = err) {
                tableModel.clearLoading()
            }
        }
    }
}

@Composable
fun TimeLineTable(data: List<ClassUnit>) {
    val calenderViewModel: SchoolCalenderViewModel =
        viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)

    val calender by calenderViewModel.data.collectAsState()

    val iconColor = if (isSystemInDarkTheme()) {
        Color.White
    } else {
        Color.Gray
    }


    //监听时间变化
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(key1 = true) {
        while (isActive) {
            currentTime = LocalDateTime.now()
            delay(60000L) // 每一分钟更新一次
        }
    }
    val list by remember(currentTime) {
        mutableStateOf(
            ItemsList(data.findClassByWeek(calender!!.currentWeek())
                .filter { it.dayInWeek.toInt() == currentTime.dayOfWeek.value })
        )
    }

    if (list.items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "今日无课！可以狠狠睡觉！")
        }
        return
    }

    JetLimeColumn(
        modifier = Modifier.padding(16.dp),
        itemsList = list,
        key = { _, item -> item.hashCode() },
        style = JetLimeDefaults.columnStyle(
            contentDistance = 32.dp,
            itemSpacing = 16.dp,
            lineThickness = 2.dp,
            lineBrush = JetLimeDefaults.lineSolidBrush(color = iconColor),
        ),
    ) { _, unit, position ->
        var type by remember {
            mutableStateOf(getTypeInClass(unit))
        }

        LaunchedEffect(key1 = currentTime) {
            type = getTypeInClass(unit, currentTime.toLocalTime())
        }
        JetLimeEvent(
            style = JetLimeEventDefaults.eventStyle(
                position = position,
                pointColor = Color(0xFF2889D6),
                pointFillColor = Color(0xFFD5F2FF),
                pointRadius = 14.dp,
                pointAnimation = if (type == ClassType.PROCESS) JetLimeEventDefaults.pointAnimation() else null,
                pointType = when (type) {
                    ClassType.SUCCESS -> EventPointType.custom(painterResource(id = R.drawable.ic_check))

                    else -> EventPointType.filled(fillPercent = 0.8f)
                },
                pointStrokeColor = MaterialTheme.colorScheme.onBackground,
            ),
        ) {
            var dialog by remember {
                mutableStateOf(false)
            }
            ClassDialog(onDismiss = { dialog = false }, unit = unit, dialog = dialog)

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(height = 90.dp)
                    .padding(3.dp)
                    .clickable {
                        dialog = true
                    },
                colors = CardDefaults.cardColors(
                    containerColor = Color(unit.name.hashCode())
                )
            ) {
                Column {
                    Text(
                        unit.name,
                        style = Typography.bodyLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = if (unit.isDegreeProgram) Color.Red else Color.Unspecified
                    )
                    Text(unit.room, style = Typography.bodyMedium)
                    Text(getTime(unit).toString(), style = Typography.bodySmall)
                }
            }
        }
    }
}

fun getTime(u: ClassUnit): Pair<LocalTime, LocalTime> {
    return getTime((u.rangeEveryDay[0] + 1) / 2)
}

fun getTime(dt:Int) : Pair<LocalTime, LocalTime> {
    return when (dt) {
        1 -> LocalTime.of(8, 0) to LocalTime.of(9, 40)
        2 -> LocalTime.of(10, 0) to LocalTime.of(11, 40)
        3 -> LocalTime.of(13, 0) to LocalTime.of(14, 40)
        4 -> LocalTime.of(14, 50) to LocalTime.of(16, 30)
        5 -> LocalTime.of(16, 40) to LocalTime.of(18, 20)
        6 -> LocalTime.of(19, 30) to LocalTime.of(21, 10)
        else -> throw IllegalStateException("no this class")
    }
 }

private fun getTypeInClass(u: ClassUnit, now: LocalTime = LocalTime.now()): ClassType {
    val (start, end) = getTime(u)
    return when {
        now < start -> ClassType.WAIT
        now > end -> ClassType.SUCCESS
        else -> ClassType.PROCESS
    }
}

enum class ClassType {
    WAIT, PROCESS, SUCCESS
}