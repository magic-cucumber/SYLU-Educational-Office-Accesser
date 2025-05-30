package top.kagg886.eoa.pages.main.home.exam.list

import top.kagg886.sylu_eoa.api.v2.bean.ExamStatus

enum class PassFilter {
    ALL, //全部
    PASS, //过
    NOT_PASS, //挂
    RE_PASS, //补
}

enum class DegreeFilter {
    ALL, //全部
    ONLY_DEGREE, //仅学位课
    NO_DEGREE, //仅非学位课
}


fun PassFilter.toExamStatus():ExamStatus? = when(this) {
    PassFilter.ALL -> null
    PassFilter.PASS -> ExamStatus.SUCCESS
    PassFilter.NOT_PASS -> ExamStatus.FAILED
    PassFilter.RE_PASS -> ExamStatus.RE_SUCCESS
}

fun DegreeFilter.toQuery():Boolean? = when(this) {
    DegreeFilter.ALL -> null
    DegreeFilter.ONLY_DEGREE -> true
    DegreeFilter.NO_DEGREE -> false
}