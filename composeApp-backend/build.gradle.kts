val databaseVersion = (project.findProperty("database.version") as String).toInt()
val appVersion = project.findProperty("app.version") as String
val appVersionCode = (project.findProperty("app.code") as String).toInt()

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildConfig)
}

group = "top.kagg886.composeapp.backend"
version = "1.0"

kotlin {
    library(
        module = "composeapp.backend",
        ios = {
            binaries.framework {
                baseName = "ComposeAppBackend"
                isStatic = true
                export(project(":util"))
                linkerOpts += "-lsqlite3"
            }
        }
    )

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)

            api(libs.compose.foundation)
            api(libs.kermit)
            api(libs.androidx.paging)
            implementation(libs.androidx.sqlite.async)
            api(libs.room.runtime)
            api(libs.room.paging)

            api(libs.mkmb.core)

            api(project.dependencies.project(":eoa-lib:network-core"))
            api(project.dependencies.project(":util"))
            api(project.dependencies.project(":second-class"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
    }
}

buildConfig {
    packageName("top.kagg886.eoa.config")
    useKotlinOutput {
        internalVisibility = false
    }

    buildConfigField("DATABASE_VERSION", databaseVersion)

    buildConfigField("APP_DESUGAR_ENABLED",useDesugarApi)
    buildConfigField("APP_VERSION_CODE", appVersionCode)
    buildConfigField("APP_VERSION_NAME", appVersion)
    buildConfigField("GIT_COMMIT_SHA", "123456")
    buildConfigField("MESSAGE_MAIL", messageMail)
    buildConfigField("MESSAGE_WEBSITE_URL", messageWebsiteUrl)
    buildConfigField("MESSAGE_QQ_GROUP_URL", messageQQGroupUrl)
    buildConfigField("MESSAGE_QQ_GROUP_LABEL", messageQQGroupLabel)
    buildConfigField("MESSAGE_GITEE_HOST", messageGiteeHost)
    buildConfigField("MESSAGE_API_ENDPOINT", messageApiEndpoint)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    with(libs.room.compiler) {
        add("kspAndroid", this)
        add("kspJvm", this)
        add("kspIosArm64", this)
        add("kspIosSimulatorArm64", this)
    }
}
