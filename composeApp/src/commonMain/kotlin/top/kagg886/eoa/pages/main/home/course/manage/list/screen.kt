package top.kagg886.eoa.pages.main.home.course.manage.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import top.kagg886.eoa.component.BackIconButton
import top.kagg886.eoa.pages.main.home.HomeScreen
import top.kagg886.eoa.pages.main.home.NavigationRoute
import top.kagg886.eoa.util.shared.LocalAnimatedContentScope
import top.kagg886.eoa.util.shared.rememberSharedContentState
import top.kagg886.eoa.util.shared.shareElementComposed

@Serializable
data object CourseManageListRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CourseManageListScreen() = HomeScreen(
    route = NavigationRoute.COURSE,
    back = { BackIconButton() },
    title = { Text("管理课程") },
) {
    Surface(
        Modifier.fillMaxSize().shareElementComposed(
            sharedContentState = rememberSharedContentState(key = "list-course-to-manage-course"),
            animatedVisibilityScope = LocalAnimatedContentScope.current
        )
    ) {

    }
}