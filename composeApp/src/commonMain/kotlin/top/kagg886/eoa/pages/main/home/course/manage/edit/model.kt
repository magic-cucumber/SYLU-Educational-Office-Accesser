package top.kagg886.eoa.pages.main.home.course.manage.edit

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.dsl.RequestMessagePartsBuilder
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.StructureFixingParser
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.structure.StructuredRequestConfig
import ai.koog.prompt.structure.json.JsonStructure
import ai.koog.prompt.structure.json.generator.StandardJsonSchemaGenerator
import androidx.lifecycle.ViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.orbitContainer
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.backend.database.dao.LLMProviderEntity
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.race
import kotlin.time.Duration.Companion.seconds

class CourseEditModel(
    database: AppDatabase,
    courseId: Long?,
    private val llmExecutors: Map<LLMProviderEntity, MultiLLMPromptExecutor>
) : ViewModel(), OrbitContainerHost<CourseEditState, CourseEditState, CourseEditSideEffect> {
    private val logger = "CourseEditModel".asTaggedLogger
    private val courseDao = database.courseDao()
    private val courseRecordDao = database.courseRecordDao()
    override val container =
        orbitContainer<CourseEditState, CourseEditSideEffect>(CourseEditState.Loading) {
            val (xnm, xqm) = AppSyncMMKV.picker!!.default.asTerm()
            val courseInfo = courseId?.let { courseDao.getById(it) } ?: CourseEntity(
                name = "",
                teacherName = "",
                classroomName = "",
                credits = 0f,
                isDegreeRequired = false,
                isExaminable = false,
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
                    llmKeys = llmExecutors.keys.toList(),
                    enableSaveButton = true,
                    aiGenerating = null,
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
    fun selectLLMKey(it: LLMProviderEntity) = intent {
        runOn<CourseEditState.Success> {
            reduce {
                state.copy(
                    selectLLMKey = it
                )
            }
        }
    }

    fun generateCourseByAI(it: String) = intent {
        postSideEffect(
            CourseEditSideEffect.Toast(
                SnackBarType.Info,
                "正在生成，请稍等.."
            )
        )

        val sub = generateCourseByAIInternal {
            text("请解析以下课程描述并输出符合 CourseAddReturn 的 JSON:")
            text(it)
        }

        sub.join()
    }

    fun generateCourseByImage() = intent {
        val picker =
            FileKit.openFilePicker(type = FileKitType.Image, dialogSettings = FileKitDialogSettings.createDefault())

        if (picker == null) {
            postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Warning, "请选择图片！"))
            return@intent
        }

        postSideEffect(
            CourseEditSideEffect.Toast(
                SnackBarType.Info,
                "正在生成，请稍等...\n请确保您的模型支持图片输入。"
            )
        )

        val byt = picker.readBytes()

        val sub = generateCourseByAIInternal {
            text("请理解图片中的内容，并输出符合 CourseAddReturn 的 JSON:")
            image(
                AttachmentSource.Image(
                    content = AttachmentContent.Binary.Bytes(byt),
                    format = "png",
                    mimeType = "image/png",
                    fileName = "capture.png"
                )
            )
        }

        sub.join()
    }

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

    @OptIn(OrbitExperimental::class)
    private fun generateCourseByAIInternal(build: RequestMessagePartsBuilder.() -> Unit) = intent {
        runOn<CourseEditState.Success> {
            val config = state.selectLLMKey
            val executor = llmExecutors[config]
            if (executor == null || config == null) {
                postSideEffect(
                    CourseEditSideEffect.Toast(
                        SnackBarType.Error,
                        "请先在 '设置 / 模型管理' 中配置模型"
                    )
                )
                return@runOn
            }

            reduce {
                state.copy(
                    enableSaveButton = false,
                    aiGenerating = ""
                )
            }

            val app = AppSyncMMKV.calender!!
            val model = OpenAIModels.Chat.GPT4o.copy(id = config.modelName)

            val response = coroutineScope {
                val pending = async {
                    try {
                        executor.executeStructured(
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
                                      - 学期开始时间：${app.start}
                                      - 学期结束时间：${app.end}
                                    - dayOfWeek: 星期几（1=周一,...,7=周日）
                                    - periodOfDay: 二维数组，每行对应一个dayOfWeek的节次，如 [[3,4]]

                                    ## 时间解析规则
                                    - "第3-16周" → 3~16
                                    - "3-5周(单)" → 3,5
                                    - "7-14周(双)" → 8,10,12,14
                                    - "第5周开始" → 5到学期结束（默认到第${app.count()}周）
                                    - "单周"/"双周" → 1,3... 或 2,4...
                                    - 当输入包含具体日期（如“2025年9月10日”），根据 ${app.start} 计算该日期属于第几周。

                                    ## 输出要求
                                    - 无课程信息 → course = null
                                    - 无时间安排 → record = []
                                    - 输出严格符合CourseAddReturn结构，不能添加任何解释或多余字符
                                    """.trimIndent()
                                )

                                user(build)
                            },
                            model = model,
                            config = StructuredRequestConfig(
                                default = when (config.supportNativeJsonOutput) {
                                    true -> StructuredRequest.Native(
                                        structure = JsonStructure.create(
                                            schemaGenerator = StandardJsonSchemaGenerator.Default,
                                            examples = example
                                        )
                                    )

                                    false -> StructuredRequest.Manual(
                                        structure = JsonStructure.create(
                                            schemaGenerator = StandardJsonSchemaGenerator.Default,
                                            examples = example
                                        )
                                    )
                                }
                            ),
                            fixingParser = StructureFixingParser(
                                model = model,
                                retries = 3
                            )
                        )
                    } catch (e: Throwable) {
                        Result.failure(e.cause ?: e)
                    }
                }


                race(
                    pending,
                    async {
                        var cnt = 1
                        while (true) {
                            cnt = (cnt + 1) % 4
                            delay(1.seconds)
                            reduce {
                                state.copy(
                                    aiGenerating = (1..cnt).joinToString("") { "." }
                                )
                            }
                        }
                        error("unreachable code")
                    }
                )
            }

            reduce {
                state.copy(
                    enableSaveButton = true,
                    aiGenerating = null,
                )
            }

            if (response.isFailure) {
                val ex = response.exceptionOrNull()
                logger.w("无法生成课表数据", ex)
                postSideEffect(
                    CourseEditSideEffect.Toast(
                        SnackBarType.Error,
                        "生成课表数据时发生错误，请查看日志\n${ex?.message?.lines()?.get(0)}",
                    )
                )
                return@runOn
            }

            val (course, record) = response.getOrThrow().data

            if (course == null && record.isNullOrEmpty()) {
                postSideEffect(
                    CourseEditSideEffect.Toast(
                        SnackBarType.Warning,
                        "未检索到有效的数据。"
                    )
                )
                return@runOn
            }

            if (course != null) {
                reduce {
                    state.copy(
                        courseInfo = with(AppSyncMMKV.picker!!.default.asTerm()) {
                            course.toEntity(xnm, xqm)
                        }
                    )
                }
            }

            if (record.isNullOrEmpty().not()) {
                reduce {
                    state.copy(
                        recordInfo = record.flatMap { it.toEntity() }
                    )
                }
            }

            postSideEffect(
                CourseEditSideEffect.Toast(
                    type = SnackBarType.Success,
                    message = when {
                        course != null && record.isNullOrEmpty()
                            .not() -> "成功生成课程详情和 ${record.size} 个 课程数据。请查阅后点击确定以添加课程"

                        course == null && record.isNullOrEmpty()
                            .not() -> "未生成课程详情，但成功解析出 ${record.size} 个 课程数据。请查阅后点击确定以添加课程"

                        course != null && record.isNullOrEmpty() -> "成功生成课程详情，但未生成出任何课程。请查阅后点击确定以添加课程"
                        else -> return@runOn
                    }
                )
            )
        }
    }
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
) {
    fun toEntity(year: String, sem: String): CourseEntity = CourseEntity(
        name = name ?: "",
        teacherName = teacherName ?: "",
        classroomName = classroomName ?: "",
        credits = credits ?: 0.0f,
        isDegreeRequired = isDegreeRequired ?: false,
        isExaminable = isExaminable ?: false,
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
    fun toEntity(): List<CourseRecordEntity> = periodOfDay.withIndex().flatMap { (index, periods) ->
        periods.map {
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

        val llmKeys: List<LLMProviderEntity>,
        val selectLLMKey: LLMProviderEntity? = llmKeys.firstOrNull(),

        val aiGenerating: String?,
    ) : CourseEditState
}

sealed interface CourseEditSideEffect {
    data class Toast(val type: SnackBarType, val message: String) : CourseEditSideEffect
    data object NavigateBack : CourseEditSideEffect
}
