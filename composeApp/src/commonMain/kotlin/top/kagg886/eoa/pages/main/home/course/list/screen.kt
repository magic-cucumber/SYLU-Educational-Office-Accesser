package top.kagg886.eoa.pages.main.home.course.list

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute

@Serializable
data object CourseListRoute

@Composable
fun CourseListScreen() = HomeScreen(NavigationRoute.COURSE) {
    Text("Course")
}
