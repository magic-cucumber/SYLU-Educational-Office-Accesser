package top.kagg886.sylu_eoa.api.test

import kotlinx.datetime.*
import kotlin.time.Clock
import top.kagg886.sylu_eoa.api.v2.BadCredentialsException
import top.kagg886.sylu_eoa.api.v2.EOAClient
import top.kagg886.sylu_eoa.api.v2.NeedCaptchaException
import top.kagg886.sylu_eoa.api.v2.Storage
import top.kagg886.sylu_eoa.api.v2.bean.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.ExperimentalTime

internal class TestEOAClient : EOAClient {
    @OptIn(ExperimentalTime::class)
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
                recommender = "丰川定治",
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
                recommender = "丰川定治",
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
                recommender = "丰川定治",
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
                recommender = "丰川定治",
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
                recommender = "丰川定治",
                submitTime = today.minus(DatePeriod(days = 10)).atTime(0, 0)
            )
        )
    }

    override suspend fun getExamInfo(examItem: ExamItem): List<List<String>> {
        return listOf(
            listOf("无")
        )
    }

    override suspend fun getExamExportSink(
        term: Term,
        config: ExamExportOptions
    ): ByteArray = byteArrayOf()

    override suspend fun getClassTable(picker: TermPicker, firstDay: LocalDate): ClassReturn {
        data class Template(
            val name: String,
            val teacher: String,
            val room: String,
            val score: String,
            val classType: String,
            val isDegreeProgram: Boolean,
            val dayOfWeek: Int,
            val weeks: IntRange,
            val periods: IntRange,
        )

        fun getTimeByLessonNumber(lessonNumber: Int): Pair<LocalTime, LocalTime> {
            return when (lessonNumber) {
                1 -> LocalTime.parse("08:00") to LocalTime.parse("08:45")
                2 -> LocalTime.parse("08:55") to LocalTime.parse("09:40")
                3 -> LocalTime.parse("10:00") to LocalTime.parse("10:45")
                4 -> LocalTime.parse("10:55") to LocalTime.parse("11:40")
                5 -> LocalTime.parse("13:00") to LocalTime.parse("13:45")
                6 -> LocalTime.parse("13:55") to LocalTime.parse("14:40")
                7 -> LocalTime.parse("14:50") to LocalTime.parse("15:35")
                8 -> LocalTime.parse("15:45") to LocalTime.parse("16:30")
                9 -> LocalTime.parse("16:40") to LocalTime.parse("17:25")
                10 -> LocalTime.parse("17:35") to LocalTime.parse("18:20")
                11 -> LocalTime.parse("19:30") to LocalTime.parse("20:15")
                12 -> LocalTime.parse("20:25") to LocalTime.parse("21:10")
                else -> error("no this class")
            }
        }

        val templates = listOf(
            // 星期一：一上午长课 + 多层嵌套冲突
            Template(
                name = "高等数学专题",
                teacher = "张教授",
                room = "教学楼A101",
                score = "4.0",
                classType = "考试",
                isDegreeProgram = true,
                dayOfWeek = 1,
                weeks = 1..16,
                periods = 1..4,
            ),
            Template(
                name = "学术讲座",
                teacher = "王教授",
                room = "报告厅",
                score = "1.0",
                classType = "考查",
                isDegreeProgram = false,
                dayOfWeek = 1,
                weeks = 1..16,
                periods = 1..2,
            ),
            Template(
                name = "线性代数",
                teacher = "陈教授",
                room = "教学楼A103",
                score = "3.0",
                classType = "考试",
                isDegreeProgram = true,
                dayOfWeek = 1,
                weeks = 1..16,
                periods = 2..3,
            ),
            Template(
                name = "数学建模",
                teacher = "李教授",
                room = "实验楼A202",
                score = "2.0",
                classType = "考查",
                isDegreeProgram = false,
                dayOfWeek = 1,
                weeks = 1..16,
                periods = 3..4,
            ),

            // 星期二：一下午长课 + 复杂嵌套
            Template(
                name = "程序设计实践",
                teacher = "王教授",
                room = "计算机楼B201",
                score = "4.0",
                classType = "考试",
                isDegreeProgram = true,
                dayOfWeek = 2,
                weeks = 1..16,
                periods = 5..10,
            ),
            Template(
                name = "算法设计",
                teacher = "刘教授",
                room = "计算机楼B202",
                score = "3.0",
                classType = "考试",
                isDegreeProgram = true,
                dayOfWeek = 2,
                weeks = 1..16,
                periods = 5..6,
            ),
            Template(
                name = "数据结构实验",
                teacher = "陈教授",
                room = "实验室B301",
                score = "2.0",
                classType = "考查",
                isDegreeProgram = true,
                dayOfWeek = 2,
                weeks = 1..16,
                periods = 6..8,
            ),
            Template(
                name = "ACM训练",
                teacher = "赵老师",
                room = "机房B401",
                score = "1.0",
                classType = "考查",
                isDegreeProgram = false,
                dayOfWeek = 2,
                weeks = 1..16,
                periods = 7..10,
            ),
            Template(
                name = "软件工程讨论",
                teacher = "孙教授",
                room = "教学楼D102",
                score = "2.0",
                classType = "考查",
                isDegreeProgram = false,
                dayOfWeek = 2,
                weeks = 1..16,
                periods = 8..9,
            ),

            // 星期三：完全重合
            Template(
                name = "大学英语",
                teacher = "刘教授",
                room = "外语楼C301",
                score = "2.0",
                classType = "考试",
                isDegreeProgram = false,
                dayOfWeek = 3,
                weeks = 1..16,
                periods = 1..2,
            ),
            Template(
                name = "大学物理",
                teacher = "周教授",
                room = "理科楼C201",
                score = "3.0",
                classType = "考试",
                isDegreeProgram = true,
                dayOfWeek = 3,
                weeks = 1..16,
                periods = 1..2,
            ),
            Template(
                name = "创新创业",
                teacher = "杨老师",
                room = "教学楼C105",
                score = "1.0",
                classType = "考查",
                isDegreeProgram = false,
                dayOfWeek = 3,
                weeks = 1..16,
                periods = 1..2,
            ),

            // 星期四：跨上午和下午的超长课程
            Template(
                name = "电子技术综合实验",
                teacher = "吴教授",
                room = "实验楼E301",
                score = "3.0",
                classType = "考试",
                isDegreeProgram = true,
                dayOfWeek = 4,
                weeks = 1..16,
                periods = 1..10,
            ),
            Template(
                name = "体育",
                teacher = "赵教练",
                room = "体育馆",
                score = "1.0",
                classType = "考查",
                isDegreeProgram = false,
                dayOfWeek = 4,
                weeks = 1..16,
                periods = 3..4,
            ),
            Template(
                name = "工程训练",
                teacher = "郑老师",
                room = "工程中心",
                score = "2.0",
                classType = "考查",
                isDegreeProgram = false,
                dayOfWeek = 4,
                weeks = 1..16,
                periods = 5..8,
            ),
            Template(
                name = "就业指导",
                teacher = "钱老师",
                room = "教学楼E102",
                score = "1.0",
                classType = "考查",
                isDegreeProgram = false,
                dayOfWeek = 4,
                weeks = 1..16,
                periods = 9..10,
            ),

            // 星期五：错位交叉
            Template(
                name = "操作系统",
                teacher = "徐教授",
                room = "计算机楼F201",
                score = "4.0",
                classType = "考试",
                isDegreeProgram = true,
                dayOfWeek = 5,
                weeks = 1..16,
                periods = 1..3,
            ),
            Template(
                name = "数据库原理",
                teacher = "高教授",
                room = "计算机楼F203",
                score = "4.0",
                classType = "考试",
                isDegreeProgram = true,
                dayOfWeek = 5,
                weeks = 1..16,
                periods = 2..4,
            ),
            Template(
                name = "计算机网络",
                teacher = "胡教授",
                room = "计算机楼F202",
                score = "4.0",
                classType = "考试",
                isDegreeProgram = true,
                dayOfWeek = 5,
                weeks = 1..16,
                periods = 3..5,
            ),
            Template(
                name = "人工智能导论",
                teacher = "林教授",
                room = "计算机楼F301",
                score = "2.0",
                classType = "考试",
                isDegreeProgram = true,
                dayOfWeek = 5,
                weeks = 1..16,
                periods = 11..12,
            ),
        )

        val tables = templates.flatMap { template ->
            template.weeks.map { weekNumber ->
                val date = firstDay
                    .plus(weekNumber - 1, DateTimeUnit.WEEK)
                    .plus(template.dayOfWeek - 1, DateTimeUnit.DAY)

                val startTime =
                    getTimeByLessonNumber(template.periods.first).first

                val endTime =
                    getTimeByLessonNumber(template.periods.last).second

                ClassTable(
                    name = template.name,
                    teacher = template.teacher,
                    room = template.room,
                    score = template.score,
                    classType = template.classType,
                    isDegreeProgram = template.isDegreeProgram,
                    startTime = date.atTime(startTime),
                    endTime = date.atTime(endTime),
                )
            }
        }

        return ClassReturn(
            extend = emptyList(),
            tables = tables,
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
            )
        )
    }

    override suspend fun getGPAScoreList(summary: GPAScoreSummary): List<GPAScore> {
        return when (summary.name) {
            "2024学年第一学期" -> listOf(
                GPAScore(name = "人工智能：无尽的前沿", score = "2.0"),
                GPAScore(name = "大语言模型及AIGC基础导论", score = "1.5"),
                GPAScore(name = "迈向共生、共享、共创的人工智能时代——DeepSeek大模型原理、技术与应用", score = "1.0"),
                GPAScore(name = "AI技术的最新发展和挑战", score = "1.0"),
                GPAScore(name = "新科技革命解放人脑", score = "0.5")
            )

            "2023学年第二学期" -> listOf(
                GPAScore(name = "科技革命与大国兴衰", score = "2.0"),
                GPAScore(name = "新一轮科技革命的发展趋势与未来展望", score = "1.5"),
                GPAScore(name = "科技革命与国家命运", score = "1.0"),
                GPAScore(name = "历史上的科技革命与产业变革", score = "1.0"),
                GPAScore(name = "新科技革命与产业变革", score = "0.5")
            )

            "2023学年第一学期" -> listOf(
                GPAScore(name = "赋能无处不在的3D智能：多粒度算法与硬件的协同", score = "2.0"),
                GPAScore(name = "极端天气与突发事件对交通运行影响分析", score = "1.5"),
                GPAScore(name = "怀疑立场与革命性科技突破：来自超越知识理论的启示", score = "1.0"),
                GPAScore(name = "“新的科学革命的结构”中的哲学与科学", score = "1.0"),
                GPAScore(name = "AI赋能的信任与可信AI", score = "0.5")
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
        if (username != "test") {
            throw BadCredentialsException()
        }

        if (password == "test") {
            return
        }

        if (password == "captcha") {
            val client = captchaHandler?.invoke(
                Base64.decode(
                    "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"
                )
            )
            if (client != "YAY5BN") {
                login(captchaHandler)
            }
            return
        }

        throw BadCredentialsException()
    }

    override suspend fun logout() {
    }

    override suspend fun markNoticeReadable(noticeId: String): Boolean = true

}
