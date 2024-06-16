package com.kagg886.sylu_eoa.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.kagg886.sylu_eoa.R
import com.kagg886.sylu_eoa.screen.LocalNavController
import com.kagg886.sylu_eoa.screen.LocalTopBar
import com.kagg886.sylu_eoa.screen.page.*

object PageConfig {
    val nav = listOf(
        PageItem("首页", R.drawable.outline_apps_24, "MainPage") @Composable { MainPage() },
        PageItem("课程表", R.drawable.baseline_calendar_month_24, "ClassTablePage") @Composable { ClassTablePage() },
        PageItem("考试", R.drawable.baseline_check_24, "ExamPage") @Composable { ExamPage() },
        PageItem("我", R.drawable.baseline_cyclone_24, "MePage") @Composable { MePage() },
    )

    val allPage: List<PageItem> = mutableListOf<PageItem>().apply {
        addAll(nav)
        add(PageItem("第二课堂", 0, "SecondClass") @Composable { BackTopBar("第二课堂");SecondClassPage() })
        add(PageItem("工具", 0, "ToolPage") @Composable { BackTopBar("工具");ToolPage() })
        add(PageItem("设置", 0, "SettingPage") @Composable { BackTopBar("设置");SettingPage() })
        add(PageItem("关于", 0, "AboutPage") @Composable { BackTopBar("关于");AboutPage() })
        add(PageItem("图书馆工具", 0, "LibraryReverser") @Composable { LibraryPage() })
        add(PageItem("全部课程", 0, "ClassAllPage") @Composable { ClassAllPage() })
    }

    const val DEFAULT_ROUTER = "MainPage"

}

fun List<PageItem>.contains(s: String): Boolean {
    return any { it.router == s }
}

data class PageItem(val title: String, val icon: Int = 0, val router: String, val widget: @Composable () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackTopBar(text: String = "") {
    val nav = LocalNavController.current
    var top by LocalTopBar.current
    LaunchedEffect(key1 = Unit, block = {
        top = {
            TopAppBar(title = { Text(text = text) }, navigationIcon = {
                IconButton(onClick = {
                    nav.popBackStack()
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "")
                }
            })
        }
    })
}