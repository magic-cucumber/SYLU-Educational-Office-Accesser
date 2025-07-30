package top.kagg886.sylu_eoa.api.v2.bean

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class ExamItem(
    @SerialName("xnm")
    val year: String,
    @SerialName("xqm")
    val semester: String,

    @SerialName("kch_id")
    val courseID: String,

    @SerialName("jxb_id")
    val detailsID: String,

//================下面是有用的信息================//
    @SerialName("kcmc")
    val name: String, //课程名称

    @SerialName("jsxm")
    val teacher: String, //老师名字

    @SerialName("xf")
    val credit: String, //学分

    @SerialName("jd")
    val gradePoint: String, //绩点

    @SerialName("xfjd")
    val crTimesGp: String, //学分*绩点

    @SerialName("bfzcj")
    val absoluteScore: String, //考试绝对分数

    @SerialName("cj")
    val relateScore: String, //评级

    @SerialName("ksxzdm")
    val completionCode: String, //挂科标识

    @SerialName("sfxwkc")
    private val _degreeProgram: String, //是否是学位课

    @SerialName("tjsj")
    @Serializable(with = SubmitTimeSerializer::class)
    val submitTime: LocalDateTime, //成绩被提交的时间
) {
    val degreeProgram = _degreeProgram == "是"

    val examStatus by lazy {
        if (absoluteScore.toDouble().compareTo(60) == -1) {
            return@lazy ExamStatus.FAILED
        } else {
            val ksxzdm = completionCode.toInt()
            if (ksxzdm == 11 || ksxzdm == 16 || ksxzdm == 17) {
                return@lazy ExamStatus.RE_SUCCESS
            }
        }
        ExamStatus.SUCCESS
    }


    private object SubmitTimeSerializer : KSerializer<LocalDateTime> {

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor(LocalDateTime::class.qualifiedName!!, PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): LocalDateTime =
            LocalDateTime.parse(decoder.decodeString(), ChinaDateFormater)

        override fun serialize(encoder: Encoder, value: LocalDateTime) {
            encoder.encodeString(value.format(ChinaDateFormater))
        }

    }
}

enum class ExamStatus {
    SUCCESS, FAILED, RE_SUCCESS
}

fun List<ExamItem>.findListByTerm(picker: TermPicker): List<ExamItem> {
    return filter {
        val xnm = picker.asTerm().xnm
        val xqm = picker.asTerm().xqm

        if (xqm.isEmpty() && xnm.isEmpty()) {
            return@filter true
        }

        if (xnm.isEmpty()) {
            return@filter it.semester == xqm
        }

        if (xqm.isEmpty()) {
            return@filter it.year == xnm
        }

        return@filter it.year == xnm && it.semester == xqm

    }
}

private val ChinaDateFormater = LocalDateTime.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    dayOfMonth()
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

