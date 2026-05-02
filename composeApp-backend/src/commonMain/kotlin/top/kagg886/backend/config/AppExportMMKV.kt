package top.kagg886.backend.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.serializersModuleOf
import top.kagg886.mkmb.MMKV
import top.kagg886.mkmb.MMKVMode
import top.kagg886.mkmb.mmkvWithID
import top.kagg886.sylu_eoa.api.v2.bean.ExamExportOptions
import top.kagg886.sylu_eoa.api.v2.bean.ExamExportOptions.SelectColumn
import top.kagg886.util.json

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/10/29 20:41
 * ================================================
 */

val ALL_SELECT_COLUMNS = listOf(
    // 核心身份信息
    SelectColumn.XH(),        // 学号
    SelectColumn.XM(),        // 姓名

    // 核心成绩信息
    SelectColumn.KCMC(),      // 课程名称
    SelectColumn.CJ(),        // 成绩
    SelectColumn.XF(),        // 学分
    SelectColumn.JD(),        // 绩点
    SelectColumn.XFJD(),      // 学分绩点

    // 时间定位信息
    SelectColumn.XNMMC(),     // 学年名称
    SelectColumn.XQMMC(),     // 学期名称

    // 课程基本信息
    SelectColumn.KCH(),       // 课程号
    SelectColumn.KCXZMC(),    // 课程性质名称
    SelectColumn.KCLBMC(),    // 课程类别名称
    SelectColumn.KCGSMC(),    // 课程归属名称

    // 教学组织信息
    SelectColumn.JXBMC(),     // 教学班名称
    SelectColumn.JSXM(),      // 教师姓名
    SelectColumn.XSBJMC(),    // 学生班级名称

    // 辅助说明信息
    SelectColumn.KSXZ(),      // 考试性质
    SelectColumn.KHFMC(),     // 考核方式名称
    SelectColumn.CJBZ(),      // 成绩备注
    SelectColumn.CJSFZF(),    // 成绩是否作废
    SelectColumn.SFXWKC(),    // 是否学位课程
    SelectColumn.KKBBMC(),    // 开课班别名称
    SelectColumn.KCBJ(),      // 课程备注
)

object AppExportMMKV : MMKV by MMKV.mmkvWithID("class-export-settings", mode = MMKVMode.MULTI_PROCESS), AppExportMMKVType {
    override var columns: List<SelectColumn> by json("columns", ALL_SELECT_COLUMNS)
    override var selected: Set<SelectColumn> by json("selected", ALL_SELECT_COLUMNS.take(9).toSet())
    override fun reset() {
        columns = ALL_SELECT_COLUMNS
        selected = ALL_SELECT_COLUMNS.take(9).toSet()
    }
}

interface AppExportMMKVType {
    var columns: List<SelectColumn>
    var selected: Set<SelectColumn>


    fun reset()
}
