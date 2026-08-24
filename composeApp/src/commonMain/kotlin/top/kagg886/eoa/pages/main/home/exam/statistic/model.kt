package top.kagg886.eoa.pages.main.home.exam.statistic

import top.kagg886.eoa.util.BaseViewModel
import org.orbitmvi.orbit.syntax.Syntax
import top.kagg886.backend.database.AppDatabase

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/6 15:01
 * ================================================
 */

class ExamStatisticModel(database: AppDatabase, private val year: String, private val term: String) :
    BaseViewModel<ExamStatisticState, ExamStatisticSideEffect>(name = "ExamStatisticModel", initial = ExamStatisticState.Loading) {
    private val examDao = database.examDao()
    override suspend fun Syntax<ExamStatisticState, ExamStatisticSideEffect>.init() {
            val currentTermData = examDao.all(yearCode = year, xqmCode = term)
            val allData = examDao.all()

            // 提取分母变量，避免重复计算
            val allCourseCount = allData.groupBy { it.courseID }.count()
            val currentTermCourseCount = currentTermData.groupBy { it.courseID }.count()

            // 拆分计算逻辑为六个变量
            val allAvgScore = if (allData.isEmpty()) 0.0 else allData.sumOf { it.credit } / allCourseCount
            val allAvgPoint = if (allData.isEmpty()) 0.0 else allData.sumOf { it.gradePoint } / allCourseCount
            val allAvgScoreMultiPoint = if (allData.isEmpty()) 0.0 else (allAvgScore + allAvgPoint) / 2

            val avgScore = if (currentTermData.isEmpty()) null else currentTermData.sumOf { it.credit } / currentTermCourseCount
            val avgPoint = if (currentTermData.isEmpty()) null else currentTermData.sumOf { it.gradePoint } / currentTermCourseCount
            val avgScoreMultiPoint = if (currentTermData.isEmpty()) null else (avgScore!! + avgPoint!!) / 2

            reduce {
                ExamStatisticState.Success(
                    allAvgScore = allAvgScore,
                    allAvgPoint = allAvgPoint,
                    allAvgScoreMultiPoint = allAvgScoreMultiPoint,
                    avgScore = avgScore,
                    avgPoint = avgPoint,
                    avgScoreMultiPoint = avgScoreMultiPoint,
                )
            }
    }
}


sealed interface ExamStatisticState {
    data object Loading : ExamStatisticState

    data class Success(
        val allAvgScore: Double,
        val allAvgPoint: Double,
        val allAvgScoreMultiPoint: Double,


        val avgScore: Double?,
        val avgPoint: Double?,
        val avgScoreMultiPoint: Double?,
    ) : ExamStatisticState
}

sealed interface ExamStatisticSideEffect {

}
