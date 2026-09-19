# App Store 拒审风险审计

审计日期：2026-09-17。依据：app-store-rejection-checker 技能及本次获取的 Apple 在线规则。

## 摘要

SYLU-EOA 是面向高校用户的第三方教务客户端，使用 Kotlin Multiplatform / Compose 与 SwiftUI，提供课程、成绩、考试、第二课堂、日历导出、Widget、图片分享扩展和可选 AI 课程识别。当前源码存在需在提交前处理的明显风险，前三项是 **HTTP 明文密码传输、敏感日志进入诊断上传包、日历授权在新系统失效**；另有隐私入口、学校服务授权及赞赏/AI 告知风险。

本次审计针对当前工作区的 iOS/iPadOS 主应用及两个扩展，包含尚未提交的配置变化；Android、桌面端及其发布规则不在范围内。未构建或验证提交用 Archive/IPA，因此没有使用 `validator-certain`，本报告不保证审核通过。

## 风险发现

### 1. [1.6] 第二课堂内网登录通过 HTTP 提交原始密码

Severity: 高

Confidence: review-risk

Evidence:

- `composeApp/src/commonMain/kotlin/top/kagg886/eoa/pages/main/home/second/model.kt:121` 的 `loginByInternal()` 使用 `http://xg.${BuildConfig.MESSAGE_API_ENDPOINT}/SyluTW/Sys/`，随后调用 `tw.login(tPassword)`。
- `second-class/src/commonMain/kotlin/top/kagg886/eoa/second/TWUser.kt:93` 的登录表单同时提交 `UserName`、原始 `Password=pass` 和 RSA 加密的 `pwd`。加密另一个字段不能保护仍然存在的原始密码。
- 审计时 `iosApp/iosApp/Info.plist:18` 设置 `NSAllowsArbitraryLoads=true`，该请求未被 ATS 默认策略阻止；外网另有 HTTPS WebVPN 路径，不能覆盖内网分支。

