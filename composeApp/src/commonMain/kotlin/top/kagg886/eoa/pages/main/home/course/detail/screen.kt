package top.kagg886.eoa.pages.main.home.course.detail

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute

@Serializable
data class CourseDetailRoute(val courseId: Long)

@Composable
fun CourseDetailScreen(route: CourseDetailRoute) = HomeScreen(NavigationRoute.COURSE) {
    Text(route.courseId.toString())
}
