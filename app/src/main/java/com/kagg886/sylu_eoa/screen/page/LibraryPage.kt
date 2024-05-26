package com.kagg886.sylu_eoa.screen.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kagg886.sylu_eoa.api.seats.SeatManager
import com.kagg886.sylu_eoa.api.seats.bean.Rooms
import com.kagg886.sylu_eoa.api.seats.bean.Seat
import com.kagg886.sylu_eoa.api.seats.bean.SeatQueryModel
import com.kagg886.sylu_eoa.api.seats.bean.SeatUsage
import com.kagg886.sylu_eoa.api.seats.util.getSeatsManager
import com.kagg886.sylu_eoa.api.seats.util.isCanReserve
import com.kagg886.sylu_eoa.api.v2.SyluUser
import com.kagg886.sylu_eoa.screen.LocalNavController
import com.kagg886.sylu_eoa.screen.LocalTopBar
import com.kagg886.sylu_eoa.ui.componment.ErrorPage
import com.kagg886.sylu_eoa.ui.componment.Loading
import com.kagg886.sylu_eoa.ui.model.BaseViewModel
import com.kagg886.sylu_eoa.ui.model.LoadingState
import com.kagg886.sylu_eoa.ui.model.impl.SyluUserViewModel
import com.kagg886.utils.throttleLatest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.time.*
import kotlin.math.abs
import kotlin.math.absoluteValue

@Composable
fun LibraryPage() {
    val flow = remember {
        MutableStateFlow<ChooseTime?>(null)
    }
    val debounce = remember(flow) {
        flow.throttleLatest(1500)
    }
    ChooseTimeTopBar(flow = flow)

    val queryState by debounce.collectAsState(initial = null)
    val userModel: SyluUserViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner)
    val user by userModel.data.collectAsState()

    val model: LibraryModel = viewModel()
    val state by model.loading.collectAsState()
    val data by model.data.collectAsState()
    when (state) {
        LoadingState.NORMAL -> {
            LaunchedEffect(key1 = user) {
                user?.let {
                    model.fetchUser(user!!)
                }
            }
        }

        LoadingState.LOADING -> {
            Loading()
        }

        LoadingState.SUCCESS -> {
            val query = remember(queryState) {
                SeatQueryModel(
                    room = queryState!!.room,
                    date = queryState!!.date,
                    startTime = queryState!!.start,
                    endTime = queryState!!.end
                )
            }
            LibraryPageContent(manager = data!!, query = query)
        }

        LoadingState.FAILED -> {
            val err by model.error.collectAsState()
            ErrorPage(ex = err) {
                model.clearLoading()
            }
        }
    }
}

@Composable
fun LibraryPageContent(manager: SeatManager, query: SeatQueryModel) {
    val model: SeatModel = viewModel()
    val state by model.loading.collectAsState()

    val data by model.data.collectAsState()
    LaunchedEffect(key1 = query, block = {
        model.clearLoading()
    })
    when (state) {
        LoadingState.NORMAL -> {
            LaunchedEffect(key1 = Unit) {
                model.fetchUser(query, manager)
            }
        }

        LoadingState.LOADING -> {
            Loading()
        }

        LoadingState.SUCCESS -> {
            LazyColumn {
                data?.let {
                    items(data!!) {
                        ListItem(headlineContent = {
                            Text(text = it.title)
                        }, trailingContent = {
                            IconButton(onClick = { { /*TODO*/ } }) {
                                Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "")
                            }
                        }, leadingContent = {
                            Icon(imageVector = Icons.Outlined.ThumbUp, contentDescription = "")
                        }, supportingContent = {
                            val usage = remember(it) {
                                String.format("%.2f", (1 - it.usage.filter {
                                    it.end > LocalDateTime.now()
                                }.sumOf {
                                    val start = if (it.start < LocalDateTime.now()) LocalDateTime.now() else it.start
                                    abs(Duration.between(it.end, start).seconds)
                                } / 86400.0) * 100) //获取空闲率
                            }
                            Text(text = "空闲率：$usage%")
                        })
                    }

                }
            }
        }

        LoadingState.FAILED -> {
            val err by model.error.collectAsState()
            ErrorPage(ex = err) {
                model.clearLoading()
            }
        }
    }
}

class SeatModel : BaseViewModel<List<Seat>>() {
    override suspend fun onDataFetch(): List<Seat>? = null
    suspend fun fetchUser(config: SeatQueryModel, user: SeatManager) {
        withContext(Dispatchers.IO) {
            runCatching {
                setDataLoadSuccess(user.getSeatList(config).filter {
                    it.isCanReserve(
                        config = SeatUsage.build(
                            start = LocalDateTime.of(LocalDate.now(), config.startTime),
                            end = LocalDateTime.of(LocalDate.now(), config.endTime),
                        )
                    )
                })
            }.onFailure {
                setDataLoadError(it)
            }
        }
    }
}

class LibraryModel : BaseViewModel<SeatManager>() {
    override suspend fun onDataFetch(): SeatManager? = null
    suspend fun fetchUser(user: SyluUser) {
        withContext(Dispatchers.IO) {
            runCatching {
                setDataLoadSuccess(user.getSeatsManager())
            }.onFailure {
                setDataLoadError(it)
            }
        }
    }
}

