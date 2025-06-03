import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

val appVersion = project.findProperty("app.version") as String
val appVersionCode = (project.findProperty("app.code") as String).toInt()

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildConfig)
}

kotlin {
    androidTarget {
        //https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }

    jvm()
    jvmToolchain(22)

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts += "-lsqlite3"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.material.icons.extended)

            implementation(libs.adaptive)
//            implementation(libs.adaptive.layout)
//            implementation(libs.adaptive.navigation)
            implementation(libs.adaptive.navigation.suite)

            implementation(libs.kermit)
            implementation(libs.okio)
            implementation(libs.multiplatform.markdown.renderer.m3)
            implementation(libs.multiplatform.markdown.renderer.coil3)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.androidx.navigation.compose)

            implementation(libs.coil)
            implementation(libs.coil.network.ktor)
            implementation(libs.multiplatformSettings)
            implementation(libs.kotlinx.datetime)
            implementation(libs.room.runtime)
            implementation(libs.materialKolor)

            implementation(libs.orbit.core)
            implementation(libs.orbit.viewmodel)
            implementation(libs.orbit.compose)

            implementation(libs.mkmb.core)
            implementation(libs.compose.stacked.snackbar)
            implementation(libs.compose.placeholder.material3)

            implementation(project(":util"))
            //方便切换到闭源后端
            implementation(project(":eoa-lib:network-html-api"))
            implementation(project(":eoa-lib:network-test-api"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.sweet.api.runtime)
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.androidx.activityCompose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.sqlite.bundled)

            implementation(libs.mkmb.platform.windows)
            implementation(libs.mkmb.platform.linux)
            implementation(libs.mkmb.platform.macos)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

    }
}

android {
    namespace = "top.kagg886.eoa"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        targetSdk = 35

        applicationId = "top.kagg886.eoa.androidApp"
        versionCode = appVersionCode
        versionName = appVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

//https://developer.android.com/develop/ui/compose/testing#setup
dependencies {
    androidTestImplementation(libs.androidx.uitest.junit4)
    debugImplementation(libs.androidx.uitest.testManifest)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SYLU-EOA"
            packageVersion = appVersion

            linux {
                iconFile.set(project.file("desktopAppIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("desktopAppIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("desktopAppIcons/MacosIcon.icns"))
                bundleID = "top.kagg886.eoa.desktopApp"
            }
        }
    }
}

buildConfig {
    // BuildConfig configuration here.
    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
    packageName("top.kagg886.eoa.config")
    buildConfigField("DATABASE_VERSION", 1)
    buildConfigField("APP_VERSION_CODE", appVersionCode)
    buildConfigField("APP_VERSION_NAME", appVersion)
    buildConfigField("GIT_COMMIT_SHA", "123456")
}

room {
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


fun getGitCommitCount(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText().trim()
        output.toIntOrNull() ?: -1
    } catch (e: Exception) {
        println("Error getting git commit count: ${e.message}")
        -1
    }
}

fun getLastCommitSha(): String? {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "HEAD")
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText().trim()
        if (output.isNotEmpty()) output else null
    } catch (e: Exception) {
        println("Error getting last commit SHA: ${e.message}")
        null
    }
}