package com.kagg886.sylu_eoa.api.seats.util

import com.kagg886.sylu_eoa.api.seats.bean.Seat
import com.kagg886.sylu_eoa.api.seats.bean.SeatUsage

fun List<Seat>.filterSeats(config: SeatUsage): List<Seat> {
    return this.filter {
        val u = it.usage
        if (u.isEmpty()) { //空的代表无人使用
            return@filter true
        }
        it.isCanReserve(config)
    }
}

fun List<Seat>.searchSeatByName(name:String):Seat {
    return this.first {
        it.title.contains(name)
    }
}

fun Seat.isCanReserve(config: SeatUsage):Boolean {
    var rtn = true
    for (i in this.usage) {
        //单个日期不重叠算法：   !(A.end< B.start || A.start > B.end)
        rtn = !(i.end < config.start || i.start > config.end)
        if (!rtn) {
            break
        }
    }
    return rtn
}