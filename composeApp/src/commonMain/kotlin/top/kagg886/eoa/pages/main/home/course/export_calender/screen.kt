package top.kagg886.eoa.pages.main.home.course.export_calender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.calendar.v2.rememberCalendarManagerState
import top.kagg886.calendar.v2.state.CalendarState
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.ErrorPage
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.showSnackBar

@Serializable
data object CourseExportCalenderRoute

@Composable
fun CourseExportCalenderScreen() {
    val mainModel = mainViewModel()
    val model = viewModel { CourseExportCalenderModel(mainModel.database) }
    val snack = LocalSnackBarHost.current
    val nav = LocalNavController.current
    model.collectSideEffect {
        when (it) {
            is CourseExportCalenderSideEffect.NavigateBack -> {
                snack.showSnackBar(title = it.msg, type = it.type)
                nav.popBackStack()
            }
        }
    }
    val state by model.collectAsState()

    DialogPageScaffold(
        title = { Text("导出到日历") },
        icon = { Icon(Icons.Default.ImportExport, "") },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = { nav.popBackStack() },
            ) {
                Text("取消")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            when (val manager = rememberCalendarManagerState()) {
                is CalendarState.Waiting -> Loading("正在准备申请权限...")
                is CalendarState.Processing -> Loading(msg = "正在申请权限...")
                is CalendarState.Granted -> {
                    LaunchedEffect(Unit) {
                        model.exportCalender(manager.manager)
                    }
                    Loading(state.message)
                }

                is CalendarState.Denied -> ErrorPage(
                    title = {
                        Text("日历权限未被授予")
                    },
                    message = {
                        Text("请前往系统设置赋予日历权限")
                    }
                )

                is CalendarState.NotSupported -> ErrorPage(
                    title = {
                        Text("错误")
                    },
                    message = {
                        Text("当前平台不支持日历导出功能")
                    }
                )
            }
        }
    }

}

@Composable
private fun Loading(msg: String) {
    CircularProgressIndicator()
    Spacer(Modifier.height(16.dp))
    Text(msg)
}
