package com.kagg886.sylu_eoa.screen.page

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kagg886.sylu_eoa.api.v2.bean.ClassUnit
import com.kagg886.sylu_eoa.api.v2.bean.findClassByWeek
import com.kagg886.sylu_eoa.getApp
import com.kagg886.sylu_eoa.screen.LocalMenuProvider
import com.kagg886.sylu_eoa.screen.LocalNavController
import com.kagg886.sylu_eoa.screen.MenuItem
import com.kagg886.sylu_eoa.ui.componment.ClassPage
import com.kagg886.sylu_eoa.ui.componment.ErrorPage
import com.kagg886.sylu_eoa.ui.componment.Loading
import com.kagg886.sylu_eoa.ui.model.LoadingState.*
import com.kagg886.sylu_eoa.ui.model.impl.ClassTableViewModel
import com.kagg886.sylu_eoa.ui.model.impl.SchoolCalenderViewModel
import com.kagg886.sylu_eoa.ui.model.impl.SyluUserViewModel
import com.kagg886.sylu_eoa.ui.theme.Typography
import com.kagg886.sylu_eoa.util.Calender
import com.kagg886.sylu_eoa.util.CalenderTipTime
import com.kagg886.sylu_eoa.util.Event
import com.kagg886.sylu_eoa.util.Promise
import com.kagg886.utils.createLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

