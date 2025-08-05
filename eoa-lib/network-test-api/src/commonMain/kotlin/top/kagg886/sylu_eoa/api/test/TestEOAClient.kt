package top.kagg886.sylu_eoa.api.test

import kotlinx.datetime.*
import top.kagg886.sylu_eoa.api.v2.BadCredentialsException
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.sylu_eoa.api.v2.bean.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal class TestEOAClient : EOAClient {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val todayWeekMonday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)



    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun getUserProfile(): UserProfile {
        val avatar = Base64.decode(
            source = "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"
        )
        return UserProfile(
            name = "千早爱音",
            collegeName = "羽秋高级中学",
            studyName = "高一",
            avatar = avatar,
            email = "anon@tokyo.com",
            phone = "1145141919810",
            id = "1",
            policy = "群众",
            language = "日语"
        )
    }

    override suspend fun getSchoolCalender(): SchoolCalender {
        return SchoolCalender(
            start = todayWeekMonday.minus(7, DateTimeUnit.WEEK),
            end = todayWeekMonday.plus(7, DateTimeUnit.WEEK),
        )
    }

    override suspend fun getAllAvailableTerms(): TermResult {
        val pickers = (today.year downTo (today.year - 20)).map {
            TermPicker(
                yearName = "$it-${it + 1}" to "$it",
                yearCode = "1" to "1"
            )
        }
        return TermResult(
            list = pickers,
            default = pickers[0]
        )
    }

    override suspend fun getExamList(picker: TermPicker): List<ExamItem> {
        return listOf(
            ExamItem(
                year = "2024",
                semester = "1",
                courseID = "CS001",
                detailsID = "CS001_001",
                name = "高等数学",
                teacher = "张教授",
                credit = "4.0",
                gradePoint = "3.8",
                crTimesGp = "15.2",
                absoluteScore = "85",
                relateScore = "良好",
                completionCode = "1",
                _degreeProgram = "是",
                submitTime = today.minus(DatePeriod(days = 30)).atTime(0, 0)
            ),
            ExamItem(
                year = "2024",
                semester = "1",
                courseID = "CS002",
                detailsID = "CS002_001",
                name = "线性代数",
                teacher = "李教授",
                credit = "3.0",
                gradePoint = "4.0",
                crTimesGp = "12.0",
                absoluteScore = "92",
                relateScore = "优秀",
                completionCode = "1",
                _degreeProgram = "是",
                submitTime = today.minus(DatePeriod(days = 25)).atTime(0, 0)
            ),
            ExamItem(
                year = "2024",
                semester = "1",
                courseID = "CS003",
                detailsID = "CS003_001",
                name = "程序设计基础",
                teacher = "王教授",
                credit = "3.5",
                gradePoint = "3.5",
                crTimesGp = "12.25",
                absoluteScore = "78",
                relateScore = "中等",
                completionCode = "1",
                _degreeProgram = "是",
                submitTime = today.minus(DatePeriod(days = 20)).atTime(0, 0)
            ),
            ExamItem(
                year = "2024",
                semester = "1",
                courseID = "CS004",
                detailsID = "CS004_001",
                name = "大学英语",
                teacher = "刘教授",
                credit = "2.0",
                gradePoint = "2.5",
                crTimesGp = "5.0",
                absoluteScore = "55",
                relateScore = "不及格",
                completionCode = "1",
                _degreeProgram = "否",
                submitTime = today.minus(DatePeriod(days = 15)).atTime(0, 0)
            ),
            ExamItem(
                year = "2023",
                semester = "2",
                courseID = "CS005",
                detailsID = "CS005_001",
                name = "数据结构",
                teacher = "陈教授",
                credit = "4.0",
                gradePoint = "3.2",
                crTimesGp = "12.8",
                absoluteScore = "72",
                relateScore = "中等",
                completionCode = "16",
                _degreeProgram = "是",
                submitTime = today.minus(DatePeriod(days = 10)).atTime(0, 0)
            )
        )
    }

    override suspend fun getExamInfo(examItem: ExamItem): List<List<String>> {
        return listOf(
            listOf("无")
        )
    }

    override suspend fun getClassTable(picker: TermPicker): ClassReturn {
        return ClassReturn(
            extend = listOf(),
            tables = listOf(
                ClassTable(
                    name = "高等数学",
                    teacher = "张教授",
                    room = "教学楼A101",
                    weekEachLesson = "1-16周",
                    lesson = "1-2",
                    dayInWeek = "1",
                    score = "4.0",
                    classType = "考试",
                    _degreeProgram = "是"
                ),
                ClassTable(
                    name = "线性代数",
                    teacher = "李教授",
                    room = "教学楼A102",
                    weekEachLesson = "1-16周",
                    lesson = "3-4",
                    dayInWeek = "1",
                    score = "3.0",
                    classType = "考试",
                    _degreeProgram = "是"
                ),
                ClassTable(
                    name = "程序设计基础",
                    teacher = "王教授",
                    room = "计算机楼B201",
                    weekEachLesson = "1-16周",
                    lesson = "5-6",
                    dayInWeek = "2",
                    score = "3.5",
                    classType = "考试",
                    _degreeProgram = "是"
                ),
                ClassTable(
                    name = "大学英语",
                    teacher = "刘教授",
                    room = "外语楼C301",
                    weekEachLesson = "1-16周",
                    lesson = "1-2",
                    dayInWeek = "3",
                    score = "2.0",
                    classType = "考查",
                    _degreeProgram = "否"
                ),
                ClassTable(
                    name = "体育",
                    teacher = "赵教练",
                    room = "体育馆",
                    weekEachLesson = "1-16周",
                    lesson = "7-8",
                    dayInWeek = "4",
                    score = "1.0",
                    classType = "考查",
                    _degreeProgram = "否"
                ),
                ClassTable(
                    name = "数据结构",
                    teacher = "陈教授",
                    room = "计算机楼B202",
                    weekEachLesson = "1-16周",
                    lesson = "3-4",
                    dayInWeek = "5",
                    score = "4.0",
                    classType = "考试",
                    _degreeProgram = "是"
                )
            )

        )
    }

    override suspend fun getGPAScores(): List<GPAScoreSummary> {
        return listOf(
            GPAScoreSummary(
                name = "2024学年第一学期",
                score = 3.45
            ),
            GPAScoreSummary(
                name = "2023学年第二学期",
                score = 3.12
            ),
            GPAScoreSummary(
                name = "2023学年第一学期",
                score = 3.78
            ),
            GPAScoreSummary(
                name = "总体GPA",
                score = 3.42
            )
        )
    }

    override suspend fun getGPAScoreList(summary: GPAScoreSummary): List<GPAScore> {
        return when (summary.name) {
            "2024学年第一学期" -> listOf(
                GPAScore(name = "高等数学", score = "85"),
                GPAScore(name = "线性代数", score = "92"),
                GPAScore(name = "程序设计基础", score = "78"),
                GPAScore(name = "大学英语", score = "55"),
                GPAScore(name = "体育", score = "88")
            )

            "2023学年第二学期" -> listOf(
                GPAScore(name = "数据结构", score = "72"),
                GPAScore(name = "计算机组成原理", score = "80"),
                GPAScore(name = "概率论与数理统计", score = "75"),
                GPAScore(name = "大学物理", score = "68"),
                GPAScore(name = "马克思主义基本原理", score = "82")
            )

            "2023学年第一学期" -> listOf(
                GPAScore(name = "C语言程序设计", score = "90"),
                GPAScore(name = "高等数学(上)", score = "88"),
                GPAScore(name = "大学英语(1)", score = "85"),
                GPAScore(name = "思想道德与法治", score = "92"),
                GPAScore(name = "军事理论", score = "95")
            )

            "总体GPA" -> listOf(
                GPAScore(name = "专业课平均分", score = "82.5"),
                GPAScore(name = "公共课平均分", score = "78.3"),
                GPAScore(name = "选修课平均分", score = "85.7"),
                GPAScore(name = "实践课平均分", score = "90.2")
            )

            else -> listOf()
        }
    }

    override suspend fun getNotice(hasRead: Boolean): List<SystemNotice> {
        return if (hasRead) {
            listOf(
                SystemNotice(
                    createTime = today.minus(DatePeriod(days = 7)).atTime(0, 0),
                    title = "2024学年第一学期期末考试安排通知",
                    content = """
                        各位同学：

                        2024学年第一学期期末考试将于2024年1月15日-1月25日举行，具体安排如下：

                        1. 考试时间：2024年1月15日-1月25日
                        2. 考试地点：详见各科目考试安排
                        3. 注意事项：
                           - 请携带学生证和身份证参加考试
                           - 考试开始15分钟后不得入场
                           - 严禁携带手机等电子设备进入考场

                        祝各位同学考试顺利！

                        教务处
                        2024年1月8日
                    """.trimIndent(),
                    id = "1",
                ),
                SystemNotice(
                    createTime = today.minus(DatePeriod(days = 5)).atTime(0, 0),
                    title = "关于寒假放假时间安排的通知",
                    content = """
                        各位同学：

                        根据学校安排，现将寒假放假时间通知如下：

                        1. 放假时间：2024年1月26日-2024年2月25日
                        2. 开学时间：2024年2月26日
                        3. 注意事项：
                           - 离校前请做好宿舍安全检查
                           - 假期注意人身安全
                           - 按时返校报到

                        学生处
                        2024年1月1日
                    """.trimIndent(),
                    id = "4",
                )
            )
        } else {
            listOf(
                SystemNotice(
                    createTime = today.minus(DatePeriod(days = 3)).atTime(0, 0),
                    title = "关于2024学年第二学期选课的通知",
                    content = """
                        各位同学：

                        2024学年第二学期选课工作即将开始，请注意以下事项：

                        1. 选课时间：2024年2月20日-2024年2月28日
                        2. 选课方式：登录教务系统进行在线选课
                        3. 注意事项：
                           - 请根据培养方案合理选择课程
                           - 注意课程时间冲突
                           - 选课结束后不得随意退课

                        教务处
                        2024年2月19日
                    """.trimIndent(),
                    id = "2",
                ),
                SystemNotice(
                    createTime = today.minus(DatePeriod(days = 1)).atTime(0, 0),
                    title = "关于开展2024年春季学期体检的通知",
                    content = """
                        各位同学：

                        为保障学生身体健康，学校将组织开展春季学期体检，具体安排如下：

                        1. 体检时间：2024年3月1日-3月15日
                        2. 体检地点：校医院
                        3. 体检项目：常规体检项目
                        4. 注意事项：
                           - 体检前一天晚上10点后禁食
                           - 体检当天早上空腹
                           - 请携带学生证

                        校医院
                        2024年2月17日
                    """.trimIndent(),
                    id = "3",
                )
            )
        }
    }

    override fun init(storage: Storage) {

    }

    override var username: String = ""
    override var password: String = ""

    override suspend fun login(captchaHandler: (suspend (a: ByteArray) -> String)?) {
        if (username != "test" || password != "test") {
            throw BadCredentialsException()
        }
    }

    override suspend fun logout() {
    }

    override suspend fun markNoticeReadable(noticeId: String): Boolean = true

}
