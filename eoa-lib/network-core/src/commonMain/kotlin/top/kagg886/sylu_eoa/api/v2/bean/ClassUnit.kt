package top.kagg886.sylu_eoa.api.v2.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClassUnit(
    //名字
    @SerialName("kcmc") val name: String,
    //老师名字
    @SerialName("xm") val teacher: String,
    //房间
    @SerialName("cdmc") val room: String,
    //第几周有课
    @SerialName("zcd") val weekEachLesson: String,
    //节数
    @SerialName("jcs") val lesson: String,
    //星期几 1 2 3 4 5 6 7
    @SerialName("xqj") val dayInWeek: String,

    //学分
    @SerialName("xf") val score: String,

    //考察形式（考查，考试）
    @SerialName("khfsmc") val classType: String,

    @SerialName("zyhxkcbj") private val _degreeProgram: String,
) {
    val isDegreeProgram by lazy {
        _degreeProgram == "是"
    }
    //1-2节
    val rangeEveryDay by lazy {
        val ls = lesson.split("-")
        ((ls[0]).toInt() ..(ls[1]).toInt()).toList()
    }

    //7周,9-11周(单),12-16周, 一定大于1。
    val rangeAllTerm by lazy {
        weekEachLesson.replace("周", "").split(",").map {
            val a = it.substring(0, it.length)
            if (!a.contains("-")) {
                return@map listOf(a.toInt())
            }
            val range = a.split("-")

            var end = range[1]
            var step = 1
            if (end.contains("(")) {
                step = 2
                end = end.substring(0, end.indexOf("("))
            }
            (range[0].toInt()..end.toInt() step step).toList()
        }.flatten()
    }
}

fun List<ClassUnit>.findClassByWeek(
    weekForSemester: Int,
): List<ClassUnit> {
    return filter {
        it.rangeAllTerm.contains(weekForSemester)
    }
}