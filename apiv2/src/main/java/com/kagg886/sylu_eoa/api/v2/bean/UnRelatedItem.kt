package com.kagg886.sylu_eoa.api.v2.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class SubmitType(val s: String) {
    SAVE("xspj_bcXspj"),
    SUBMIT("xspj_tjXspj")
}

class RelatedQuestions(
    val source: UnRelatedItem,
    val xspfb_id:String,
    val pjzbxm_id:String,
    val pyID:String
): MutableList<RelatedQuestion> by mutableListOf()

data class RelatedQuestion(
    val desc: String,
    val choices: List<Choice>,
    var index: Int = -1,
    val zsmbmcb_id: String,
    val pfdjdmb_id: String,
    val pjzbxm_id: String,
) {
    val selected: Choice
        get() = choices[index]

    fun select(s: Choice) {
        this.index = choices.indexOf(s)
    }

    val isNoLabelMode by lazy {
        choices.isEmpty()
    }

    var labelValue : String = ""
}

@Serializable
data class Choice(
    val pfdjdmxmb: String,
    val name: String,
    val value: String,
)

@Serializable
data class UnRelatedItem(
    @SerialName("jzgmc")
    val teacher: String,
    @SerialName("jxdd")
    val room: String = "",
    @SerialName("kcmc")
    val name: String,

    @SerialName("tjzt")
    private val submitStatus: Int,

    internal val jxb_id: String,
    internal val jgh_id: String,
    internal val kch_id: String,
    internal val xsdm: String,
) {

    fun isSubmit(): Boolean {
        return submitStatus == 1
    }
}