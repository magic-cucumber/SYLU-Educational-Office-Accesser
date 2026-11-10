package top.kagg886.eoa.gym

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.kagg886.eoa.gym.internal.NotStandardISOInstantSerializer
import kotlin.time.Instant

@Serializable
internal data class LoginRequest(
    val username: String,
    val password: String,
    @SerialName("sys_id")
    val sysId: String = "iscpMobile",
    val nonceStr: String = "",
    val captchaValue: String = "",
)

@Serializable
internal data class ListRequest(
    @SerialName("user_id")
    val userId: String
)

@Serializable
internal data class ListResponse(
    @SerialName("data_arr")
    val data: List<GymSummary>
)

@Serializable
data class GymSummary(
    @SerialName("school_year")
    val id: String,
    @SerialName("total")
    val score: Float,
    @SerialName("nums")
    val all: Int,
    val done: Int,
)

@Serializable
internal data class DetailRequest(
    @SerialName("user_id")
    val userId: String,
    @SerialName("school_year")
    val id: String,
)

@Serializable
internal data class DetailResponse(
    val data: DetailResponseInner
)

@Serializable
internal data class DetailResponseInner(
    @SerialName("data_arr")
    val data: List<GymDetail>
)


/**
 * {
 *                 "subject_id": "356c2c193a814c418b2b6a0d55794762",
 *                 "result": "15.4", //数据
 *                 "score": 76, //评分
 *                 "unit": "cm", //单位
 *                 "sub_name": "坐位体前屈",
 *                 "sub_ratio": "10",
 *                 "grade": "及格", //评级
 *                 "is_join": 1, //1为参加
 *                 "icon": "flexion.png",
 *                 "source_type": "0",
 *                 "school_year": "2025",
 *                 "source_code": "zwtqq"
 *}
 */
@Serializable
data class GymDetail(
    /** id */
    @SerialName("subject_id")
    val id: String,
    /** 测试数据。 */
    val result: String,
    /** 评分。 */
    val score: Float,
    /** 单位。 */
    val unit: String,
    /** 评级。 */
    val grade: String,
    /** 比例。 */
    @SerialName("sub_ratio")
    val ratio: String,
    /** 1 表示参加。 */
    @SerialName("is_join")
    val isJoin: Int,
)

@Serializable
data class HistoryRequest(
    @SerialName("user_id")
    val userId: String,
    @SerialName("school_year")
    val listId: String,
    @SerialName("subject_id")
    val detailId: String,

    @SerialName("page_num")
    val pageNum: Int = 1,
    @SerialName("page_size")
    val pageSize: Int = 10,
)

@Serializable
internal data class HistoryResponseOuter(
    val data: HistoryResponse
)

@Serializable
data class HistoryResponse(
    @SerialName("data_list")
    val data: List<GymDetailHistory>,

    @SerialName("total_rows")
    val total: Int,
)


/**
 * {
 *                 "result": "15.4", //测量的字面量
 *                 "session_name": "25-26上学期",
 *                 "score_time": "2025-05-17 10:50:32", //录入时间
 *                 "sourceScoreId": "473e8f1aa7794b36be0b960acbfc01ed",
 *                 "sub_name": "坐位体前屈",
 *                 "score_status": "2" //2为已采用
 * }
 */
@Serializable
data class GymDetailHistory(
    /** 测量的字面量。 */
    val result: String,
    /** 录入时间。 */
    @SerialName("score_time")
    @Serializable(with = NotStandardISOInstantSerializer::class)
    val scoreTime: Instant,
    /** "2" 表示已采用。 */
    @SerialName("score_status")
    val scoreStatus: String,
)
