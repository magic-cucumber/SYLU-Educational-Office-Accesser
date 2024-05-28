package com.kagg886.sylu_eoa.api.seats.bean

import LocalDateTimeAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Serializable
data class Seat(
    val title: String,
    @SerialName("ts") val usage: List<SeatUsage>,
    val freeTime: Int, //空闲时间，单位为分钟
    internal val devId: Long, //devId，用于提交预约信息
)

enum class Rooms(val title:String,val roomId: String) {
    L2_LIBRARY("二楼电子阅览室","100455322"),
    L3_CORRIDOR("三楼环廊","100455324"),
    L4_CORRIDOR("四楼环廊","100455326"),
    L5_CORRIDOR("五楼环廊","100455328"),
    L5_NORTH("五楼北区座位","100455330")
}

@Serializable
data class SeatUsage(
    @Serializable(with = LocalDateTimeAsStringSerializer::class) val start: LocalDateTime,
    @Serializable(with = LocalDateTimeAsStringSerializer::class) val end: LocalDateTime,
    val owner: String,
) {
    init {
        check(start.toLocalDate() == end.toLocalDate()) {
            "日期不可以跨域"
        }

        check(start.toLocalTime().isBefore(end.toLocalTime())) {
            "开始时间必须在终止时间之前"
        }

        check(end.toLocalTime().hour - start.toLocalTime().hour >= 1) {
            "预约时间需要超过1小时"
        }
    }

    internal constructor(start: LocalDateTime, end: LocalDateTime) : this(start, end, "")

    companion object {
        fun build(start: LocalDateTime, end: LocalDateTime = start.plusHours(1)): SeatUsage {
            return SeatUsage(start, end)
        }
    }
}

data class SeatQueryModel(
    val room:Rooms,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
) {

    val roomId: String
        get() = room.roomId


    init {
        check(startTime < endTime) {
            "开始日期不得大于结束日期!"
        }
        check(startTime >= LocalTime.of(6, 0)) {
            "开始日期不得小于六点！"
        }
        check(endTime <= LocalTime.of(22, 0)) {
            "结束日期不得大于22点"
        }
        check(date >= LocalDate.now()) {
            "不可以查询今天以前的信息"
        }
        check(date < LocalDate.now().plusDays(2)) {
            "只可以查询3天以内的预约信息"
        }
    }
}