package top.kagg886.sylu_eoa.api.v2.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GPAScoreSummary(
    @SerialName("xmlbmc")
    val name: String,

    @SerialName("hdfz")
    val score: Double = 0.0
)