data class ChooseTime(
    val room: Rooms,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseTimeTopBar(flow: MutableStateFlow<ChooseTime?>) {
    val nav = LocalNavController.current
    var top by LocalTopBar.current
    var dialog by remember {
        mutableStateOf(false)
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())


    var timeStartState by remember {
        mutableIntStateOf(Duration.between(LocalTime.now(), LocalTime.of(6, 0)).toMinutes().toInt().absoluteValue)
    }

    var timeEndState by remember {
        mutableIntStateOf(
            Duration.between(LocalTime.now().plusHours(1), LocalTime.of(6, 0)).toMinutes().toInt().absoluteValue
        )
    }


    var room by remember {
        mutableStateOf(Rooms.L2_LIBRARY)
    }
    LaunchedEffect(
        key1 = dateState.selectedDateMillis,
        key2 = timeStartState,
        key3 = timeEndState,
        block = {
            flow.emit(
                ChooseTime(
                    room,
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(dateState.selectedDateMillis!!), ZoneId.systemDefault()
                    ).toLocalDate(),
                    LocalTime.of(6, 0).plusMinutes(timeStartState.toLong()),
                    LocalTime.of(6, 0).plusMinutes(timeEndState.toLong())
                )
            )
        })

    LaunchedEffect(
        key1 = room,
        block = {
            flow.emit(
                ChooseTime(
                    room,
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(dateState.selectedDateMillis!!), ZoneId.systemDefault()
                    ).toLocalDate(),
                    LocalTime.of(6, 0).plusMinutes(timeStartState.toLong()),
                    LocalTime.of(6, 0).plusMinutes(timeEndState.toLong())
                )
            )
        })

    LaunchedEffect(key1 = Unit, block = {
        top = {
            TopAppBar(title = {
                TextButton(onClick = { dialog = true }) {
                    val text = buildString {
                        append(room.title)
                        append("   ")
                        if (dateState.selectedDateMillis != null) {
                            append(
                                LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(dateState.selectedDateMillis!!), ZoneId.systemDefault()
                                ).toLocalDate().toString()
                            )
                        } else {
                            append("未定日期")
                        }
                        append(
                            LocalTime.of(6, 0).plusMinutes(timeStartState.toLong()).toString()
                        )
                        append("-")
                        append(
                            LocalTime.of(6, 0).plusMinutes(timeEndState.toLong()).toString()
                        )
                    }
                    Text(text = text)
                }
            }, navigationIcon = {
                IconButton(onClick = {
                    nav.popBackStack()
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "")
                }
            })
        }
    })

    if (dialog) {
        var _step by remember {
            mutableIntStateOf(0)
            //0: 选择楼层
            //1: 选择日期
            //2: 选择开始时间
            //3: 选择结束时间
            //4: 完成
        }
        AlertDialog(onDismissRequest = {
            _step = 0
            dialog = false
        }, confirmButton = {
            Button(onClick = { _step++ }) {
                Text(text = "下一步")
            }
        }, title = {
            Text(
                text = when (_step) {
                    0 -> "选择日期"
                    1 -> "选择楼层"
                    2 -> "选择起止时间"
                    else -> ""
                }
            )
        }, text = {
            AnimatedContent(targetState = _step, label = "step", transitionSpec = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left).togetherWith(
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
                )
            }) { step ->
                when (step) {
                    0 -> {
                        DatePicker(state = dateState)
                    }

                    1 -> {
                        LazyColumn {
                            items(Rooms.entries) {
                                ListItem(headlineContent = { Text(text = it.title) }, leadingContent = {
                                    if (room == it) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                            contentDescription = ""
                                        )
                                    }
                                }, modifier = Modifier.clickable {
                                    room = it
                                })
                            }
                        }
                    }

                    2 -> {
                        // 预约允许时间为6:00-22:00，其中共16小时-->960
                        var value by remember(timeStartState, timeEndState) {
                            mutableStateOf(timeStartState.toFloat()..timeEndState.toFloat())
                        }
                        LaunchedEffect(key1 = value, block = {
                            var newStart = value.start.toInt()
                            var newEnd = value.endInclusive.toInt()

                            if (newStart >= 900) {
                                newStart = 900
                                newEnd = 959
                                timeStartState = newStart
                                timeEndState = newEnd
                                return@LaunchedEffect
                            }
                            if (newEnd - newStart < 60) {
                                newEnd = newStart + 60
                            }

                            timeStartState = newStart
                            timeEndState = newEnd
                        })
                        Column {
                            RangeSlider(
                                value = value,
                                steps = 960,
                                onValueChange = { range -> value = range },
                                valueRange = 0f..960f
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val start = remember(timeStartState) {
                                    LocalTime.of(6, 0).plusMinutes(timeStartState.toLong()).toString()
                                }
                                val end = remember(timeEndState) {
                                    LocalTime.of(6, 0).plusMinutes(timeEndState.toLong()).toString()
                                }
                                Text(text = start)
                                Text(text = end)
                            }
                        }
                    }

                    3 -> {
                        LaunchedEffect(key1 = Unit, block = {
                            _step = 0
                            dialog = false
                        })
                    }
                }
            }
        })
    }
}