private val log = createLogger("MainPage")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Picker() {
    val calenderViewModel: SchoolCalenderViewModel =
        viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val currentIndex by calenderViewModel.currentWeekIndex.collectAsState()
    val all by calenderViewModel.all.collectAsState()


    val init by remember {
        mutableIntStateOf(calenderViewModel.currentWeekIndex.value)
    }

    var choosePick by remember {
        mutableStateOf(false)
    }

    if (choosePick) {
        ModalBottomSheet(onDismissRequest = {
            choosePick = false
        }) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items((1..all).toList()) {
                    ListItem(headlineContent = {
                        Text("第${it}周${if (it == init) "(当前周)" else ""}")
                    }, modifier = Modifier.clickable {
                        calenderViewModel.setCurrentSelectedWeek(it)
                        choosePick = false
                    })
                }
            }
        }
    }

    Text(
        "第${currentIndex}周，共${all}周",
        style = Typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                choosePick = true
            }
            .padding(top = 10.dp, bottom = 20.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClassTable() {
    val tableModel: ClassTableViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val calendarViewModel: SchoolCalenderViewModel =
        viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)

    val calender by calendarViewModel.data.collectAsState()

    val table by tableModel.data.collectAsState()
    val currentWeek by calendarViewModel.currentWeekIndex.collectAsState()
    val all by calendarViewModel.all.collectAsState()

    val init by remember {
        mutableIntStateOf(calendarViewModel.currentWeekIndex.value - 1)
    }

    val pagerState = rememberPagerState(
        initialPage = init,
        initialPageOffsetFraction = 0f,
    ) { all }

    LaunchedEffect(pagerState.currentPage) {
        calendarViewModel.setCurrentSelectedWeek(pagerState.currentPage + 1)
    }


    LaunchedEffect(currentWeek) {
        pagerState.animateScrollToPage(currentWeek - 1)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { index -> //从0开始
        val week by remember(index) {
            mutableStateOf(calender!!.start.plusWeeks(index.toLong()))
        }
        val classDataByWeek by remember(week) {
            mutableStateOf(table!!.findClassByWeek(index + 1))
        }
        ClassPage(date = week, classDataByWeek)
    }
}

@Composable
fun ClassTablePage() {
    val tableModel: ClassTableViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val userModel: SyluUserViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val calenderViewModel: SchoolCalenderViewModel =
        viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)

    val user by userModel.data.collectAsState()
    val state by tableModel.loading.collectAsState()
    val state1 by calenderViewModel.loading.collectAsState()
    val err by tableModel.error.collectAsState()


    val action = LocalMenuProvider.current


    var promise by remember {
        mutableStateOf<Promise<List<String>,Boolean>?>(null)
    }


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            val bool = it.map { (_,v)->v }.toList()
            var bool1 = true
            bool.forEach { it1 ->
                bool1 = it1 && bool1
            }
            promise!!.resolve(bool1)
        }
    )

    promise = Promise {
        launcher.launch(it!!.toTypedArray())
    }

    var dialog by remember {
        mutableStateOf(false)
    }

    var complete by remember {
        mutableIntStateOf(-1)
    }

    if (complete != -1) {
        AlertDialog(onDismissRequest = {
                                       if (complete == 1) {
                                           complete = -1
                                       }
        }, confirmButton = {}, title = {
            Text(text = if (complete == 1) "导入完成！" else "导入中")
        }, text = {
            if (complete == 1) {
                Text(text = "您可以卸载这个软件了！\n直到下学期课表发布...或者毕业了呢")
            } else {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        })
    }

    if (dialog) {
        AlertDialog(onDismissRequest = { dialog = false }, confirmButton = {
            val course by tableModel.data.collectAsState()
            val calender by calenderViewModel.data.collectAsState()
            TextButton(onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    val code = promise!!.startForResult(listOf(
                        android.Manifest.permission.READ_CALENDAR,
                        android.Manifest.permission.WRITE_CALENDAR
                    ))
                    if (code) {
                        dialog = false
                        complete = 0
                        Calender("sylu_class_calender").apply {
                            clearEvents()
                            insertEvents(course!!.flatMap {
                                val l = mutableListOf<Event>()
                                //计算什么时候有课
                                for (week in 1..calender!!.count()) {
                                    for (day in 1..7) {
                                        //该天有课
                                        if (it.rangeAllTerm.contains(week) && day == it.dayInWeek.toInt()) {
                                            val (start,end) = getTime(it)
                                            l.add(Event(
                                                title = it.name,
                                                description = "${it.lesson}(${it.weekEachLesson})",
                                                location = it.room,
                                                startDate = LocalDateTime.of(calender!!.start.plusWeeks(week.toLong()).plusDays(day.toLong() % 7),start).toInstant(
                                                    ZoneOffset.of("+8")).toEpochMilli(),
                                                endDate =LocalDateTime.of(calender!!.start.plusWeeks(week.toLong()).plusDays(day.toLong() % 7),end).toInstant(
                                                    ZoneOffset.of("+8")).toEpochMilli()
                                            ))
                                        }
                                    }
                                }
                                return@flatMap l
                            }, getApp().getConfig(CalenderTipTime).first())
                        }
                        complete = 1
                    }
                }
            }) {
                Text(text = "确定")
            }
        }, title = {
            Text(text = "授权提示")
        }, text = {
            Text(text = "请先授予日历读取和写入权限，否则我们无法将课表写入到您的系统日历中")
        })
    }

    DisposableEffect(key1= LocalNavController.current.currentDestination!!.route) {
        action.add(MenuItem("导出到日历") {
            dialog = true
        })
        //跳出页面时清除主页图标
        onDispose {
            action.clear()
        }
    }

    when (state) {
        NORMAL -> {
            LaunchedEffect(key1 = Unit) {
                calenderViewModel.loadData(user!!)
                tableModel.loadData()
            }
        }

        LOADING -> {
            Loading()
        }

        SUCCESS -> {
            if (state1 == SUCCESS) {
                Column {
                    Picker()
                    ClassTable()
                }
            } else {
                Loading()
            }
        }

        FAILED -> {
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

private fun getTime(u: ClassUnit): Pair<LocalTime, LocalTime> {
    val dt = (u.rangeEveryDay[0] + 1) / 2 //1-2 3-4 5-6 7-8 9-10 11-12
    return when (dt) {
        1 -> LocalTime.of(8, 0) to LocalTime.of(9, 40)
        2 -> LocalTime.of(10, 0) to LocalTime.of(11, 40)
        3 -> LocalTime.of(13, 0) to LocalTime.of(14, 40)
        4 -> LocalTime.of(15, 0) to LocalTime.of(16, 40)
        5 -> LocalTime.of(17, 0) to LocalTime.of(18, 40)
        6 -> LocalTime.of(19, 0) to LocalTime.of(20, 40)
        else -> throw IllegalStateException("no this class")
    }
}
