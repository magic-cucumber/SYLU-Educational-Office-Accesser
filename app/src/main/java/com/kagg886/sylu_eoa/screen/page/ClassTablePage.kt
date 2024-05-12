package com.kagg886.sylu_eoa.screen.page

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastJoinToString
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kagg886.sylu_eoa.MainActivity
import com.kagg886.sylu_eoa.api.v2.bean.ClassUnit
import com.kagg886.sylu_eoa.api.v2.bean.SchoolCalender
import com.kagg886.sylu_eoa.api.v2.bean.findClassByWeek
import com.kagg886.sylu_eoa.getApp
import com.kagg886.sylu_eoa.screen.LocalFABProvider
import com.kagg886.sylu_eoa.screen.LocalTopBar
import com.kagg886.sylu_eoa.toast
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
import kotlinx.coroutines.withContext
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.TimeZone
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.validate.ValidationEntry
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.*
import kotlin.IllegalStateException
import kotlin.OptIn
import kotlin.Pair
import kotlin.String
import kotlin.Unit
import kotlin.apply
import kotlin.to

private val log = createLogger("MainPage")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Picker() {
    val calenderViewModel: SchoolCalenderViewModel =
        viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val currentIndex by calenderViewModel.currentWeekIndex.collectAsState()
    val defaultIndex by calenderViewModel.defaultWeekIndex.collectAsState()
    val all by calenderViewModel.all.collectAsState()


    var choosePick by remember {
        mutableStateOf(false)
    }

    if (currentIndex != defaultIndex) {
        var fab by LocalFABProvider.current
        DisposableEffect(key1 = Unit, effect = {
            fab = {
                FloatingActionButton(onClick = {
                    calenderViewModel.setCurrentSelectedWeek(defaultIndex)
                }) {
                    Text(text = "今")
                }
            }
            onDispose {
                fab = null
            }
        })
    }

    if (choosePick) {
        ModalBottomSheet(onDismissRequest = {
            choosePick = false
        }) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items((1..all).toList()) {
                    ListItem(headlineContent = {
                        Text(
                            "第${it}周".plus(
                                when (it) {
                                    defaultIndex -> "(本周)"
                                    currentIndex -> "(当前选择周)"
                                    else -> ""
                                }
                            )
                        )
                    }, modifier = Modifier.clickable {
                        calenderViewModel.setCurrentSelectedWeek(it)
                        choosePick = false
                    })
                }
            }
        }
    }

    Text("第${currentIndex}周，共${all}周",
        style = Typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                choosePick = true
            })
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
        state = pagerState, modifier = Modifier.fillMaxSize()
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

private sealed interface Result {
    data object Grant : Result

    sealed interface Deny : Result
    data object DenyOnce : Deny
    data object DenyAll : Deny
}

