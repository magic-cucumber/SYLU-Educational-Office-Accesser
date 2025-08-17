# EOA-Lib - 教务系统网络API库

[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg)]()
[![Architecture](https://img.shields.io/badge/architecture-plugin--based-blue.svg)]()

EOA-Lib 是 SYLU-EOA 项目的核心网络API库，采用插件化架构设计，支持适配不同学校的教务系统。通过标准化的接口定义，可以轻松扩展支持新的教务系统。

## 📁 模块结构

```
eoa-lib/
├── network-core/          # 核心接口定义和数据模型
├── network-html-api/      # 沈阳理工大学HTML API实现
└── network-test-api/      # 测试API实现（示例数据）
```

### 🔧 network-core

核心模块，定义了所有教务系统适配器必须实现的标准接口和数据模型。

**主要组件：**

- `EOAClient` - 教务系统客户端核心接口
- `EOAClientProvider` - 客户端提供者接口
- `Storage` - 数据存储抽象接口
- 数据模型 Bean 类（UserProfile、ClassUnit、ExamItem等）
- 异常定义类

### 🌐 network-html-api

沈阳理工大学教务系统的HTML解析实现，通过逆向工程教务网页端获得。

**特性：**

- 基于Ktor Client的HTTP请求处理
- HTML文档解析和数据提取
- RSA加密登录支持
- 自动重试和会话管理
- 验证码处理机制

### 🧪 network-test-api

测试API实现，提供模拟数据用于开发和测试。

**用途：**

- 开发环境测试
- 功能演示
- 新功能验证
- 适配器开发参考

## 🏫 适配其他学校教务系统

### 快速开始

1. **创建新模块**

```bash
mkdir eoa-lib/network-xxx-api
cd eoa-lib/network-xxx-api
```

2. **配置 build.gradle.kts**

```kotlin
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":eoa-lib:network-core"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
```

3. **实现 EOAClient 接口**

```kotlin
class XXXEOAClient : EOAClient {
    private lateinit var storage: Storage
    override var username: String = ""
    override var password: String = ""

    override fun init(storage: Storage) {
        this.storage = storage
    }

    override suspend fun login(captchaHandler: (suspend (ByteArray) -> String)?) {
        // 实现您学校的登录逻辑
    }

    override suspend fun getUserProfile(): UserProfile {
        // 实现获取用户信息
    }

    override suspend fun getClassTable(picker: TermPicker): List<ClassUnit> {
        // 实现获取课程表
    }

    // ... 实现其他必需方法
}
```

4. **创建 Provider**

```kotlin
@ServiceProvider
object XXXEOAClientProvider : EOAClientProvider {
    override val id: String = "your.package.XXXEOAClientProvider"
    override val name: String = "您的学校名称"
    override val description: String = "适用于XXX大学教务系统"
    override val version: String = "1.0"

    override fun provide(): EOAClient = XXXEOAClient()
}
```

### 📋 核心接口说明

#### EOAClient 接口

教务系统客户端的核心接口，定义了所有必需的功能方法：

```kotlin
interface EOAClient {
    // 认证相关
    suspend fun login(captchaHandler: (suspend (ByteArray) -> String)? = null)
    suspend fun logout()

    // 用户信息
    suspend fun getUserProfile(): UserProfile

    // 学期和校历
    suspend fun getAllAvailableTerms(): TermResult
    suspend fun getSchoolCalender(): SchoolCalender

    // 课程相关
    suspend fun getClassTable(picker: TermPicker): List<ClassUnit>

    // 考试相关
    suspend fun getExamList(picker: TermPicker = TERM_ALL_PICKER): List<ExamItem>
    suspend fun getExamInfo(examItem: ExamItem): List<List<String>>

    // 成绩相关
    suspend fun getGPAScores(): List<GPAScoreSummary>
    suspend fun getGPAScoreList(summary: GPAScoreSummary): List<GPAScore>

    // 通知相关
    suspend fun getNotice(hasRead: Boolean = false): List<SystemNotice>
    suspend fun markNoticeReadable(noticeId: String): Boolean

    // 初始化和配置
    fun init(storage: Storage)
    var username: String
    var password: String
}
```

#### EOAClientProvider 接口

客户端提供者接口，用于注册和管理不同的教务系统适配器：

```kotlin
@Service
interface EOAClientProvider {
    val id: String          // 唯一标识符
    val name: String        // 显示名称
    val description: String // 描述信息
    val version: String     // 版本号

    fun provide(): EOAClient // 创建客户端实例
}
```

### 📊 核心数据模型

#### UserProfile - 用户信息

```kotlin
data class UserProfile(
    val name: String,           // 姓名
    val collegeName: String,    // 学院名称
    val studyName: String,      // 专业/年级
    val avatar: ByteArray,      // 头像数据
    val email: String,          // 邮箱
    val phone: String,          // 电话
    val id: String,            // 学号
    val policy: String,        // 政治面貌
    val language: String       // 语言
)
```

#### ClassUnit - 课程单元

```kotlin
data class ClassUnit(
    val name: String,              // 课程名称
    val teacher: String,           // 教师姓名
    val room: String,              // 教室
    val weekEachLesson: String,    // 上课周次 (如: "1-16")
    val lesson: String,            // 节次 (如: "1-2")
    val dayInWeek: String,         // 星期几 (1-7)
    val score: String,             // 学分
    val classType: String,         // 考核方式 (考试/考查)
    val isDegreeProgram: Boolean   // 是否学位课
)
```

#### ExamItem - 考试项目

```kotlin
data class ExamItem(
    val year: String,           // 学年
    val semester: String,       // 学期
    val detailsID: String,      // 详情ID
    val name: String,           // 课程名称
    val teacher: String,        // 教师姓名
    val credit: String,         // 学分
    val gradePoint: String,     // 绩点
    val absoluteScore: String,  // 绝对分数
    val relateScore: String,    // 相对分数/等级
    val completionCode: String, // 完成状态代码
    val degreeProgram: Boolean  // 是否学位课程
)
```

#### TermPicker - 学期选择器

```kotlin
data class TermPicker(
    private val yearName: Pair<String, String>,  // 学年名称和代码
    private val yearCode: Pair<String, String>   // 学期名称和代码
) {
    fun asTerm(): Term          // 获取学期代码
    fun asDisplay(): Term       // 获取显示名称
}

data class TermResult(
    val list: List<TermPicker>, // 所有可用学期
    val default: TermPicker     // 默认学期
)
```

#### GPAScoreSummary & GPAScore - 成绩相关

```kotlin
data class GPAScoreSummary(
    val name: String,    // 成绩类别名称
    val score: Double    // 总分
)

data class GPAScore(
    val name: String,    // 科目名称
    val score: String    // 分数
)
```

#### SystemNotice - 系统通知

```kotlin
data class SystemNotice(
    val createTime: LocalDateTime, // 创建时间
    val title: String,             // 标题
    val content: String,           // 内容
    val id: String                 // 通知ID
)
```

### 🔧 实现指南

#### 1. 网络请求处理

使用 Ktor Client 进行HTTP请求：

```kotlin
class XXXEOAClient : EOAClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    override suspend fun login(captchaHandler: (suspend (ByteArray) -> String)?) {
        val response = client.post("https://your-school.edu.cn/login") {
            setBody(FormDataContent(Parameters.build {
                append("username", username)
                append("password", password)
            }))
        }
        // 处理登录响应
    }
}
```

#### 2. 数据解析

根据您学校的API格式选择合适的解析方式：

**JSON API 解析：**

```kotlin
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T
)

override suspend fun getUserProfile(): UserProfile {
    val response = client.get("api/user/profile")
        .body<ApiResponse<UserProfileDto>>()

    return response.data.toUserProfile()
}
```

**HTML 解析：**

```kotlin
override suspend fun getClassTable(picker: TermPicker): List<ClassUnit> {
    val html = client.get("schedule.html").bodyAsText()
    val document = Jsoup.parse(html)

    return document.select(".course-item").map { element ->
        ClassUnit(
            name = element.select(".course-name").text(),
            teacher = element.select(".teacher").text(),
            // ... 其他字段解析
        )
    }
}
```

#### 3. 异常处理

使用项目定义的标准异常：

```kotlin
override suspend fun login(captchaHandler: (suspend (ByteArray) -> String)?) {
    try {
        val response = client.post("login") { /* ... */ }

        when (response.status) {
            HttpStatusCode.OK -> {
                // 登录成功
            }
            HttpStatusCode.Unauthorized -> {
                throw BadCredentialsException()
            }
            HttpStatusCode.Forbidden -> {
                // 可能需要验证码
                val captchaImage = getCaptchaImage()
                val captchaText = captchaHandler?.invoke(captchaImage)
                    ?: throw NeedCaptchaException()
                // 重新登录
            }
            else -> {
                throw UnknownException("登录失败: ${response.status}")
            }
        }
    } catch (e: Exception) {
        when (e) {
            is EOAClientException -> throw e
            else -> throw UnknownException("网络错误", e)
        }
    }
}
```

#### 4. 会话管理

实现自动重试和会话保持：

```kotlin
class XXXEOAClient : EOAClient {
    private var isLoggedIn = false

    private suspend fun ensureLoggedIn() {
        if (!isLoggedIn) {
            login()
            isLoggedIn = true
        }
    }

    private suspend fun <T> withRetry(block: suspend () -> T): T {
        return try {
            ensureLoggedIn()
            block()
        } catch (e: UnauthorizedException) {
            // 会话过期，重新登录
            isLoggedIn = false
            ensureLoggedIn()
            block()
        }
    }

    override suspend fun getUserProfile(): UserProfile = withRetry {
        // 实际的API调用
    }
}
```

### 🧪 测试您的实现

#### 单元测试示例

```kotlin
class XXXEOAClientTest {
    private val client = XXXEOAClient()

    @Test
    fun testLogin() = runBlocking {
        client.init(MemoryStorage())
        client.username = "test"
        client.password = "test"

        assertDoesNotThrow {
            client.login()
        }
    }

    @Test
    fun testGetUserProfile() = runBlocking {
        client.init(MemoryStorage())
        client.login()

        val profile = client.getUserProfile()
        assertNotNull(profile.name)
        assertNotNull(profile.id)
    }
}

class MemoryStorage : Storage {
    private var data: String? = null

    override fun get(): String? = data
    override fun set(value: String) {
        data = value
    }
}
```

### 📝 最佳实践

#### 1. 错误处理

- 使用标准异常类型
- 提供详细的错误信息
- 实现适当的重试机制

#### 2. 性能优化

- 缓存不变的数据（如学期列表）
- 使用连接池复用HTTP连接
- 实现请求去重

#### 3. 安全考虑

- 不在日志中输出敏感信息
- 使用HTTPS进行通信
- 正确处理Cookie和会话

#### 4. 兼容性

- 处理API版本变化
- 适配不同的数据格式
- 提供降级方案

### 🔗 集成到主应用

1. **添加模块依赖**

在 `composeApp/build.gradle.kts` 中：

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(project(":eoa-lib:network-xxx-api"))
    }
}
```

2. **注册模块**

在 `settings.gradle.kts` 中：

```kotlin
include(":eoa-lib:network-xxx-api")
```

3. **验证集成**

启动应用后，新的教务系统选项将自动出现在登录页面的后端选择列表中。

### 📚 参考资源

- **核心接口定义**: `network-core/src/commonMain/kotlin/top/kagg886/sylu_eoa/api/v2/`
- **HTML实现参考**: `network-html-api/src/commonMain/kotlin/top/kagg886/sylu_eoa/api/html/`
- **测试实现参考**: `network-test-api/src/commonMain/kotlin/top/kagg886/sylu_eoa/api/test/`
- **异常定义**: `network-core/src/commonMain/kotlin/top/kagg886/sylu_eoa/api/v2/exceptions.kt`

### 🤝 贡献

欢迎为EOA-Lib贡献新的教务系统适配器！请确保：

1. 遵循现有的代码风格和架构
2. 提供完整的测试用例
3. 更新相关文档
4. 处理边界情况和异常

---

**注意**: 在实现新的适配器时，请确保遵守目标学校的服务条款和使用政策。

```
