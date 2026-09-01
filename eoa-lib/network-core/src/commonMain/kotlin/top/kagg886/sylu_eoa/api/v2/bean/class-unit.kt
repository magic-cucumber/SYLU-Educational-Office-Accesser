package top.kagg886.sylu_eoa.api.v2.bean

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/5 16:30
 * ================================================
 */

@Serializable
data class ClassReturn(
    val extend: List<ClassExtend>,
    val tables: List<ClassTable>
)

@Serializable
data class ClassExtend(
    val teacher: String,
    val name: String,
    val isOther: Boolean,
    val ranges: List<Int>
)

@Serializable
data class ClassTable(
    //课程名称
    val name: String,
    //老师名称
    val teacher: String,
    //教室
    val room: String,
    //学分
    val score: String,
    //考察形式（考查，考试）
    val classType: String,
    //是否为学位课
    val isDegreeProgram: Boolean,
    //上课开始时间
    val startTime: LocalDateTime,
    //上课结束时间
    val endTime: LocalDateTime,
)
