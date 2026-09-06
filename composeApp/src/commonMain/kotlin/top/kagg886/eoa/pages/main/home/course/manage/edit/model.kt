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
import top.kagg886.eoa.util.BaseViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.annotation.OrbitExperimental
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.backend.database.dao.LLMProviderEntity
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.race
import kotlin.time.Duration.Companion.seconds

class CourseEditModel(
    database: AppDatabase,
    private val courseId: Long?,
    private val llmExecutors: Map<LLMProviderEntity, MultiLLMPromptExecutor>
) : BaseViewModel<CourseEditState, CourseEditSideEffect>(name = "CourseEditModel", initial = CourseEditState.Loading) {
    private val courseDao = database.courseDao()
    private val courseRecordDao = database.courseRecordDao()
    private var nextDraftRecordId = -1L

    private fun newDraftRecordId(): Long = nextDraftRecordId--

    override suspend fun Syntax<CourseEditState, CourseEditSideEffect>.init() {
            val calendar = AppSyncMMKV.calender!!
            val courseInfo = courseId?.let { courseDao.getById(it) } ?: CourseEntity(
                name = "",
                teacherName = "",
                classroomName = "",
                credits = 0f,
                isDegreeRequired = false,
                isExaminable = false,
                isUserAdded = true
            )

            val records = courseId
                ?.let { courseRecordDao.getByCourseId(it) }
                .orEmpty()
                .map { record ->
                    if (record.id == null) record.copy(id = newDraftRecordId()) else record
                }

            reduce {
                CourseEditState.Success(
                    courseId = courseId,
                    courseInfo = courseInfo,
                    recordInfo = records,
                    startDate = calendar.start,
                    allWeekNumber = calendar.count(),
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
            state.recordInfo.timeValidationError()?.let { message ->
                postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Error, message))
                return@runOn
            }
            reduce { state.copy(enableSaveButton = false) } //防止重复点击
            val id = state.courseId?.also {
                courseDao.update(state.courseInfo.copy(id = it))
            } ?: courseDao.insert(state.courseInfo.copy(id = null))

            courseRecordDao.getByCourseId(id).forEach {
                courseRecordDao.delete(it)
            }
            courseRecordDao.insertAll(
                state.recordInfo.map {
                    it.copy(
                        id = null,
                        courseId = id,
                    )
                }
            )

            postSideEffect(CourseEditSideEffect.Toast(SnackBarType.Success, "保存成功"))
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
    fun addRecord(date: LocalDate) = intent {
        runOn<CourseEditState.Success> {
            val record = CourseRecordEntity(
                id = newDraftRecordId(),
                courseId = state.courseId,
                startTime = date.atTime(LocalTime(8, 0)),
                endTime = date.atTime(LocalTime(9, 40)),
                isUserAdded = true
            )
            reduce {
                state.copy(
                    recordInfo = state.recordInfo + record
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun updateRecord(
        record: CourseRecordEntity,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ) = intent {
        runOn<CourseEditState.Success> {
            reduce {
                state.copy(
                    recordInfo = state.recordInfo.map {
                        if (it.id == record.id) {
                            it.copy(startTime = startTime, endTime = endTime)
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }

    @OptIn(OrbitExperimental::class)
    fun deleteRecord(record: CourseRecordEntity) = intent {
        runOn<CourseEditState.Success> {
            reduce {
                state.copy(
                    recordInfo = state.recordInfo.filterNot { it.id == record.id }
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

    private fun examples(firstDay: LocalDate): List<CourseAddReturn> {
        val records = (2..3).map { weekIndex ->
            val date = firstDay
                .plus(weekIndex, DateTimeUnit.WEEK)
                .plus(2, DateTimeUnit.DAY)
            LLMRecordReturn(
                startTime = date.atTime(LocalTime(10, 0)).toString(),
                endTime = date.atTime(LocalTime(11, 40)).toString(),
            )
        }
        return listOf(
            CourseAddReturn(
                course = LLMCourseReturn(
                    name = "艺术鉴赏",
                    teacherName = null,
                    classroomName = "A栋302",
                    credits = null,
                    isDegreeRequired = null,
                    isExaminable = null,
                ),
                record = records,
            )
        )
    }

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
                                    3. 每个record表示一次实际发生的课程，必须包含完整的开始和结束日期时间。
                                    4. 周次范围、单双周和多个星期必须展开为多条具体record。

                                    ## 字段定义
                                    course:
                                    - name: 课程名（必须完整）
                                    - teacherName: 教师姓名
                                    - classroomName: 教室位置
                                    - credits: 学分
                                    - isDegreeRequired: 是否为学位课
                                    - isExaminable: 是否为考试课

                                    record:
                                    - startTime: 开始时间，严格使用yyyy-MM-ddTHH:mm格式
                                    - endTime: 结束时间，严格使用yyyy-MM-ddTHH:mm格式

                                    ## 学期信息
                                    - 第一周周一：${app.start}
                                    - 学期结束日期：${app.end}
                                    - 总周数：${app.count()}

                                    ## 节次时间
                                    - 第1节 08:00-08:45；第2节 08:55-09:40
                                    - 第3节 10:00-10:45；第4节 10:55-11:40
                                    - 第5节 13:00-13:45；第6节 13:55-14:40
                                    - 第7节 14:50-15:35；第8节 15:45-16:30
                                    - 第9节 16:40-17:25；第10节 17:35-18:20
                                    - 第11节 19:30-20:15；第12节 20:25-21:10

                                    ## 时间解析规则
                                    - "第3-16周" → 展开第3至16周的每一次上课日期
                                    - "3-5周(单)" → 展开第3、5周
                                    - "7-14周(双)" → 展开第8、10、12、14周
                                    - "第5周开始" → 5到学期结束（默认到第${app.count()}周）
                                    - "单周"/"双周" → 1,3... 或 2,4...
                                    - "3-4节" → 一条record，开始时间为10:00，结束时间为11:40
                                    - 输入给出具体时刻时，直接使用输入中的小时和分钟
                                    - 输入给出具体日期时，直接使用该日期，不再输出周次字段
                                    - 无法同时确定开始和结束时间时，不生成record

                                    ## record约束
                                    - 每条record必须位于${app.start}至${app.end}之间
                                    - startTime和endTime必须在同一天，且startTime必须早于endTime
                                    - 时间精确到分钟，不得包含秒、时区或偏移量
                                    - record之间不得发生时间交叠；首尾相接不算交叠

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
                                            examples = examples(app.start)
                                        )
                                    )

                                    false -> StructuredRequest.Manual(
                                        structure = JsonStructure.create(
                                            schemaGenerator = StandardJsonSchemaGenerator.Default,
                                            examples = examples(app.start)
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

            val generatedRecords = try {
                record.orEmpty().map {
                    it.toEntity(
                        id = newDraftRecordId(),
                        courseId = state.courseId,
                    )
                }.also { records ->
                    require(
                        records.all {
                            it.startTime.date in app.start..app.end &&
                                    it.endTime.date in app.start..app.end
                        }
                    ) {
                        "AI生成的课程时间超出当前学期范围"
                    }
                }
            } catch (e: Throwable) {
                postSideEffect(
                    CourseEditSideEffect.Toast(
                        SnackBarType.Error,
                        "AI生成的时间格式无效：${e.message}",
                    )
                )
                return@runOn
            }

            val generatedRecordInfo = if (record.isNullOrEmpty()) {
                state.recordInfo
            } else {
                val recordsOutsideCurrentTerm = state.recordInfo.filter {
                    it.startTime.date !in app.start..app.end
                }
                recordsOutsideCurrentTerm + generatedRecords
            }

            reduce {
                state.copy(
                    courseInfo = course?.toEntity(state.courseInfo) ?: state.courseInfo,
                    recordInfo = generatedRecordInfo,
                )
            }

            postSideEffect(
                CourseEditSideEffect.Toast(
                    type = SnackBarType.Success,
                    message = when {
                        course != null && record.isNullOrEmpty()
                            .not() -> "成功生成课程详情和 ${generatedRecords.size} 条课程时间。请检查后保存"

                        course == null && record.isNullOrEmpty()
                            .not() -> "未生成课程详情，但成功解析出 ${generatedRecords.size} 条课程时间。请检查后保存"

                        course != null && record.isNullOrEmpty() -> "成功生成课程详情，但未生成课程时间。请检查后保存"
                        else -> return@runOn
                    }
                )
            )

            generatedRecordInfo.timeValidationError()?.let {
                postSideEffect(
                    CourseEditSideEffect.Toast(
                        SnackBarType.Warning,
                        "生成结果存在无效或重叠的课程时间，修正前无法保存",
                    )
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
    fun toEntity(base: CourseEntity): CourseEntity = base.copy(
        name = name ?: base.name,
        teacherName = teacherName ?: base.teacherName,
        classroomName = classroomName ?: base.classroomName,
        credits = credits ?: base.credits,
        isDegreeRequired = isDegreeRequired ?: base.isDegreeRequired,
        isExaminable = isExaminable ?: base.isExaminable,
        isUserAdded = true,
    )
}

@Serializable
@SerialName("LLMRecordReturn")
@LLMDescription("课程时间安排记录")
private data class LLMRecordReturn(
    @property:LLMDescription("课程开始日期时间，严格使用yyyy-MM-ddTHH:mm格式，不含秒和时区")
    val startTime: String,
    @property:LLMDescription("课程结束日期时间，严格使用yyyy-MM-ddTHH:mm格式，不含秒和时区")
    val endTime: String,
) {
    fun toEntity(id: Long, courseId: Long?): CourseRecordEntity = CourseRecordEntity(
        id = id,
        courseId = courseId,
        startTime = LocalDateTime.parse(startTime),
        endTime = LocalDateTime.parse(endTime),
        isUserAdded = true,
    )
}

private fun List<CourseRecordEntity>.timeValidationError(): String? {
    if (any { it.startTime.date != it.endTime.date || it.startTime >= it.endTime }) {
        return "结束时间必须晚于开始时间，且二者必须在同一天"
    }

    for (firstIndex in indices) {
        for (secondIndex in firstIndex + 1 until size) {
            val first = this[firstIndex]
            val second = this[secondIndex]
            if (first.startTime < second.endTime && second.startTime < first.endTime) {
                return "课程时间不能重叠"
            }
        }
    }
    return null
}

private fun List<CourseRecordEntity>.invalidRecordIds(): Set<Long> = buildSet {
    this@invalidRecordIds.forEach { record ->
        if (record.startTime.date != record.endTime.date || record.startTime >= record.endTime) {
            record.id?.let(::add)
        }
    }

    for (firstIndex in indices) {
        for (secondIndex in firstIndex + 1 until size) {
            val first = this@invalidRecordIds[firstIndex]
            val second = this@invalidRecordIds[secondIndex]
            if (first.startTime < second.endTime && second.startTime < first.endTime) {
                first.id?.let(::add)
                second.id?.let(::add)
            }
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
    ) : CourseEditState {
        val invalidRecordIds: Set<Long>
            get() = recordInfo.invalidRecordIds()

        val canSave: Boolean
            get() = enableSaveButton && invalidRecordIds.isEmpty()
    }
}

sealed interface CourseEditSideEffect {
    data class Toast(val type: SnackBarType, val message: String) : CourseEditSideEffect
    data object NavigateBack : CourseEditSideEffect
}
