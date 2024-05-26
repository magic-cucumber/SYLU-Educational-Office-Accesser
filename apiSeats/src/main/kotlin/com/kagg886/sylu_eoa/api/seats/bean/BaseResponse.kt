package com.kagg886.sylu_eoa.api.seats.bean

import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    val ret: Int,
    val msg: String,
    val data: T?,
) {
    val isSuccess by lazy {
        ret == 1
    }
}