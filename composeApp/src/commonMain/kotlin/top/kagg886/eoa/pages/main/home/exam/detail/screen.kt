package top.kagg886.eoa.pages.main.home.exam.detail

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute

@Serializable
data class ExamDetailRoute(val examId: Long)

@Composable
fun ExamDetailScreen(route: ExamDetailRoute) = HomeScreen(NavigationRoute.EXAM) {
    Text("Exam")
}
