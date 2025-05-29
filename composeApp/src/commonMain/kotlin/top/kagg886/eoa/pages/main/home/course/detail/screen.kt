package top.kagg886.eoa.pages.main.home.course.detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import top.kagg886.eoa.LocalNavController
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed

@Serializable
data class CourseDetailRoute(val recordId: Long)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CourseDetailScreen(route: CourseDetailRoute) = HomeScreen(
    NavigationRoute.COURSE,
    title = { Text("课程详情") },
    back = {
        val nav = LocalNavController.current
        IconButton(
            onClick = {
                nav.popBackStack()
            }
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
    }
) {
    Text(
        text = route.recordId.toString(),
        modifier = Modifier.shareElementComposed(
            sharedContentState = rememberSharedContentState(key = "summary-course-to-detail-${route.recordId}"),
            animatedVisibilityScope = LocalAnimatedContentScope.current
        )
    )
}