Why it matters: [1.6 数据安全](https://developer.apple.com/app-store/review/guidelines/#data-security)要求采取 “appropriate security measures”。校园内网也可能被监听；政策里的 HTTP 风险提示不能代替传输保护。

Fix: 使用经验证可用的 HTTPS 学校接口或现有 HTTPS WebVPN 通道；服务器不支持安全传输时停用该明文登录路径。按学校协议核实并移除不必要的原始密码字段，收紧 ATS 到必要域名，验证请求与重定向全程不降级。

> ## 处理进度：
> 已完成以下处理：
>
> - 在 `iosApp/iosApp/Info.plist` 移除全量 `NSAllowsArbitraryLoads`，改为仅对 `xg.sylu.edu.cn` 配置 `NSExceptionAllowsInsecureHTTPLoads=true`，未启用子域名豁免。
> - 通过 `plutil -lint iosApp/iosApp/Info.plist` 与 `git diff --check` 验证配置格式及补丁内容。
> - 按校园网现状保留第二课堂内网 HTTP 路由；原始 `Password` 字段及 HTTP 传输仍属于残余风险，ATS 配置只能收窄系统网络豁免范围，不能提供传输加密。
> - 为 `jxw.sylu.edu.cn` 增加域名级 ATS 例外，仅设置 `NSExceptionRequiresForwardSecrecy=false` 以兼容学校教务 HTTPS 服务当前不支持 PFS 的 TLS 套件；未设置 `NSExceptionMinimumTLSVersion`，未允许该域名 HTTP 明文加载。
> - 已核对 Apple ATS 文档：会触发额外 App Store Review 说明的键包括 `NSAllowsArbitraryLoads`、`NSAllowsArbitraryLoadsForMedia`、`NSAllowsArbitraryLoadsInWebContent`、`NSExceptionAllowsInsecureHTTPLoads`、`NSExceptionMinimumTLSVersion`；单独设置 `NSExceptionRequiresForwardSecrecy=false` 未被列为触发项。因此本次 `jxw.sylu.edu.cn` PFS 兼容配置不需要额外追加 App Review Notes。
>
> 后续在 App Store Connect 后台提审时，在“App Review Information → Notes”中说明以下情景：
>
> ```text
> The Second Classroom feature accesses the university's existing on-campus service.
> When the device is connected to the campus network, this legacy service is available only through its HTTP endpoint at xg.sylu.edu.cn, so the app must retain this route for on-campus users.
>
> This is a domain-specific ATS exception, not a global exception: NSAllowsArbitraryLoads is disabled, and insecure HTTP is allowed only for xg.sylu.edu.cn. All off-campus access uses the university WebVPN over HTTPS.
>
> The HTTP endpoint is used only for the Second Classroom login and data retrieval flow. Please use the provided demo account and the WebVPN flow when reviewing from outside the campus network.
> ```
>
> 中文说明：
>
> ```text
> “第二课堂”功能访问学校现有的校园网服务。
> 该历史服务仅在手机连接校园网时可用，其服务端点 xg.sylu.edu.cn 仅通过 HTTP 提供，因此应用必须为校园网用户保留该路由。
>
> 这是针对单一域名的 ATS 豁免，而不是全局豁免：NSAllowsArbitraryLoads 已禁用，仅允许 xg.sylu.edu.cn 使用不安全的 HTTP。所有校外访问均通过 HTTPS 连接学校 WebVPN。
>
> HTTP 域名 xg.sylu.edu.cn 仅在手机处于校园网环境中时可用，离开校园网后无法通过公网访问。该 HTTP 端点仅用于第二课堂登录和数据获取流程。若审核时不在校园网，请使用已提供的测试账号和 WebVPN 流程进行审核。
> ```

### 2. [1.6 / 5.1.1(ii)(iii)] 网络日志未经脱敏进入崩溃报告，与授权文案冲突

Severity: 高

Confidence: review-risk

Evidence:

- `second-class/src/commonMain/kotlin/top/kagg886/eoa/second/TWUser.kt:57` 无条件启用 `LogLevel.ALL`，同一客户端在第 98 行提交原始密码；本科、研究生客户端也启用完整日志。
- `composeApp/src/commonMain/kotlin/top/kagg886/eoa/pages/main/model.kt:83` 和 `pages/main/settings/ai/manage/edit/model.kt:65` 为携带 API Key 的 AI 请求配置完整日志，未见 `sanitizeHeader` 或正文脱敏。
- `composeApp/src/iosMain/kotlin/main.kt:80` 注册数据库日志；`composeApp-backend/src/commonMain/kotlin/top/kagg886/eoa/util/logger.kt:32` 原样保存 message/stacktrace；`composeApp-backend/src/commonMain/kotlin/top/kagg886/backend/database/dao/log.kt:57` 原样读取。
- `crashApp/src/commonMain/kotlin/top/kagg886/report/AppModel.kt:440` 将这些日志直接写入待上传数据库。虽然第 360–435 行已处理部分业务数据和模型配置，日志复制路径没有对应处理。
- `composeApp/src/commonMain/kotlin/top/kagg886/eoa/pages/welcome/collect/screen.kt:94` 承诺“账号、密码等凭据会被脱敏，不会被收集”；`composeApp/src/commonMain/composeResources/drawable/privacy.md:29` 却承认原始日志可能包含敏感内容。

Why it matters: [1.6、5.1.1(ii)(iii)](https://developer.apple.com/app-store/review/guidelines/#privacy)涉及安全、知情同意与数据最小化。此处是可追踪的凭据泄露通路，不只是匿名诊断统计；上传加密不消除接收端解密后的泄露风险。

Fix: Release 禁止记录登录和 AI 请求的敏感头、请求体及响应体；在日志落库和诊断打包处增加统一脱敏与允许字段清单。用虚构密码、Cookie、API Key 验证本地日志及解密后的诊断包均不含原值，并统一授权文案与政策。

限定：`AppSettingsMMKV.kt:48` 的崩溃上传默认关闭，`AppModel.kt:111` 检查开关；不是“未经同意默认上传”。风险在于开启后上传内容超出界面承诺，实际日志还受运行时日志级别影响。

> ## 处理进度：
> 当前已完成风险审计与问题定位，尚未完成 Release 日志限制、统一脱敏及诊断包验证；问题未修复。

### 3. [2.1 / 2.5.1] 日历导出仍使用已失效的旧授权流程

Severity: 高

Confidence: review-risk

Evidence:

- `lib/calender-exporter-v2/src/iosMain/kotlin/top/kagg886/calendar/v2/compose.ios.kt:22` 只检查 `NSCalendarsUsageDescription`；第 34 行仍调用 `requestAccessToEntityType`。
- 该回调在 `granted=false` 时只记录日志并返回，不更新 `CalendarState.Processing`。
- `iosApp/iosApp.xcodeproj/project.pbxproj:751` 的 Release 配置只有旧日历用途键，没有 `NSCalendarsFullAccessUsageDescription`。
- `composeApp/src/commonMain/kotlin/top/kagg886/eoa/pages/main/home/course/export_calender/screen.kt:64` 实际使用此授权状态，第 66 行将 Processing 显示为“正在申请权限...”。

Why it matters: [2.1、2.5.1](https://developer.apple.com/app-store/review/guidelines/#performance)要求功能可用及系统兼容。Apple 的 [EventKit 授权 API 文档](https://developer.apple.com/documentation/eventkit/ekeventstore/requestaccess%28to%3Acompletion%3A%29)说明旧方法在 iOS 17 起不再弹窗并直接返回错误；这里还会留下持续加载状态。

Fix: 依据 [TN3152](https://developer.apple.com/documentation/technotes/tn3152-migrating-to-the-latest-calendar-access-levels)迁移至完整读写授权 API、用途键和状态判断；代码会读取、更新已有事件，因此不能仅换成只写权限。所有失败、拒绝路径都应结束加载；若支持旧系统则保留版本分支。真机验证首次允许、拒绝、撤销权限和再次导出。

限定：这是源码与官方 API 行为支持的功能风险，未作上传必定被拦截或真机已复现的断言。

> ## 处理进度：
> 已完成 EventKit 授权迁移，问题已修复：
>
> - `rememberCalendarManagerState` 现在通过 `remember` 持有单一 `EKEventStore`，并在授权检查中按系统版本选择新旧 EventKit API。
> - 新系统使用 `requestFullAccessToEventsWithCompletion`，仅接受 `EKAuthorizationStatusFullAccess`；`WriteOnly`、拒绝、限制和回调错误均映射为 `CalendarState.Denied`，不会继续停留在 `Processing`。
> - 主 App 的 Debug/Release 配置新增 `NSCalendarsFullAccessUsageDescription`，同时保留旧用途键；缺少对应用途键会进入 `NotSupported`。
> - 授权回调改为可取消协程等待，统一处理授权失败和错误日志；上层 Compose 页面及导出模型无需修改。
> - 已删除 `#Preview` 宏；恢复 `RefreshTodayCourseWidgetIntent` 作为 Widget 刷新动作，并用 `if #available(iOS 17.0, *)` 保护 `Button(intent:)`，低于 iOS 17 时隐藏刷新按钮。
> - 审核影响：预览宏不会进入生产包；刷新按钮只在支持 AppIntent 交互的系统显示，避免旧系统出现无效按钮，不新增权限、隐私采集或商店元数据要求。
>
> 已通过 `:lib:calender-exporter-v2:compileKotlinIosArm64`、`:lib:calender-exporter-v2:compileKotlinIosSimulatorArm64` 和 `:composeApp:linkDebugFrameworkIosArm64` 编译验证；尚未完成真机授权弹窗、撤销权限和再次导出的运行时验证。

### 4. [5.1.1(i)] 完成首次引导后缺少易访问的隐私政策入口

Severity: 中

Confidence: review-risk

Evidence:

- `composeApp/src/commonMain/kotlin/top/kagg886/eoa/pages/welcome/privacy/screen.kt:55` 有完整隐私政策资源，入口位于首次引导。
- `composeApp/src/commonMain/kotlin/top/kagg886/eoa/pages/welcome/model.kt:36` 对已初始化用户跳过引导。
- 检查 `pages/main/settings/list/screen.kt:61` 的导航及设置列表、`pages/main/about/screen.kt:126` 的关于页操作，未发现政策入口；全局政策资源/路由引用也仅在 welcome 下。

Why it matters: [5.1.1(i)](https://developer.apple.com/app-store/review/guidelines/#privacy)要求应用内政策处于 “easily accessible manner”。仅首次显示不足以支持日后查阅、授权撤回和删除请求。

Fix: 在设置或关于页增加常驻隐私政策、用户协议入口，允许未登录用户访问；同时在 App Store Connect 填写对应公开政策 URL。后者本次未取得，不能断言线上已缺失。

> ## 处理进度：
> 已在关于页添加“隐私政策与用户协议”入口，点击后通过 Bottom Sheet 展示两个 Tab 及对应 Markdown，功能已测试并通过 JVM 编译验证；问题已修复。

### 5. [5.2.2] 学校服务访问授权需要提交证据

Severity: 中

Confidence: review-risk

Evidence:

- `composeApp/src/commonMain/composeResources/drawable/user.md:11` 明确声明与学校及相关系统不存在官方授权关系。
- `eoa-lib/network-html-api/src/commonMain/kotlin/top/kagg886/sylu_eoa/api/html/EOAHTMLClient.kt`、`eoa-lib/network-graduate-api/src/commonMain/kotlin/top/kagg886/sylu_eoa/api/graduate/EOAGraduateClient.kt` 及 `second-class` 实现第三方系统登录、内容解析和数据访问。
- 用户协议第 29 行取得的是用户对账号访问的同意；本次未发现学校服务条款许可或授权材料。

Why it matters: [5.2.2](https://developer.apple.com/app-store/review/guidelines/#intellectual-property)要求服务条款允许该访问，并可应要求提供授权。用户账号授权与服务方许可是不同事项；“非官方”声明本身不等于违规，也不替代后者。

Fix: 核对学校系统条款并保存适用许可依据；必要时取得学校书面许可，在审核备注提供可核验材料。当前结论是授权证据缺口，不是已确认侵权。

> ## 处理进度：
>
> 当前已完成风险审计与证据缺口记录，尚未核实学校服务条款或取得授权材料；风险未解除。

### 6. [3.1.1 / 3.2.1(vii)] iOS 共用页面存在站外赞赏码

Severity: 中

Confidence: judgment-call

Evidence:

- `composeApp/src/commonMain/kotlin/top/kagg886/eoa/pages/main/about/screen.kt:143` 提供“赞赏我”，第 251 行弹窗显示 `Res.drawable.good`。
- `composeApp/src/commonMain/kotlin/top/kagg886/eoa/pages/welcome/done/screen.kt:167` 提供“捐赠作者”“支持项目持续更新”，也展示同一图片。
- 已查看 `composeApp/src/commonMain/composeResources/drawable/good.jpg`，确为站外赞赏码；上述代码没有 iOS 排除条件。未发现 IAP 实现，也未发现赞赏后解锁权益。

Why it matters: [3.1.1、3.2.1(vii)](https://developer.apple.com/app-store/review/guidelines/#business)区分开发者打赏、数字服务与纯个人赠与。此入口与应用维护相联系，能否适用个人赠与例外存在判断空间；不能仅因有二维码就断言违反 IAP。

Fix: 最低风险方案是从 iOS 移除两个入口。若保留，须先核实收款人为个人、全部款项 100% 归该个人、打赏完全自愿且不解锁或关联任何数字内容/服务，并在审核备注解释；不满足这些条件时应改用合适的 IAP 或移除入口。

> ## 处理进度：
>
> 已完成风险审计，并准备在 App Review 备注中追加以下说明。注意：仅在提交前确认二维码收款人确为个人开发者、每笔款项 100% 归该个人且不提供任何数字权益时，才应使用该声明。
>
> **Additional App Review Remark (English):**
>
> This app includes an optional “Donate to the author” entry that displays a personal WeChat appreciation QR code. This is a completely voluntary person-to-person monetary gift to the individual developer. It is not a purchase, subscription, or payment for digital content or services. Donating does not unlock or enable any feature, content, functionality, account status, badge, priority, or other benefit; all app features remain available without donating. No digital content or service is provided in exchange for a gift, and 100% of each gift goes to the individual recipient. We understand Guideline 3.2.1(vii) to permit optional monetary gifts from one individual to another without In-App Purchase under these conditions. If App Review determines that this flow is not covered by that guideline, we will remove the donation entries from the iOS build.
>
> **中文翻译：**
>
> 本 App 提供一个可选的“捐赠作者”入口，显示作者个人的微信赞赏二维码。这是用户自愿向个人开发者进行的个人间货币赠与，不是购买、订阅，也不是购买数字内容或数字服务。捐赠不会解锁或启用任何功能、内容、账户状态、徽章、优先权或其他权益；不捐赠时，App 的全部功能仍然可用。捐赠不会换取任何数字内容或服务，且每笔赠与金额的 100% 均归个人收款人所有。我们理解，按照审核指南 3.2.1(vii)，在满足这些条件时，个人之间的自愿货币赠与无需使用 App 内购买项目。如果 App Review 认为该流程不属于该条款的适用范围，我们将从 iOS 版本中移除赞赏入口。
>
> **处理结果：**
>
> 已追加审核备注草案；代码尚未移除 iOS 入口，也未实现 IAP。当前结论不是“二维码必然违规”，而是“可能适用 3.2.1(vii)，但收款主体、100% 到账及无数字权益关联仍需提交前核实”；在 Apple 明确接受前，审核风险仍保留。

### 7. [5.1.2(i)] 后续从设置启用 AI 时，数据共享告知不足

Severity: 中

Confidence: judgment-call

Evidence:

- `composeApp-backend/src/commonMain/kotlin/top/kagg886/backend/config/AppSettingsMMKV.kt:50` 默认禁用 AI；首次引导 `pages/welcome/collect/screen.kt:113` 提示将数据发送给自行配置的模型商，政策第 1.4 节也有具体说明。
- 但 `composeApp/src/commonMain/kotlin/top/kagg886/eoa/pages/main/settings/ai/summary/screen.kt:71` 后续启用入口只显示“启用AI / 关闭后将停用 AI 相关功能”，没有重新呈现数据类型、接收方或共享确认。
- `pages/main/home/course/manage/edit/model.kt:208`、第 224 行将用户课程文字、所选图片用于模型请求，可能包含姓名、学号、教师和个人日程；它不是纯本地识别。

Why it matters: [5.1.2(i)](https://developer.apple.com/app-store/review/guidelines/#data-use-and-sharing)要求明确告知第三方个人数据共享并取得 “explicit permission”。已有首次告知和开关是保护措施；风险集中在首次拒绝、日后开启或更换接收方时，普通功能开关能否充分表达共享授权。

Fix: 在后续首次启用/首次向新服务发送前，明确展示接收方域名及将发送的文字、图片和学期信息，提供明确同意/取消；保留撤回入口，在执行请求处检查授权状态。固定测试请求与真实个人数据请求分开说明，无需每次都重复弹窗。

> ## 处理进度：
> 已完成后续从设置启用 AI 时的数据共享告知与明确确认：
>
> - `composeApp/src/commonMain/kotlin/top/kagg886/eoa/pages/main/settings/ai/summary/screen.kt:97-179` 使用 `BasicAlertDialog` 展示启用前说明，明确告知由用户配置的第三方 AI 服务商处理，并说明会上传图片、输入文本和本学期起止日期；同时说明 EOA 不存储数据及可随时关闭 AI。
> - `screen.kt:87-94` 在对话框打开后执行 3 秒倒计时；`screen.kt:184-200` 倒计时期间确认按钮 disabled，结束后才显示“我已了解，启用 AI”。
> - `screen.kt:184-189` 仅在用户点击确认后调用 `onEnableAIChanged(true)`；取消、返回或关闭对话框不会启用 AI。`screen.kt:236-245` 仅在开关从关闭切换为开启时弹窗，关闭 AI 则直接撤回启用状态。
>
> 根据当前 [Apple App Review Guidelines 5.1.2(i)](https://developer.apple.com/app-store/review/guidelines/#data-use-and-sharing)，要求是在向第三方（包括第三方 AI）共享个人数据前，清楚披露共享对象和用途并取得明确许可；没有要求每次 AI 请求前重复弹窗。现有首次引导说明与本次“启用时”确认共同覆盖该风险，后续每次生成无需重复确认。

## 已检查但不新增拒审结论

- **4.2 / 4.3**：有本地数据库、课程编辑、成绩/考试展示、导出和原生扩展，不具备单一 WebView 网站包装特征；未见足够证据认定最低功能或重复应用违规。
- **4.8 / 5.1.1(v)**：登录学校既有账号，未发现应用自行注册账号或社交登录流程；不据此要求新增 Apple 登录或学校账号注销功能。
- **5.4**：`VPNClient.kt:44` 使用学校 HTTPS WebVPN 网页网关；未发现系统隧道或 `NEVPNManager` 服务，不能仅凭 VPN 命名按独立 VPN 产品处理。
- **权限与扩展**：照片保存用途键已见于主应用构建配置及 ImageProcessing；三个目标的 App Group 字符串一致。未见明确的相机、录音、定位、ATT、推送缺失用途键触发点；最终依赖和签名仍需成品核验。
- **账号/审核访问**：`eoa-lib/network-test-api/src/commonMain/kotlin/top/kagg886/sylu_eoa/api/test/EOAClientProvider.impl.kt:8` 已定义“测试API”，说明账号/密码均为 `test`，主模块包含此依赖、登录页有后端选择。不能报告“完全没有演示模式”；是否进入 Release、覆盖第二课堂与 AI、是否已向 Apple 说明仍未验证。[2.1 审核访问要求](https://developer.apple.com/app-store/review/guidelines/#app-completeness)应在提交前落实。
- **动态内容**：公告、链接、更新及反馈配置可远端获取；未见据此下载执行代码的确定证据。远端内容需要写入审核说明并保持可访问。

## 未核验与提交前验证

- **App Store Connect 元数据未审计**：仓库未见完整 fastlane/metadata 或提交字段；未取得正式名称、副标题、描述、关键词、年龄评级问卷、截图说明、隐私标签、政策/支持 URL、审核备注及演示访问配置。按本次仅审仓库且不追问的要求记录此缺口，不能把缺失本地副本写成线上违规。提交前核对 2.3、4.4 的功能/扩展说明及 AI、诊断数据披露。
- **隐私清单及上传校验未完成**：仓库未见 `PrivacyInfo.xcprivacy` 或 Gradle 显式清单配置。存在 MMKV、Okio、Room/SQLite、Compose 等依赖；`util/src/commonMain/kotlin/top/kagg886/util/okio.kt:10` 与 `crashApp/src/commonMain/kotlin/top/kagg886/report/AppModel.kt:531` 使用文件 metadata，但未检查解析后的 iOS 依赖源码和成品符号，不能据此断言某项 Required Reason API 已进入二进制或必定拒收。应检查主 App、两个扩展及内嵌 SDK 清单、理由码和 SDK 签名，运行 Archive 的 Validate App。[Apple 隐私清单上传要求](https://developer.apple.com/news/?id=3d8a9yyh)
- **导出合规未核验**：源码未见 `ITSAppUsesNonExemptEncryption`；这首先是提交问卷准备项，不是单独的拒审结论。项目有 RSA/AES 和 cryptography 依赖，应按实际 iOS 加密实现回答问卷，不能仅凭 HTTPS 或算法名称填写豁免状态。
- 未运行构建、模拟器或真机；运行时崩溃、权限弹窗、拒绝授权后行为、Widget/分享扩展、iPad 布局、安装包最低系统兼容性均未验证。
- 未测试失效链接、学校真实服务、验证码/二次验证、OAuth 流程、境外审核网络及远程配置实际内容；未调用学校账号或发送诊断、AI、反馈数据。
- 未检查恢复购买运行行为（未发现 IAP）、赞赏交易、商店地区资格、服务端保留/删除执行情况或数据接收方实践。

## 规则核验说明

- 本次已在线读取 [App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/) 的相关安全、性能、商业、设计和隐私条款，且可读取 5.4–5.6；没有因页面尾部截断而引用二手指南措辞。以上短引文均来自本次在线规则。
- [Apple Developer News](https://developer.apple.com/news/) 已扫描至技能参考日期 2026-08-20：9 月 16 日订阅更新、欧盟 ATT 提示变化及 8 月 24 日 Apple 登录中继域名变化，未发现本项目对应实现，不另列风险；9 月 9 日发布准备公告适用于后续真机与元数据检查。其 [SDK 新门槛公告](https://developer.apple.com/news/?id=k1mtkt1k)指向 2027 年 4 月，不能提前当作本次上传必定失败依据。
- Apple 文档站部分页面直接读取仅返回脚本外壳：EventKit 结论通过搜索获取的 Apple 官方 API/TN3152 文本，并以 [Apple WWDC EventKit 说明](https://developer.apple.com/videos/play/wwdc2023/10052/)交叉核对；这是检索回退，未使用论坛或非 Apple 技术结论。
- Required Reason API 页面同样出现脚本外壳；本次仅以官方上传公告核实一般要求，没有验证具体理由码或实际打包内容，不把技能示例理由码直接套入项目。
- 未发现影响本报告具体结论的技能参考与在线指南冲突；未审计的地区支付、评级及加密事项保留为待验证项。
