# SYLU-EOA

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-blue.svg)]()
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg)]()
[![License](https://img.shields.io/badge/license-MIT-blue.svg)]()

沈阳理工大学教务办公助手 (SYLU Educational Office Accesser) - 一个基于 Kotlin Multiplatform 开发的移动端教务系统客户端，支持
Android 和 iOS 平台。

## 📱 功能特性

### 🎯 核心功能

- **用户认证** - 支持教务系统登录，包含验证码处理
- **课程管理** - 查看课程表、添加自定义课程、导出课程安排
- **考试查询** - 查看考试安排、考试详情、成绩查询
- **绩点统计** - GPA 查询与统计分析
- **校历查看** - 学校日历信息展示
- **数据同步** - 自动同步教务系统数据到本地

### 🛠️ 技术特性

- **跨平台支持** - Android 和 iOS 原生应用
- **现代化 UI** - 基于 Jetpack Compose 构建的 Material Design 3 界面
- **离线支持** - 本地数据库存储，支持离线查看
- **多后端支持** - 支持多种数据源（HTML API、测试 API）
- **主题定制** - 支持浅色/深色主题切换和自定义颜色
- **数据导出** - 支持课程表导出为 ICS 日历格式，也可导出为系统日历

## 🏗️ 技术架构

### 技术栈

- **Kotlin Multiplatform** - 跨平台开发框架
- **Jetpack Compose** - 现代化声明式 UI 框架
- **Room Database** - 本地数据持久化
- **Ktor Client** - 网络请求处理
- **Orbit MVI** - 状态管理架构
- **MMKV** - 高性能键值存储
- **Kermit** - 跨平台日志框架

### 项目结构

```
SYLU-EOA/
├── composeApp/                 # 主应用模块
│   ├── src/androidMain/       # Android 平台特定代码
│   ├── src/iosMain/          # iOS 平台特定代码
│   ├── src/commonMain/       # 共享业务逻辑
│   └── src/jvmMain/          # JVM 平台代码（开发调试用）
├── eoa-lib/                   # 教务系统 API 库
│   ├── network-core/         # 核心网络接口定义
│   ├── network-html-api/     # HTML 解析 API 实现
│   └── network-test-api/     # 测试 API 实现
├── lib/                      # 工具库
│   ├── ics-generator/        # ICS 日历生成器
│   └── calender-exporter/    # 日历导出工具
└── util/                     # 通用工具类
```

## 🚀 快速开始

### 环境要求

- **Android Studio** Hedgehog (2023.1.1) 或更高版本
- **JDK** 17 或更高版本
- **Android SDK** API 28+ (目标 API 35)
- **Xcode** 16+ (iOS 开发)
- **Kotlin** 2.0+
- **Rust** 1.86.0 或更高版本

### 环境配置

1. **安装 Rust 工具链**

```bash
# 安装指定版本的 Rust
rustup default 1.86.0

# Android 开发需要的目标平台
rustup target add aarch64-linux-android x86_64-linux-android

# iOS 开发需要的目标平台 (仅 macOS)
rustup target add aarch64-apple-ios

# 安装必要组件
rustup component add rustfmt clippy
```

2. **添加 local.properties 文件**
   在项目根目录创建 `local.properties` 文件并设置 Android SDK 路径：

```properties
sdk.dir=/path/to/your/android/sdk
```

### 构建项目

1. **克隆项目**

```bash
git clone https://github.com/your-username/sylu-educational-office-accesser.git
cd sylu-educational-office-accesser
```

2. **设置执行权限(非windows平台需要执行)**

```bash
chmod +x ./gradlew
```

#### Android 构建

**调试版本**

```bash
./gradlew assembleDebug
```

**发布版本**

```bash
./gradlew assembleRelease
```

构建完成后，APK 文件位于：

- 调试版本：`composeApp/build/outputs/apk/debug/composeApp-debug.apk`
- 发布版本：`composeApp/build/outputs/apk/release/composeApp-release.apk`

#### iOS 构建 (仅 macOS)

**发布版本 IPA**

```bash
# 设置 Xcode 环境变量(如xcode构建出错则需要添加环境变量。)
export PATH=/Applications/Xcode_16.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS18.0.sdk/usr/lib:$PATH
export LIBRARY_PATH="$LIBRARY_PATH:/Applications/Xcode_16.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS18.0.sdk/usr/lib"

# 构建 IPA
./gradlew buildReleaseIpa
```

构建完成后，IPA 文件位于：`composeApp/build/archives/release/iosApp.ipa`

**模拟器测试**

```bash
./gradlew iosSimulatorArm64Test
```

### 运行应用

> 仅提供`Android Studio` 运行教程。
>

#### Android

- 在 Android Studio 中打开项目
- 选择 `composeApp` 配置
- 点击运行按钮或使用 `Ctrl+R`

#### iOS

>
在此之前，您需要安装 [Kotlin MultiPlatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-plugin-releases.html)
插件。
>
> **它仅支持macOS设备，请不要在windows或linux中安装。**

- 在 Android Studio 中打开项目
- 选择 `iosApp` 配置
- 点击运行按钮或使用 `Ctrl+R`

## 📖 使用指南

### 登录设置

1. 打开应用后进入登录页面

2. 输入**教务系统**用户名和密码

3. 选择合适的登录后端

   > 一般情况下都要选择 `公开API`。
   >
   > 如果您是外校开发者，想要适配自己的教务系统的话，您可以选择`测试API`。账户密码为 **test**/**test**

1. 如需验证码，按提示输入
2. 登录成功后自动跳转到主界面

### 主要功能使用

#### 📅 课程表管理

- **查看课程** - 在首页查看本日课程安排，在课表页面查看本周课程安排
- **添加课程** - 点击"+"按钮添加自定义课程
- **编辑课程** - 左滑课程卡片编辑，右滑课程卡片删除自定义课程
- **导出课程** - 支持导出为 ICS 格式，支持一键导出到系统日历中

#### 📝 考试管理

- **考试列表** - 查看所有考试成绩
- **筛选功能** - 按学期、考试状态、学位类型筛选，支持关键词
- **考试详情** - 点击考试项目即可查看详细信息

#### 📊 绩点查询

- **GPA 统计** - 查看总体 GPA 统计

### 设置选项

- **主题设置** - 浅色/深色/跟随系统
- **颜色定制** - 自定义应用主题色
- **首页模块** - 自定义首页显示的功能模块

## 🔧 开发指南

### 添加新功能

1. 在 `commonMain` 中添加共享业务逻辑
2. 在对应平台目录中添加平台特定实现
3. 使用 Orbit MVI 模式管理状态
4. 遵循 Material Design 3 设计规范

### 数据库操作

- 使用 Room 数据库进行数据持久化
- DAO 接口定义在 `backend/database/dao` 目录
- 数据库迁移在 `database.kt` 中管理

### 网络请求

- 实现 `EOAClient` 接口添加新的数据源
- 使用 `EOAClientProvider` 注册新的后端
- 网络请求统一使用 Ktor Client

## 🏫 适配其他学校教务系统

本项目采用插件化架构设计，支持适配不同学校的教务系统。详细的适配指南请参考：

📖 **[EOA-Lib 适配指南](eoa-lib/README.md)**

该指南包含：

- 完整的适配步骤和代码示例
- 核心接口说明和数据模型定义
- 网络请求处理和异常处理
- 测试指导和最佳实践

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 提交规范

- 使用清晰的提交信息
- 遵循项目的代码风格
- 添加必要的测试用例
- 更新相关文档

### 开发流程

1. Fork 项目
2. 创建功能分支
3. 提交更改
4. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Ktor](https://ktor.io/)

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 在Github提交 [Issue](https://github.com/kagg886/sylu-educational-office-accesser/issues)
- 发送邮件至：[iveour@163.com](mailto:iveour@163.com)。

---

**注意**: 本应用仅供学习交流使用，请遵守学校相关规定。
