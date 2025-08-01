package top.kagg886.eoa.pages.main.home.course.manage.edit

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.structure.executeStructured
import ai.koog.prompt.structure.json.JsonSchemaGenerator
import ai.koog.prompt.structure.json.JsonStructuredData
import androidx.lifecycle.ViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppAiMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.logger as kermit
import top.kagg886.util.asTaggedLogger
import kotlin.time.Duration.Companion.seconds

class CourseEditModel(
    database: AppDatabase,
    courseId: Long?
) : ViewModel(), ContainerHost<CourseEditState, CourseEditSideEffect> {
    private val logger = "CourseEditModel".asTaggedLogger

    private val courseDao = database.courseDao()
    private val courseRecordDao = database.courseRecordDao()
    override val container =
        container<CourseEditState, CourseEditSideEffect>(CourseEditState.Loading) {
            val (xnm, xqm) = AppSyncMMKV.picker!!.default.asTerm()
            val courseInfo = courseId?.let { courseDao.getById(it) } ?: CourseEntity(
                name = "",
                teacherName = "",
                classroomName = "",
                credits = 0f,
                isDegreeRequired = false,
                yearCode = xnm,
                semesterCode = xqm,
                isUserAdded = true
            )

            val records = courseId?.let { courseRecordDao.getByCourseId(it) } ?: emptyList()

            reduce {
                CourseEditState.Success(
                    courseId = courseId,
                    courseInfo = courseInfo,
                    recordInfo = records,
                    startDate = AppSyncMMKV.calender!!.start,
                    allWeekNumber = AppSyncMMKV.calender!!.count(),
                    enableSaveButton = true,
                    aiGenerating = null,
                    aiKey = AppAiMMKV.apiKey,
                    aiEndpoint = AppAiMMKV.endpoint,
                    aiModel = AppAiMMKV.model
                )
            }
        }


    @OptIn(OrbitExperimental::class)
    fun modifyCourse(it: CourseEntity) = intent {
        runOn<CourseEditState.Success> {
            reduce {
                state.copy(
                    courseInfo = it,
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun confirmModifyCourse() = intent {
        runOn<CourseEditState.Success> {
            if (state.courseInfo.name.isBlank()) {
                postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Error, "课程名不能为空"))
                return@runOn
            }
            if (state.courseInfo.classroomName.isBlank()) {
                postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Error, "教室不能为空"))
                return@runOn
            }
            if (state.recordInfo.isEmpty()) {
                postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Error, "请添加课程时间"))
                return@runOn
            }
            reduce { state.copy(enableSaveButton = false) } //防止重复点击
            val id = state.courseId?.apply { courseDao.update(state.courseInfo) } ?: courseDao.insert(state.courseInfo)
            if (state.courseId == null) {
                courseRecordDao.insertAll(state.recordInfo.map { it.copy(courseId = id) })
            }
            postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Success, "修改成功"))
            reduce {
                state.copy(
                    courseInfo = state.courseInfo.copy(id = id),
                )
            }
            delay(3.seconds)
            postSideEffect(CourseEditSideEffect.NavigateBack)
        }
    }

    @OptIn(OrbitExperimental::class)
    fun addRecord(weekNumber: Int, dayOfWeek: Int, periodOfDay: Int) = intent {
        runOn<CourseEditState.Success> {
            val record = CourseRecordEntity(
                id = null,
                courseId = state.courseId,
                weekNumber = weekNumber,
                dayOfWeek = dayOfWeek,
                periodOfDay = periodOfDay,
                isUserAdded = true
            )
            //是修改模式则编辑数据库
            if (state.courseId != null) {
                courseRecordDao.insert(record)
            }
            reduce {
                state.copy(
                    recordInfo = state.recordInfo + record
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun deleteRecord(it: CourseRecordEntity) = intent {
        runOn<CourseEditState.Success> {
            //是修改模式则编辑数据库
            if (it.courseId != null) {
                courseRecordDao.delete(it)
            }
            reduce {
                state.copy(
                    recordInfo = state.recordInfo - it
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun setAiEndpoint(it: String) = intent {
        runOn<CourseEditState.Success> {
            AppAiMMKV.endpoint = it
            reduce {
                state.copy(
                    aiEndpoint = it
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun setAiKey(it: String) = intent {
        runOn<CourseEditState.Success> {
            AppAiMMKV.apiKey = it
            reduce {
                state.copy(
                    aiKey = it
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun setAiModel(it: String) = intent {
        runOn<CourseEditState.Success> {
            AppAiMMKV.model = it
            reduce {
                state.copy(
                    aiModel = it
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun generateCourseByAI(it: String) = intent {
        runOn<CourseEditState.Success> {
            reduce {
                state.copy(
                    enableSaveButton = false,
                    aiGenerating = ""
                )
            }

            postSideEffect(
                CourseEditSideEffect.Toast(
                    SnackBarType.Info,
                    "正在生成，请稍等.."
                )
            )

            // 构造Koog的agent对象
            val agent = SingleLLMPromptExecutor(
                llmClient = OpenAILLMClient(
                    apiKey = state.aiKey,
                    settings = OpenAIClientSettings(
                        baseUrl = state.aiEndpoint,
                    ),
                    baseClient = HttpClient {
                        install(Logging) {
                            logger  = object : Logger {
                                override fun log(message: String) = kermit.d(message)
                            }
                            level = LogLevel.ALL
                        }
                    }
                )
            )

            val response = agent.executeStructured(
                prompt = prompt("structured-data") {
                    system(
                        """
                        你是一个专业的课程信息提取Agent，负责从自然语言描述中提取课程相关信息并转换为结构化的JSON数据。

                        ## 核心原则
                        1. **准确性优先**：只提取明确存在于输入文本中的信息，绝对不允许编造或推测任何数据
                        2. **宁缺毋滥**：如果某个字段的信息在输入中不明确或不存在，必须返回null，而不是猜测或编造
                        3. **严格匹配**：只有当输入文本中明确提到相关信息时，才填充对应字段

                        ## 提取规则

                        ### 课程基本信息 (course)
                        - **name**: 课程名称，必须是输入中明确提到的完整课程名
                        - **teacherName**: 教师姓名，只有明确提到教师姓名时才填充
                        - **classroomName**: 教室位置，只有明确提到教室信息时才填充
                        - **credits**: 课程学分，只有明确提到学分数值时才填充
                        - **isDegreeRequired**: 是否学位课，只有明确说明是否为学位课时才填充

                        ### 时间安排信息 (record)
                        **重要**：对于时间范围（如"第3周至第16周"），需要为每一周都创建一个record记录！

                        - **weekNumber**: 学期周数(1-20)，需要理解时间范围表达：
                          * "第3周至第16周" → 需要创建14个record（第3,4,5...16周各一个）
                          * "第1-10周" → 需要创建10个record（第1,2,3...10周各一个）
                          * "第5周开始" → 从第5周开始到学期结束
                          * "单周" → 只在奇数周（1,3,5,7...）
                          * "双周" → 只在偶数周（2,4,6,8...）
                          * "每周" → 每一周都有
                          * **复合表达**：
                            - "3-5周（单）" → 在3,4,5周范围内选择单数周 → 第3,5周
                            - "7-14周（双）" → 在7,8,9,10,11,12,13,14周范围内选择双数周 → 第8,10,12,14周
                            - "1-8周（单），10-16周（双）" → 第1,3,5,7周 + 第10,12,14,16周

                        - **dayOfWeek**: 星期几(1=周一, 2=周二, ..., 7=周日)，只有明确提到星期时才填充
                        - **periodOfDay**: 第几节课(1-12)，只有明确提到节次时才填充

                        **处理逻辑示例**：
                        输入："第3周至第16周开课，每周三下午3-4节"
                        输出：应该创建14个record，每个record为：
                        - weekNumber: 3 (第一个record)
                        - weekNumber: 4 (第二个record)
                        - ...
                        - weekNumber: 16 (最后一个record)
                        每个record的dayOfWeek都是[3]，periodOfDay都是[[3,4]]

                        ## 常见时间表达映射
                        - 周一/星期一 → 1, 周二/星期二 → 2, ..., 周日/星期日 → 7
                        - 第一节/1节 → 1, 第二节/2节 → 2, 以此类推
                        - 上午1-2节 → [1,2], 下午3-4节 → [3,4], 晚上9-10节 → [9,10]
                        - 时间范围关键词：
                          * "至"、"到"、"—"、"-" → 表示从开始到结束的连续范围
                          * "每周" → 表示所有周都有
                          * "单周"、"奇数周" → 只在1,3,5,7...周
                          * "双周"、"偶数周" → 只在2,4,6,8...周

                        ## 时间范围处理示例
                        - "第3周至第16周" → 创建14个record，weekNumber分别为3,4,5,6,7,8,9,10,11,12,13,14,15,16
                        - "第1-5周，每周三下午3-4节" → 创建5个record，每个record的weekNumber为1,2,3,4,5，dayOfWeek=[3]，periodOfDay=[[3,4]]
                        - "第2周开始，单周，周一上午1-2节" → 创建record，weekNumber为3,5,7,9,11,13,15...（从第3周开始的奇数周）
                        - "3-5周（单）" → 在3,4,5周中筛选单数周，创建2个record，weekNumber为3,5
                        - "7-14周（双）" → 在7,8,9,10,11,12,13,14周中筛选双数周，创建4个record，weekNumber为8,10,12,14
                        - "3-5周（单），7-14周（双）" → 创建6个record，weekNumber为3,5,8,10,12,14

                        ## 输出要求
                        - 如果输入文本中没有任何课程相关信息，返回course为null
                        - 如果没有明确的时间安排信息，record返回空数组[]
                        - **对于时间范围，必须为范围内的每一周都创建一个独立的record**
                        - **每个record只能包含一个weekNumber，不能用数组**
                        - 所有不确定的字段必须设置为null
                        - 确保输出的JSON格式严格符合定义的数据结构
                    """.trimIndent()
                    )

                    user("""
                        请仔细分析以下课程描述，特别注意时间范围的表达：

                        $it

                        处理步骤：
                        1. 首先识别课程基本信息（名称、教师、教室等）
                        2. 然后识别时间安排，特别注意：
                           - 如果提到"第X周至第Y周"，需要为X到Y之间的每一周都创建一个record
                           - 如果提到"每周"，表示所有涉及的周都有课
                           - 如果提到具体的星期和节次，要准确映射到数字
                           - **复合时间表达**：如"3-5周（单）"，需要先确定范围（3,4,5周），再筛选单数周（3,5周）
                           - **多段时间表达**：如"3-5周（单），7-14周（双）"，需要分别处理每个时间段
                        3. 确保每个record只包含一个weekNumber值
                        4. 对于不确定的信息，设置为null
                    """.trimIndent())
                },
                mainModel = OpenAIModels.Chat.GPT4o.copy(id = state.aiModel),
                fixingModel = OpenAIModels.Chat.GPT4o.copy(id = state.aiModel),
                structure = JsonStructuredData.createJsonStructure<CourseAddReturn>(
                    schemaFormat = JsonSchemaGenerator.SchemaFormat.JsonSchema,
                    examples = listOf(
                        // 示例1：时间范围处理
                        CourseAddReturn(
                            course = LLMCourseReturn(
                                name = "艺术鉴赏",
                                teacherName = null,
                                classroomName = "A栋302",
                                credits = null,
                                isDegreeRequired = null
                            ),
                            record = listOf(
                                // 第3周至第16周，每周三下午3-4节
                                LLMRecordReturn(
                                    weekNumber = 3,
                                    dayOfWeek = listOf(3),
                                    periodOfDay = listOf(listOf(3, 4))
                                ),
                                LLMRecordReturn(
                                    weekNumber = 4,
                                    dayOfWeek = listOf(3),
                                    periodOfDay = listOf(listOf(3, 4))
                                ),
                                LLMRecordReturn(
                                    weekNumber = 5,
                                    dayOfWeek = listOf(3),
                                    periodOfDay = listOf(listOf(3, 4))
                                )
                                // ... 应该继续到第16周，这里只展示前几个
                            )
                        ),
                        // 示例2：复合时间概念（单双周）
                        CourseAddReturn(
                            course = LLMCourseReturn(
                                name = "高等数学",
                                teacherName = "王教授",
                                classroomName = "B201",
                                credits = 4.0f,
                                isDegreeRequired = true
                            ),
                            record = listOf(
                                // 3-5周（单）：第3,5周
                                LLMRecordReturn(
                                    weekNumber = 3,
                                    dayOfWeek = listOf(2),
                                    periodOfDay = listOf(listOf(1, 2))
                                ),
                                LLMRecordReturn(
                                    weekNumber = 5,
                                    dayOfWeek = listOf(2),
                                    periodOfDay = listOf(listOf(1, 2))
                                ),
                                // 7-14周（双）：第8,10,12,14周
                                LLMRecordReturn(
                                    weekNumber = 8,
                                    dayOfWeek = listOf(2),
                                    periodOfDay = listOf(listOf(1, 2))
                                ),
                                LLMRecordReturn(
                                    weekNumber = 10,
                                    dayOfWeek = listOf(2),
                                    periodOfDay = listOf(listOf(1, 2))
                                ),
                                LLMRecordReturn(
                                    weekNumber = 12,
                                    dayOfWeek = listOf(2),
                                    periodOfDay = listOf(listOf(1, 2))
                                ),
                                LLMRecordReturn(
                                    weekNumber = 14,
                                    dayOfWeek = listOf(2),
                                    periodOfDay = listOf(listOf(1, 2))
                                )
                            )
                        ),
                        // 示例3：简单时间安排
                        CourseAddReturn(
                            course = LLMCourseReturn(
                                name = "大学英语",
                                teacherName = "李芳",
                                classroomName = "A314",
                                credits = 3.5f,
                                isDegreeRequired = true
                            ),
                            record = listOf(
                                // 第1周，周一第2-3节，周三第2节
                                LLMRecordReturn(
                                    weekNumber = 1,
                                    dayOfWeek = listOf(1, 3),
                                    periodOfDay = listOf(
                                        listOf(2, 3),
                                        listOf(2)
                                    )
                                )
                            )
                        )
                    ),
                    schemaType = JsonStructuredData.JsonSchemaType.SIMPLE
                ),
            )

            reduce {
                state.copy(
                    enableSaveButton = true,
                    aiGenerating = null,
                )
            }

            if (response.isFailure) {
                logger.w("无法生成课表数据", response.exceptionOrNull())
                postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Error, "生成课表数据时发生错误，请查看日志"))
                return@runOn
            }

            val write = response.getOrThrow().structure

            logger.i("生成数据：${write}")

            postSideEffect(
                CourseEditSideEffect.Toast(
                    SnackBarType.Success,
                    "共生成了 ${write.record.size} 个数据，请查阅后点击确定。"
                )
            )

            reduce {
                state.copy(
                    courseInfo = with(AppSyncMMKV.picker!!.default.asTerm()) {
                        write.course.toEntity(xnm, xqm)
                    },
                    recordInfo = write.record.flatMap { it.toEntity() }
                )
            }
        }
    }
}

@Serializable
@SerialName("CourseAddReturn")
@LLMDescription("课程添加返回结果，包含课程基本信息和时间安排")
private data class CourseAddReturn(
    @property:LLMDescription("课程基本信息")
    val course: LLMCourseReturn,
    @property:LLMDescription("课程时间安排列表")
    val record: List<LLMRecordReturn>
)

@Serializable
@SerialName("LLMCourseReturn")
@LLMDescription("课程基本信息")
private data class LLMCourseReturn(
    @property:LLMDescription("课程名称，如'高等数学'")
    val name: String?,
    @property:LLMDescription("任课教师姓名，如'张教授'")
    val teacherName: String?,
    @property:LLMDescription("上课教室，如'教学楼A101'")
    val classroomName: String?,
    @property:LLMDescription("课程学分，如2.0")
    val credits: Float?,
    @property:LLMDescription("是否为学位课，true表示学位课")
    val isDegreeRequired: Boolean?,
) {
    fun toEntity(year: String, sem: String): CourseEntity = CourseEntity(
        name = name ?: "",
        teacherName = teacherName ?: "",
        classroomName = classroomName ?: "",
        credits = credits ?: 0.0f,
        isDegreeRequired = isDegreeRequired ?: false,
        isUserAdded = true,
        yearCode = year,
        semesterCode = sem
    )
}

@Serializable
@SerialName("LLMRecordReturn")
@LLMDescription("课程时间安排记录")
private data class LLMRecordReturn(
    @property:LLMDescription("具体的周数，表示这门课在第几周有课，从1开始计数。对于时间范围（如第3-16周），需要为每一周都创建一个独立的record。")
    val weekNumber: Int, // 学期周数(从1开始)
    @property:LLMDescription("dayOfWeek属性，在星期几会有这门课，1=周一，2=周二，...，7=周日")
    val dayOfWeek: List<Int>, // 星期几(1-7)
    @property:LLMDescription("一个二维矩阵，纵坐标为dayOfWeek属性的index，横坐标为在第几节会有这门课。")
    val periodOfDay: List<List<Int>>, // 第几节课
) {
    fun toEntity(): List<CourseRecordEntity> = periodOfDay.withIndex().flatMap { (index,periods)->
        periods.map { it->
            CourseRecordEntity(
                weekNumber = weekNumber,
                dayOfWeek = dayOfWeek[index],
                periodOfDay = it,
                isUserAdded = true
            )
        }
    }
}


sealed interface CourseEditState {
    data object Loading : CourseEditState
    data class Success(
        val courseId: Long?,
        val enableSaveButton: Boolean,
        val courseInfo: CourseEntity,
        val recordInfo: List<CourseRecordEntity>,
        val allWeekNumber: Int,
        val startDate: LocalDate,

        val aiGenerating: String?,
        val aiKey: String,
        val aiEndpoint: String,
        val aiModel: String
    ) : CourseEditState
}

sealed interface CourseEditSideEffect {
    data class Toast(val type: SnackBarType, val message: String) : CourseEditSideEffect
    data object NavigateBack : CourseEditSideEffect
}
