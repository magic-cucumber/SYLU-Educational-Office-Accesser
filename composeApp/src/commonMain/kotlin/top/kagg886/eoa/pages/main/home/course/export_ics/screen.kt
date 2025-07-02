package top.kagg886.eoa.pages.main.home.course.export_ics

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.LocalSnackBarHost
import top.kagg886.eoa.component.dialog.DialogPageScaffold
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.showSnackBar

@Serializable
data object CourseExportIcsRoute

@Composable
fun CourseExportIcsScreen() {
    val mainModel = mainViewModel()
    val model = viewModel { CourseExportIcsModel(mainModel.database) }
    val snack = LocalSnackBarHost.current
    val nav = LocalNavController.current
    model.collectSideEffect {
        when (it) {
            is CourseIcsExportSideEffect.NavigateBack -> {
                snack.showSnackBar(title = it.msg, type = it.type)
                nav.popBackStack()
            }
        }
    }
    val state by model.collectAsState()

    DialogPageScaffold(
        title = { Text("导出课程表") },
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
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(state.message)
        }
    }
}
