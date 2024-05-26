package com.kagg886.sylu_eoa.api.seats

import com.kagg886.sylu_eoa.api.seats.bean.BaseResponse
import com.kagg886.sylu_eoa.api.seats.bean.Seat
import com.kagg886.sylu_eoa.api.seats.bean.SeatQueryModel
import com.kagg886.sylu_eoa.api.seats.bean.SeatUsage
import com.kagg886.sylu_eoa.api.seats.util.appendURLParam
import com.kagg886.sylu_eoa.api.seats.util.urlDone
import com.kagg886.sylu_eoa.api.v2.network.NetWorkClient
import com.kagg886.sylu_eoa.api.v2.network.asJSONBean
import java.time.format.DateTimeFormatter

data class SeatManager(val net: NetWorkClient) {
    suspend fun reserve(seat: Seat,usage: SeatUsage) {
        //https://seats.sylu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?dialogid=
        //dev_id=100455341
        //lab_id=
        //kind_id=
        //room_id=
        //type=dev
        //prop=
        //test_id=
        //term=
        //number=
        //classkind=
        //test_name=
        //start=2024-05-26+14%3A30
        //end=2024-05-26+15%3A30
        //start_time=1430
        //end_time=1530
        //up_file=
        //memo=
        //act=set_resv
        //_=1716698785171
        val resp = net.execute(buildString {
            append("reserve.aspx")
            appendURLParam("dev_id",seat.devId.toString())
            appendURLParam("lab_id","")
            appendURLParam("kind_id","")
            appendURLParam("room_id","")
            appendURLParam("type","dev")
            appendURLParam("prop","")
            appendURLParam("test_id","")
            appendURLParam("term","")
            appendURLParam("number","")
            appendURLParam("classkind","")
            appendURLParam("test_name","")
            appendURLParam("start",usage.start.toString())
            appendURLParam("end",usage.end.toString())
            appendURLParam("start_time",usage.start.format(DateTimeFormatter.ofPattern("HHmm")))
            appendURLParam("end_time",usage.end.format(DateTimeFormatter.ofPattern("HHmm")))
            appendURLParam("up_file","")
            appendURLParam("memo","")
            appendURLParam("act","set_resv")
            appendURLParam("_",System.currentTimeMillis().toString())
        }.urlDone()).asJSONBean<BaseResponse<Unit>>()

        check(resp.isSuccess) {
            resp.msg
        }
    }

    suspend fun delReserve() {
        // https://seats.sylu.edu.cn/ClientWeb/pro/ajax/reserve.aspx?act=del_resv
        //id=107190810
        //_=1716698785174
    }

    suspend fun getSeatList(modal: SeatQueryModel): List<Seat> {
        // device.aspx?
        // byType=devcls
        //classkind=8
        //display=fp
        //md=d
        //room_id=100455322
        //purpose=
        //selectOpenAty=
        //cld_name=default
        //date=2024-05-26
        //fr_start=12%3A15
        //fr_end=15%3A15
        //act=get_rsv_sta
        //_=1716696253396
        return net.execute(buildString {
            append("device.aspx")
            appendURLParam("byType","devcls")
            appendURLParam("classkind","8")
            appendURLParam("display","fp")
            appendURLParam("md","d")
            appendURLParam("room_id",modal.roomId) //楼层标号
            appendURLParam("purpose","")
            appendURLParam("selectOpenAty","")
            appendURLParam("cld_name","default")
            appendURLParam("date",modal.date.toString()) //预约日期
            appendURLParam("fr_start",modal.startTime.withNano(0).toString()) //预约开始时间
            appendURLParam("fr_end",modal.endTime.withNano(0).toString()) //预约结束时间
            appendURLParam("act","get_rsv_sta")
            appendURLParam("_",System.currentTimeMillis().toString())
        }.urlDone()) {
            this.method("GET",null)
        }.asJSONBean<BaseResponse<List<Seat>>>().data!!
    }
}

