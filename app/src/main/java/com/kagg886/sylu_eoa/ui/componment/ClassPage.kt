package com.kagg886.sylu_eoa.ui.componment

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kagg886.sylu_eoa.api.v2.bean.ClassUnit
import com.kagg886.sylu_eoa.screen.page.getTime
import com.kagg886.sylu_eoa.ui.theme.Typography
import net.fortuna.ical4j.model.Dur
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.absoluteValue

@Composable
fun ClassPage(date: LocalDate, list: List<ClassUnit>,openFunc: (Offset, IntSize, ClassUnit) -> Unit) {
    if (list.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Outlined.Clear, contentDescription = "")
                Text(text = "本周无课程!", style = Typography.labelLarge)
            }
        }
        return
    }
    val iconColor = if (isSystemInDarkTheme()) {
        Color.White
    } else {
        Color.Gray
    }
    val perHeight = 110
    Column {
        Row {
            Text(
                text = "${date.month.value}\n月",
                modifier = Modifier.width(38.dp),
                fontSize = 10.sp, textAlign = TextAlign.Center,
                color = iconColor
            )

            (0..6).toList().forEach {
                val date = date.plusDays(it.toLong())
                val textText = "${date.monthValue}-${date.dayOfMonth}"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1F, true)
                        .height(40.dp),
                    color = Color.Transparent
                ) {
                    if (date != LocalDate.now()) {
                        Text(
                            text = textText,
                            modifier = Modifier.background(Color.Transparent),
                            fontSize = 11.sp,
                            lineHeight = 10.sp,
                            textAlign = TextAlign.Center,
                            color = iconColor
                        )
                    } else {
                        Text(
                            text = textText,
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Blue,
                                            Color.Transparent
                                        )
                                    )
                                ),
                            fontSize = 11.sp,
                            lineHeight = 10.sp,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                var time = LocalTime.of(8, 0)
                // 时间列
                (1..6).toList().forEach { i ->
                    Text(
                        buildAnnotatedString {
                            withStyle(style = ParagraphStyle(lineHeight = 6.sp)) {
                                withStyle(
                                    style = SpanStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iconColor
                                    )
                                ) {
                                    append("\n\n${i * 2 - 1}-${i * 2}")
                                }
                            }
                            withStyle(
                                style = SpanStyle(
                                    fontSize = 10.sp,
                                    color = iconColor,
                                    fontWeight = FontWeight.Medium
                                )
                            ) {
                                withStyle(style = ParagraphStyle(lineHeight = 10.sp)) {
                                    append(time.toString()) //8:00
                                    time = time.plusMinutes(45)
                                    append("\n-\n")
                                    append(time.toString()) //8:45
                                    append("\n")
                                    time = time.plusMinutes(10)
                                    append("\n")
                                    append(time.toString()) //8:55
                                    time = time.plusMinutes(45)
                                    append("\n-\n")
                                    append(time.toString()) //9:30
                                    time = time.plusMinutes(20)
                                    if (time.hour == 12) {
                                        time = LocalTime.of(13, 0)
                                    }
                                    if (time.hour == 18) {
                                        time = LocalTime.of(19, 0)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .height(perHeight.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            //课表列，一列高 perHeight dp

            //礼拜一到礼拜天
            for (i in 1..7) {
                val list = list.filter { i == it.dayInWeek.toInt() } //筛选出该天的课程
                var empty = 0
                Column(Modifier.weight(0.6F)) {
                    for (j in 1..6) {
                        //搜索2*j-1到2*j节课的课程。
                        //无课程empty+1
                        //有课程添加宽empty*perHeight的Space后添加课程条目，同时empty清零
                        val list = list.filter { it.rangeEveryDay.contains(2 * j - 1) }

                        when {
                            list.isEmpty() -> {
                                empty++
                            }

                            list.size == 1 -> {
                                if (empty != 0) {
                                    Spacer(modifier = Modifier.height((perHeight * empty).dp))
                                }
                                ClassItem(unit = list[0], height = perHeight) { offset, intSize ->
                                    openFunc(offset,intSize,list[0])
                                }

                                empty = 0
                            }

                            else -> {
                                Spacer(modifier = Modifier.height((perHeight * empty).dp))
                                ConflictItem(unit = list, height = perHeight)
                                empty = 0
                            }
                        }
                    }
                }
            }
        }
    }


}

@Composable
fun ClassItem(unit: ClassUnit, height: Int,openFunc: (Offset, IntSize) -> Unit = {_,_->}) {
    var offset by remember {
        mutableStateOf(Offset.Zero)
    }
    var size by remember {
        mutableStateOf(IntSize.Zero)
    }
    Box(modifier = Modifier
        .onGloballyPositioned {
            offset = it.positionInRoot()
            size = it.size
        }
        .clickable {openFunc(offset,size)}
        .height(height = height.dp)
        .padding(3.dp)
    ) {
        val (start,end) = getTime(unit)
        val timeAll = Duration.between(start,end).toMinutes().absoluteValue
        val now = LocalTime.now()
        if (now > start && now < end) {
            HorizontalDivider(modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(
                        x = 0,
                        y = (Duration
                            .between(now, start)
                            .toMinutes().absoluteValue / timeAll * height).toInt()
                    )
                })
        }

        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = Color(unit.name.hashCode())
            )
        ) {
            Column {
                Text(
                    unit.name, style = Typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    color = if (unit.isDegreeProgram) Color.Red else Color.Unspecified
                )
                Text(unit.room, style = Typography.bodySmall)
            }
        }
    }
}

@Preview
@Composable
private fun ClassItemPreview() {
    ClassItem(
        unit = ClassUnit(
            name = "1",
            teacher = "2",
            room = "room",
            weekEachLesson = "1",
            lesson = "1",
            dayInWeek = "1",
            score = "a",
            classType = "1",
            _degreeProgram = "sdsd"
        ),
        height = 110
    )
}

@Composable
fun Details(k: String, v: String) {
    ListItem(headlineContent = {
        Text(v)
    }, leadingContent = {
        Text(k)
    })
}

@Composable
fun ConflictItem(unit: List<ClassUnit>, height: Int) {
    var dialog by remember {
        mutableStateOf(false)
    }

    if (dialog) {
        AlertDialog(
            onDismissRequest = { dialog = false }, confirmButton = { },
            title = {
                Text(text = "冲突课程查看")
            }, text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    unit.forEach {
                        ClassItem(unit = it, height = height)
                    }
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .height(height = height.dp)
            .padding(3.dp)
            .clickable {
                dialog = true
            }, colors = CardDefaults.cardColors(
            containerColor = Color("冲突课程".hashCode())
        )
    ) {
        Text("冲突课程\n点我查看", style = Typography.bodyMedium)
    }
}