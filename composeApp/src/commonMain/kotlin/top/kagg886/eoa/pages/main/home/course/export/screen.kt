package top.kagg886.eoa.pages.main.home.course.export

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import top.kagg886.eoa.pages.main.mainViewModel
import top.kagg886.eoa.util.showSnackBar

@Serializable
data object CourseExportRoute

@Composable
fun CourseExportScreen() {
    val mainModel = mainViewModel()
    val model = viewModel { CourseExportModel(mainModel.database) }
    val snack = LocalSnackBarHost.current
    val nav = LocalNavController.current
    model.collectSideEffect {
        when (it) {
            is CourseExportSideEffect.NavigateBack -> {
                snack.showSnackBar(title = it.msg, type = it.type)
                nav.popBackStack()
            }
        }
    }
    val state by model.collectAsState()
    Surface(Modifier.fillMaxSize(0.8f)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(state.message)
            Spacer(Modifier.weight(1f))
        }
    }
}