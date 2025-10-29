package top.kagg886.sylu_eoa.api.v2.bean

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import top.kagg886.sylu_eoa.api.v2.bean.ExamExportOptions.SelectColumn

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/10/16 13:49
 * ================================================
 */

/**
 * # HTML表单POST请求解析
 *
 * ## 请求基本信息
 * - **请求方法**: POST
 * - **目标URL**: `/cjcx/cjcx_dcListByXs.html`
 * - **编码类型**: `application/x-www-form-urlencoded; charset=UTF-8`
 * - **目标窗口**: `_blank` (新窗口打开)
 *
 * ## 请求参数
 *
 * ### 基本参数
 * - `gnmkdmKey`: N305005
 * - `sessionUserKey`: 2203050528
 * - `xnm`: 2025 (学年)
 * - `xqm`: 3 (学期)
 * - `sfzgcj`: (空值)
 * - `kcbj`: (空值)
 * - `dcclbh`: JW_N305005_XSCXCJ
 * - `queryModel.sortName`: (空值)
 * - `queryModel.sortOrder`: asc
 * - `exportModel.exportWjgs`: xls
 *
 * ### 导出列配置 (exportModel.selectCol)
 * 表单定义了23个导出列，每个列格式为：`字段名@列名@宽度`
 *
 * 1. `kch@课程代码@120`
 * 2. `kcmc@课程名称@120`
 * 3. `xf@学分@50`
 * 4. `xnmmc@学年@90`
 * 5. `jd@绩点@50`
 * 6. `kcxzmc@课程性质@100`
 * 7. `xqmmc@学期@50`
 * 8. `cjbz@成绩备注@100`
 * 9. `ksxz@成绩性质@100`
 * 10. `sfxwkc@是否学位课程@150`
 * 11. `kkbmmc@开课学院@120`
 * 12. `kcbj@课程标记@100`
 * 13. `kclbmc@课程类别@100`
 * 14. `kcgsmc@课程归属@100`
 * 15. `jxbmc@教学班@120`
 * 16. `jsxm@任课教师@120`
 * 17. `khfsmc@考核方式@100`
 * 18. `xh@学号@120`
 * 19. `xm@姓名@80`
 * 20. `xsbjmc@学生标记@120`
 * 21. `cj@成绩@50`
 * 22. `cjsfzf@是否成绩作废@80`
 * 23. `xfjd@学分绩点@80`
 *
 * ## 功能分析
 * 这个表单是一个**学生成绩查询和导出**功能：
 * - 查询2025年第3学期的成绩数据
 * - 用户ID为2203050528
 * - 导出格式为Excel (.xls)
 * - 包含学生基本信息、课程信息、成绩信息等23个字段
 * - 按升序排列
 *
 * 当提交这个表单时，系统会在新窗口中打开，并返回一个包含学生成绩详情的Excel文件。
 */

data class ExamExportOptions(
    val format: Format,
    val select: List<SelectColumn>,
) {
    enum class Format {
        XLS, TXT, DBF
    }

    @Serializable
    sealed interface SelectColumn {
        val remark: String
        val width: Int

        @Serializable
        @SerialName("KCH")
        data class KCH(override val remark: String = "课程代码", override val width: Int = 120) : SelectColumn

        @Serializable
        @SerialName("KCMC")
        data class KCMC(override val remark: String = "课程名称", override val width: Int = 120) : SelectColumn

        @Serializable
        @SerialName("XF")
        data class XF(override val remark: String = "学分", override val width: Int = 50) : SelectColumn

        @Serializable
        @SerialName("XNMMC")
        data class XNMMC(override val remark: String = "学年", override val width: Int = 90) : SelectColumn

        @Serializable
        @SerialName("JD")
        data class JD(override val remark: String = "绩点", override val width: Int = 50) : SelectColumn

        @Serializable
        @SerialName("KCXZMC")
        data class KCXZMC(override val remark: String = "课程性质", override val width: Int = 100) : SelectColumn

        @Serializable
        @SerialName("XQMMC")
        data class XQMMC(override val remark: String = "学期", override val width: Int = 50) : SelectColumn

        @Serializable
        @SerialName("CJBZ")
        data class CJBZ(override val remark: String = "成绩备注", override val width: Int = 100) : SelectColumn

        @Serializable
        @SerialName("KSXZ")
        data class KSXZ(override val remark: String = "成绩性质", override val width: Int = 100) : SelectColumn

        @Serializable
        @SerialName("SFXWKC")
        data class SFXWKC(override val remark: String = "是否学位课程", override val width: Int = 150) : SelectColumn

        @Serializable
        @SerialName("KKBBMC")
        data class KKBBMC(override val remark: String = "开课学院", override val width: Int = 120) : SelectColumn

        @Serializable
        @SerialName("KCBJ")
        data class KCBJ(override val remark: String = "课程标记", override val width: Int = 100) : SelectColumn

        @Serializable
        @SerialName("KCLBMC")
        data class KCLBMC(override val remark: String = "课程类别", override val width: Int = 100) : SelectColumn

        @Serializable
        @SerialName("KCGSMC")
        data class KCGSMC(override val remark: String = "课程归属", override val width: Int = 100) : SelectColumn

        @Serializable
        @SerialName("JXBMC")
        data class JXBMC(override val remark: String = "教学班", override val width: Int = 120) : SelectColumn

        @Serializable
        @SerialName("JSXM")
        data class JSXM(override val remark: String = "任课教师", override val width: Int = 120) : SelectColumn

        @Serializable
        @SerialName("KHFMC")
        data class KHFMC(override val remark: String = "考核方式", override val width: Int = 100) : SelectColumn

        @Serializable
        @SerialName("XH")
        data class XH(override val remark: String = "学号", override val width: Int = 120) : SelectColumn

        @Serializable
        @SerialName("XM")
        data class XM(override val remark: String = "姓名", override val width: Int = 80) : SelectColumn

        @Serializable
        @SerialName("XSBJMC")
        data class XSBJMC(override val remark: String = "学生标记", override val width: Int = 120) : SelectColumn

        @Serializable
        @SerialName("CJ")
        data class CJ(override val remark: String = "成绩", override val width: Int = 50) : SelectColumn

        @Serializable
        @SerialName("CJSFZF")
        data class CJSFZF(override val remark: String = "是否成绩作废", override val width: Int = 80) : SelectColumn

        @Serializable
        @SerialName("XFJD")
        data class XFJD(override val remark: String = "学分绩点", override val width: Int = 80) : SelectColumn
    }}

// 扩展函数：获取字段名
fun SelectColumn.getFieldName(): String = this::class.simpleName!!.lowercase()

// 扩展函数：生成表单参数值
fun SelectColumn.toFormValue(): String = "${getFieldName()}@${remark}@${width}"