@OptIn(ExperimentalMaterial3Api::class)
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

    var promise by remember {
        mutableStateOf<Promise<List<String>, Result>?>(null)
    }

    val avt = LocalContext.current as MainActivity
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions(), onResult = {
            val bool = it.map { (_, v) -> v }.toList()
            var bool1 = true
            bool.forEach { it1 ->
                bool1 = it1 && bool1
            }
            if (!bool1) {
                it.forEach {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(avt, it.key)) {
                        promise!!.resolve(Result.DenyAll)
                        return@rememberLauncherForActivityResult
                    }
                }
                promise!!.resolve(Result.DenyOnce)
                return@rememberLauncherForActivityResult
            }
            promise!!.resolve(Result.Grant)
        })

    promise = Promise {
        launcher.launch(it!!.toTypedArray())
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
                Loading(fullScreen = false)
            }
        })
    }
    var exportCalenderDialog by remember {
        mutableStateOf(false)
    }
    if (exportCalenderDialog) {
        AlertDialog(onDismissRequest = { exportCalenderDialog = false }, confirmButton = {
            val course by tableModel.data.collectAsState()
            val calender by calenderViewModel.data.collectAsState()
            TextButton(onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    val code = promise!!.startForResult(
                        listOf(
                            android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR
                        )
                    )
                    if (code is Result.Deny) {
                        exportCalenderDialog = false
                        getApp().apply {
                            toast("请手动前往设置页面授予日历权限!")
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                setData(Uri.fromParts("package", packageName, null))
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        }
                        return@launch
                    }
                    if (code == Result.Grant) {
                        exportCalenderDialog = false
                        complete = 0
                        Calender("sylu_class_calender").apply {
                            clearEvents()
                            insertEvents(
                                course!!.flatMapToEvent(calender!!),
                                getApp().getConfig(CalenderTipTime).first()
                            )
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

    var exportICSDialog by remember {
        mutableStateOf(false)
    }
    if (exportICSDialog) {
        val course by tableModel.data.collectAsState()
        val calender by calenderViewModel.data.collectAsState()
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {},
            title = { Text(text = "导出中...") },
            text = { Loading(fullScreen = false) })

        LaunchedEffect(key1 = Unit) {
            val app = getApp()
            val tip = app.getConfig(CalenderTipTime).first()
            withContext(Dispatchers.IO) {
                val calendar = Calendar()
                calendar.properties.add(ProdId("-//Ben Fortuna//iCal4j 1.0//EN"))
                calendar.properties.add(Version.VERSION_2_0)
                calendar.properties.add(CalScale.GREGORIAN)
                calendar.properties.add(XProperty("X-WR-TIMEZONE", TimeZone.getDefault().id))

                course!!.flatMapToEvent(calender!!).forEach { course ->
                    val event = VEvent(
                        DateTime(course.startDate),
                        DateTime(course.endDate),
                        course.title
                    )
                    event.properties.add(Location(course.location))
                    event.properties.add(Description(course.description))
                    event.properties.add(Uid(UUID.randomUUID().toString()))
                    val alarm = VAlarm(DateTime(course.startDate - tip.toLong()))
                    alarm.properties.add(Description("alarm"))
                    alarm.properties.add(Action.DISPLAY)
                    event.alarms.add(alarm)

                    calendar.components.add(event)
                }

                val result = calendar.validate()
                check(!result.hasErrors()) {
                    "导出ics文件失败!\n${
                        result.entries.filter { it.severity == ValidationEntry.Severity.ERROR }.map {
                            "${it.context}-->${it.message}"
                        }.fastJoinToString("\n")
                    }"
                }

                val file = File(app.externalCacheDir,"event.ics")
                if (file.exists()) {
                    file.delete()
                }
                file.parentFile!!.mkdirs()
                file.createNewFile()

                CalendarOutputter().output(calendar, FileOutputStream(file))


                val intent = Intent("android.intent.action.SEND")
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(
                    "android.intent.extra.STREAM",
                    FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
                )
                intent.setType("*/*")
                app.startActivity(intent)
                exportICSDialog = false
            }
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
                    var top by LocalTopBar.current
                    LaunchedEffect(key1 = Unit, block = {
                        top = {
                            TopAppBar(title = {
                                Picker()
                            }, actions = {
                                var expanded by remember {
                                    mutableStateOf(false)
                                }
                                IconButton(onClick = { expanded = true }) {
                                    Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "")
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    DropdownMenuItem(text = { Text(text = "导出到日历") }, onClick = {
                                        expanded = false
                                        exportCalenderDialog = true
                                    })
                                    DropdownMenuItem(text = { Text(text = "导出到ICS") }, onClick = {
                                        expanded = false
                                        exportICSDialog = true
                                    })
                                }
                            })
                        }
                    })
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


private fun List<ClassUnit>.flatMapToEvent(calender: SchoolCalender): List<Event> {
    return flatMap {
        val l = mutableListOf<Event>()
        //计算什么时候有课
        for (week in 1..calender!!.count()) {
            for (day in 1..7) {
                //该天有课
                if (it.rangeAllTerm.contains(week) && day == it.dayInWeek.toInt()) {
                    val (start, end) = getTime(it)
                    l.add(
                        Event(
                            title = it.name,
                            description = "${it.teacher}(${it.weekEachLesson})",
                            location = it.room,
                            startDate = LocalDateTime.of(
                                calender!!.start.plusWeeks((week - 1).toLong())
                                    .plusDays((day.toLong() - 1) % 7), start
                            ).toInstant(
                                ZoneOffset.of("+8")
                            ).toEpochMilli(),
                            endDate = LocalDateTime.of(
                                calender!!.start.plusWeeks((week - 1).toLong())
                                    .plusDays((day.toLong() - 1) % 7), end
                            ).toInstant(
                                ZoneOffset.of("+8")
                            ).toEpochMilli()
                        )
                    )
                }
            }
        }
        return@flatMap l
    }
}