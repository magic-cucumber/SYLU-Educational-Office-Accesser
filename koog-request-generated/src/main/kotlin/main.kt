import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.structure.StructuredRequestConfig
import ai.koog.prompt.structure.executeStructured
import ai.koog.prompt.structure.json.JsonStructure
import ai.koog.prompt.structure.json.generator.StandardJsonSchemaGenerator
import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2026/2/23 18:21
 * ================================================
 */


fun main() = runBlocking {
    val agent = SingleLLMPromptExecutor(
        llmClient = OpenAILLMClient(
            apiKey = "",
            settings = OpenAIClientSettings(
                baseUrl = "",
            ),
            baseClient = HttpClient {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) = println(message)
                    }
                    level = LogLevel.ALL
                }
            }
        )
    )

    val data = agent.executeStructured(
        prompt = prompt("structured-data") {
            system(
                content = """
                                    你是课程信息提取Agent，将自然语言描述转为符合CourseAddReturn JSON Schema的结构化数据。

                                    ## 原则
                                    1. 仅提取输入中明确提到的信息，不推测、不补全。
                                    2. 缺失信息填null，不能虚构。
                                    3. 每个record仅对应一个weekNumber。

                                    ## 字段定义
                                    course:
                                    - name: 课程名（必须完整）
                                    - teacherName: 教师姓名
                                    - classroomName: 教室位置
                                    - credits: 学分
                                    - isDegreeRequired: 是否为学位课
                                    - isExaminable: 是否为考试课

                                    record:
                                    - weekNumber: 周次（展开所有范围），如果输入出现具体日期，需要根据学期起止日期计算周数：
                                      - 学期开始时间：${1}
                                      - 学期结束时间：${2}
                                    - dayOfWeek: 星期几（1=周一,...,7=周日）
                                    - periodOfDay: 二维数组，每行对应一个dayOfWeek的节次，如 [[3,4]]

                                    ## 时间解析规则
                                    - "第3-16周" → 3~16
                                    - "3-5周(单)" → 3,5
                                    - "7-14周(双)" → 8,10,12,14
                                    - "第5周开始" → 5到学期结束（默认到第${3}周）
                                    - "单周"/"双周" → 1,3... 或 2,4...
                                    - 当输入包含具体日期（如“2025年9月10日”），根据 ${4} 计算该日期属于第几周。

                                    ## 输出要求
                                    - 无课程信息 → course = null
                                    - 无时间安排 → record = []
                                    - 输出严格符合CourseAddReturn结构，不能添加任何解释或多余字符
                                """.trimIndent()
            )
            user {

            }
        },
        model = OpenAIModels.Chat.GPT4o.copy(id = ""),
        config = StructuredRequestConfig(
            default = StructuredRequest.Manual(
                JsonStructure.create(
                    schemaGenerator = StandardJsonSchemaGenerator,
                    examples = example
                )
            ),
        ),
    )

    println(data.getOrThrow().data)
}


@Serializable
@SerialName("CourseAddReturn")
@LLMDescription("课程添加返回结果，包含课程基本信息和时间安排")
private data class CourseAddReturn(
    @property:LLMDescription("课程基本信息")
    val course: LLMCourseReturn?,
    @property:LLMDescription("课程时间安排列表")
    val record: List<LLMRecordReturn>?
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
    @property:LLMDescription("是否为考试课，true标识考试课")
    val isExaminable: Boolean?,
)

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
)

private val example = listOf(
    // 示例1：时间范围处理
    CourseAddReturn(
        course = LLMCourseReturn(
            name = "艺术鉴赏",
            teacherName = null,
            classroomName = "A栋302",
            credits = null,
            isDegreeRequired = null,
            isExaminable = null,
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
        )
    )
)
