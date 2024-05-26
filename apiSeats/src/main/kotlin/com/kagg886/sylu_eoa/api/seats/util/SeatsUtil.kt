package com.kagg886.sylu_eoa.api.seats.util

import com.kagg886.sylu_eoa.api.seats.SeatManager
import com.kagg886.sylu_eoa.api.seats.bean.BaseResponse
import com.kagg886.sylu_eoa.api.v2.SyluUser
import com.kagg886.sylu_eoa.api.v2.network.NetWorkClient
import com.kagg886.sylu_eoa.api.v2.network.asFormBody
import com.kagg886.sylu_eoa.api.v2.network.asJSONBean
import java.net.URLEncoder

suspend fun SyluUser.getSeatsManager(): SeatManager {
    val pass = with(getUserProfile().id) { //身份证后六位
        this.substring(this.length - 6)
    }
    val net = NetWorkClient("https://seats.sylu.edu.cn/ClientWeb/pro/ajax/", serializer)
    val resp = net.execute("login.aspx") {
        this.method(
            "POST", mapOf(
                "id" to this@getSeatsManager.user,
                "pwd" to pass,
                "act" to "login"
            ).asFormBody()
        )
    }.asJSONBean<BaseResponse<Unit>>()
    check(resp.isSuccess) {
        resp.msg
    }
    return SeatManager(net)
}

fun StringBuilder.appendURLParam(k: String, v: String): StringBuilder {
    val v = URLEncoder.encode(v, "UTF-8")
    if (last() == '&') {
        return append("$k=$v&")
    }
    return append("?$k=$v&")
}

fun String.urlDone(): String {
    return substring(0,length - 1)
}