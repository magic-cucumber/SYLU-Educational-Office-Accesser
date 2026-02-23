package top.kagg886.eoa.pages.main.home.course.manage.edit

import androidx.lifecycle.ViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readBytes
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.annotation.OrbitExperimental
import org.orbitmvi.orbit.viewmodel.container
import top.kagg886.backend.config.AppAiMMKV
import top.kagg886.backend.config.AppSyncMMKV
import top.kagg886.backend.database.AppDatabase
import top.kagg886.backend.database.dao.CourseEntity
import top.kagg886.backend.database.dao.CourseRecordEntity
import top.kagg886.eoa.util.SnackBarType
import top.kagg886.util.asKtorLogger
import top.kagg886.util.asTaggedLogger
import top.kagg886.util.http.HttpClient
import kotlin.io.encoding.Base64
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

    fun generateCourseByAI(it: String) = intent {
        postSideEffect(
            CourseEditSideEffect.Toast(
                SnackBarType.Info,
                "正在生成，请稍等.."
            )
        )

        val sub = generateCourseByAIInternal(
            JsonPrimitive("请解析以下课程描述并输出符合 CourseAddReturn 的 JSON:\n$it")
        )

        sub.join()
    }

    fun generateCourseByImage() = intent {
        val picker = FileKit.openFilePicker(type = FileKitType.Image, title = "选择AI要读取的图片")

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

        val sub = generateCourseByAIInternal(
            buildJsonArray {
                addJsonObject {
                    put("type", "text")
                    put("content", "请理解图片中的内容，并输出符合 CourseAddReturn 的 JSON:")
                }

                addJsonObject {
                    put("type", "image_url")
                    putJsonObject("image_url") {
                        put("url", "data:image/png;base64,${Base64.encode(byt)}")
                    }
                }
            }
        )

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
    private fun generateCourseByAIInternal(build: JsonElement) = intent {
        runOn<CourseEditState.Success> {
            reduce {
                state.copy(
                    enableSaveButton = false,
                    aiGenerating = ""
                )
            }

            val agent = HttpClient {
                install(Logging) {
                    logger = this@CourseEditModel.logger.asKtorLogger
                    level = LogLevel.ALL
                }

                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                        }
                    )
                }

                defaultRequest {
                    url(state.aiEndpoint)
                    headers {
                        append("Authorization", "Bearer ${state.aiKey}")
                    }
                }
            }
            addCloseable(agent)

            val (course, record) = try {
                val app = AppSyncMMKV.calender!!
                val resp = agent.post("/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("model", state.aiModel)
                        put("stream", false)
                        putJsonArray("messages") {
                            add(buildJsonObject {
                                put("role", "system")
                                put(
                                    "content", """
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
                            })

                            add(buildJsonObject {
                                put("role", "user")
                                put("content", build)
                            })

                            add(buildJsonObject {
                                put("role", "user")
                                put(
                                    "content", $$"""
                                    ## NEXT MESSAGE OUTPUT FORMAT
                                    The output in the next message MUST ADHERE TO CourseAddReturn format.

                                    ### DEFINITION OF CourseAddReturn
                                    The CourseAddReturn format is defined only and solely with JSON, without any additional characters, comments, backticks or anything similar.

                                    You must adhere to the following JSON schema:
                                    {
                                        "$id": "CourseAddReturn",
                                        "$defs": {
                                            "LLMCourseReturn?": {
                                                "type": "object",
                                                "properties": {
                                                    "name": {
                                                        "type": [
                                                            "string",
                                                            "null"
                                                        ],
                                                        "description": "课程名称，如'高等数学'"
                                                    },
                                                    "teacherName": {
                                                        "type": [
                                                            "string",
                                                            "null"
                                                        ],
                                                        "description": "任课教师姓名，如'张教授'"
                                                    },
                                                    "classroomName": {
                                                        "type": [
                                                            "string",
                                                            "null"
                                                        ],
                                                        "description": "上课教室，如'教学楼A101'"
                                                    },
                                                    "credits": {
                                                        "type": [
                                                            "number",
                                                            "null"
                                                        ],
                                                        "description": "课程学分，如2.0"
                                                    },
                                                    "isDegreeRequired": {
                                                        "type": [
                                                            "boolean",
                                                            "null"
                                                        ],
                                                        "description": "是否为学位课，true表示学位课"
                                                    },
                                                    "isExaminable": {
                                                        "type": [
                                                            "boolean",
                                                            "null"
                                                        ],
                                                        "description": "是否为考试课，true标识考试课"
                                                    }
                                                },
                                                "required": [
                                                    "name",
                                                    "teacherName",
                                                    "classroomName",
                                                    "credits",
                                                    "isDegreeRequired",
                                                    "isExaminable"
                                                ],
                                                "additionalProperties": false,
                                                "description": "课程基本信息"
                                            },
                                            "LLMRecordReturn": {
                                                "type": "object",
                                                "properties": {
                                                    "weekNumber": {
                                                        "type": "integer",
                                                        "description": "具体的周数，表示这门课在第几周有课，从1开始计数。对于时间范围（如第3-16周），需要为每一周都创建一个独立的record。"
                                                    },
                                                    "dayOfWeek": {
                                                        "type": "array",
                                                        "items": {
                                                            "type": "integer"
                                                        },
                                                        "description": "dayOfWeek属性，在星期几会有这门课，1=周一，2=周二，...，7=周日"
                                                    },
                                                    "periodOfDay": {
                                                        "type": "array",
                                                        "items": {
                                                            "type": "array",
                                                            "items": {
                                                                "type": "integer"
                                                            }
                                                        },
                                                        "description": "一个二维矩阵，纵坐标为dayOfWeek属性的index，横坐标为在第几节会有这门课。"
                                                    }
                                                },
                                                "required": [
                                                    "weekNumber",
                                                    "dayOfWeek",
                                                    "periodOfDay"
                                                ],
                                                "additionalProperties": false,
                                                "description": "课程时间安排记录"
                                            },
                                            "CourseAddReturn": {
                                                "type": "object",
                                                "properties": {
                                                    "course": {
                                                        "oneOf": [
                                                            {
                                                                "$ref": "#/$defs/LLMCourseReturn?"
                                                            },
                                                            {
                                                                "type": "null"
                                                            }
                                                        ],
                                                        "description": "课程基本信息"
                                                    },
                                                    "record": {
                                                        "type": [
                                                            "array",
                                                            "null"
                                                        ],
                                                        "items": {
                                                            "$ref": "#/$defs/LLMRecordReturn"
                                                        },
                                                        "description": "课程时间安排列表"
                                                    }
                                                },
                                                "required": [
                                                    "course",
                                                    "record"
                                                ],
                                                "additionalProperties": false,
                                                "description": "课程添加返回结果，包含课程基本信息和时间安排"
                                            }
                                        },
                                        "$ref": "#/$defs/CourseAddReturn"
                                    }

                                    #### EXAMPLES
                                    Here is an example of a valid response:
                                    ```
                                    {
                                        "course": {
                                            "name": "艺术鉴赏",
                                            "classroomName": "A栋302"
                                        },
                                        "record": [
                                            {
                                                "weekNumber": 3,
                                                "dayOfWeek": [
                                                    3
                                                ],
                                                "periodOfDay": [
                                                    [
                                                        3,
                                                        4
                                                    ]
                                                ]
                                            },
                                            {
                                                "weekNumber": 4,
                                                "dayOfWeek": [
                                                    3
                                                ],
                                                "periodOfDay": [
                                                    [
                                                        3,
                                                        4
                                                    ]
                                                ]
                                            },
                                            {
                                                "weekNumber": 5,
                                                "dayOfWeek": [
                                                    3
                                                ],
                                                "periodOfDay": [
                                                    [
                                                        3,
                                                        4
                                                    ]
                                                ]
                                            }
                                        ]
                                    }
                                    ```
                                    ## RESULT
                                    Provide ONLY the resulting JSON, WITHOUT ANY free text comments, backticks, or other symbols.
                                    Output should start with { and end with }

                                """.trimIndent()
                                )
                            })
                        }
                    })
                }.body<JsonObject>()


                val respString =
                    resp["choices"]?.jsonArray[0]?.jsonObject["message"]?.jsonObject["content"]?.jsonPrimitive?.content
                        ?: error("找不到预期的返回。\n$resp")

                Json.decodeFromString<CourseAddReturn>(respString)
            } catch (ex: Exception) {
                logger.w("无法生成课表数据", ex)
                postSideEffect(
                    CourseEditSideEffect.Toast(
                        SnackBarType.Error,
                        ex.message ?: "生成课表数据时发生错误，请查看日志"
                    )
                )
                return@runOn
            } finally {
                reduce {
                    state.copy(
                        enableSaveButton = true,
                        aiGenerating = null
                    )
                }
                agent.close()
            }

            reduce {
                state.copy(
                    enableSaveButton = true,
                    aiGenerating = null,
                )
            }

            if (course == null) {
                postSideEffect(
                    CourseEditSideEffect.Toast(
                        SnackBarType.Success,
                        "未检索到有效的课程数据。"
                    )
                )
                return@runOn
            }

            if (record.isNullOrEmpty() || record.flatMap { it.toEntity() }.isEmpty()) {
                postSideEffect(
                    CourseEditSideEffect.Toast(
                        SnackBarType.Success,
                        "成功填写课程，但未检索到有效的课表数据。"
                    )
                )
                return@runOn
            }

            logger.i("生成数据：${course},${record}")

            postSideEffect(
                CourseEditSideEffect.Toast(
                    SnackBarType.Success,
                    "共生成了 ${record.flatMap { it.toEntity() }.size} 个课表，请查阅后点击确定。"
                )
            )

            reduce {
                state.copy(
                    courseInfo = with(AppSyncMMKV.picker!!.default.asTerm()) {
                        course.toEntity(xnm, xqm)
                    },
                    recordInfo = record.flatMap { it.toEntity() }
                )
            }
        }
    }
}

@Serializable
@SerialName("CourseAddReturn")
private data class CourseAddReturn(
    val course: LLMCourseReturn?,
    val record: List<LLMRecordReturn>?
)

@Serializable
@SerialName("LLMCourseReturn")
private data class LLMCourseReturn(
    val name: String?,
    val teacherName: String?,
    val classroomName: String?,
    val credits: Float?,
    val isDegreeRequired: Boolean?,
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
private data class LLMRecordReturn(
    val weekNumber: Int, // 学期周数(从1开始)
    val dayOfWeek: List<Int>, // 星期几(1-7)
    val periodOfDay: List<List<Int>>, // 第几节课
) {
    fun toEntity(): List<CourseRecordEntity> = periodOfDay.withIndex().flatMap { (index, periods) ->
        periods.map { it ->
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
