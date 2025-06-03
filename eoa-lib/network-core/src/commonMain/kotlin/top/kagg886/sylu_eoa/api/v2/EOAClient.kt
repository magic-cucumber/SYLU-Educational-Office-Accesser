package top.kagg886.sylu_eoa.api.v2

import top.kagg886.sylu_eoa.api.v2.bean.ClassUnit
import top.kagg886.sylu_eoa.api.v2.bean.ExamItem
import top.kagg886.sylu_eoa.api.v2.bean.GPAScore
import top.kagg886.sylu_eoa.api.v2.bean.GPAScoreSummary
import top.kagg886.sylu_eoa.api.v2.bean.SchoolCalender
import top.kagg886.sylu_eoa.api.v2.bean.TERM_ALL_PICKER
import top.kagg886.sylu_eoa.api.v2.bean.TermPicker
import top.kagg886.sylu_eoa.api.v2.bean.TermResult
import top.kagg886.sylu_eoa.api.v2.bean.UserProfile

/**
 * 沈阳理工大学教务系统客户端接口
 * 提供身份验证、数据获取和系统交互的方法
 * 调用流程：
 * 1. 调用 init 方法初始化存储
 * 2. 实例化客户端
 * 3. 调用 login 方法进行身份验证
 * 4. 调用其他方法获取数据
 * 5. 调用 logout 方法登出系统
 */
interface EOAClient {
    /**
     * 获取用户的个人资料信息
     * @return 包含个人信息的用户资料
     */
    suspend fun getUserProfile(): UserProfile

    /**
     * 获取学校校历信息
     * @return 包含开始和结束日期的校历
     */
    suspend fun getSchoolCalender(): SchoolCalender

    /**
     * 获取所有可用的学期
     * @return 包含所有可用学期和默认学期的结果
     */
    suspend fun getAllAvailableTerms(): TermResult

    /**
     * 获取指定学期的考试列表
     * @param picker 要获取考试的学期（默认为所有学期）
     * @return 指定学期的考试项目列表
     */
    suspend fun getExamList(picker: TermPicker = TERM_ALL_PICKER): List<ExamItem>

    /**
     * 获取特定考试的详细信息
     * @param examItem 要获取详情的考试
     * @return 包含考试详情的列表
     */
    suspend fun getExamInfo(examItem: ExamItem): List<List<String>>

    /**
     * 获取指定学期的课程表
     * @param picker 要获取课程表的学期
     * @return 课程表中的课程单元列表
     */
    suspend fun getClassTable(picker: TermPicker): List<ClassUnit>

    /**
     * 获取GPA成绩类别
     * @return 成绩类别
     */
    suspend fun getGPAScores(): List<GPAScoreSummary>

    /**
     * 获取GPA成绩列表
     * @return 成绩列表
     */
    suspend fun getGPAScoreList(summary: GPAScoreSummary): List<GPAScore>

    /**
     * 初始化存储
     * @param storage 要使用的存储实现
     * @return 初始化的存储实例
     */
    fun init(storage: Storage)


    var username: String
    var password: String

    /**
     * 使用提供的密码登录系统
     * @param username 用户名
     * @param pass 用户密码
     * @param captchaHandler 可选的验证码处理程序
     */
    suspend fun login(captchaHandler: (suspend (a: ByteArray) -> String)? = null)

    /**
     * 从系统登出
     */
    suspend fun logout()
}
