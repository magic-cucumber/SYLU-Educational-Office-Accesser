package top.kagg886.eoa.pages.main.home.course.export_calender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import top.kagg886.calender.data.CalenderPermissionGrantType.*
import top.kagg886.calender.rememberCalenderState
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.ErrorPage
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
    Surface(Modifier.fillMaxSize(0.8f)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            val manager = rememberCalenderState(name = "eoa-calender")


            when(manager.permission) {
                WAIT -> Loading("正在准备申请权限...")
                PROCESSING -> Loading(msg = "正在申请权限...")
                ALL_GRANTED -> {
                    LaunchedEffect(Unit) {
                        model.exportCalender(manager.events)
                    }
                    Loading(state.message)
                }
                DENY_PERMANENT,DENY_ONCE -> ErrorPage(
                    title = {
                        Text("日历权限未被授予")
                    },
                    message = {
                        Text("请前往系统设置赋予日历权限")
                    }
                )
                NOT_SUPPORTED -> ErrorPage(
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
private fun ColumnScope.Loading(msg: String) {
    Spacer(Modifier.weight(1f))
    CircularProgressIndicator()
    Spacer(Modifier.height(16.dp))
    Text(msg)
    Spacer(Modifier.weight(1f))
}
