package top.kagg886.eoa.pages.main.home.exam.list

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import kotlinx.serialization.Serializable
import top.kagg886.eoa.component.nav.transition
import top.kagg886.eoa.pages.main.home.exam.list.content.ExamListContentRoute
import top.kagg886.eoa.pages.main.home.exam.list.content.ExamListContentScreen
import top.kagg886.eoa.pages.main.home.exam.list.filter.ExamListFilterRoute
import top.kagg886.eoa.pages.main.home.exam.list.filter.ExamListFilterScreen

/**
 * ================================================
 * Author:     iveou
 * Created on: 2026/8/6 15:31
 * ================================================
 */

@Serializable
data object ExamListRoute

val installExamListRoute: NavGraphBuilder.() -> Unit = {
    transition<ExamListContentRoute> { ExamListContentScreen() }
    dialog<ExamListFilterRoute> { ExamListFilterScreen() }
}
