import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

val appVersion = project.findProperty("app.version") as String
val appVersionCode = (project.findProperty("app.code") as String).toInt()

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.buildConfig)
    id("com.google.osdetector") version "1.7.3"
}

kotlin {
    library(
        module = "eoa",
        android = { androidResources.enable = true },
        ios = {
            binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
                linkerOpts += "-lsqlite3"
            }
        }
    )

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.material.icons.extended)

            implementation(libs.adaptive)
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
            implementation(libs.androidx.paging)


            implementation(libs.coil)
            implementation(libs.coil.network.ktor)
            implementation(libs.kotlinx.datetime)
            implementation(libs.room.runtime)
            implementation(libs.room.paging)
            implementation(libs.materialKolor)
            implementation(libs.kolor.picker)

            implementation(libs.orbit.core)
            implementation(libs.orbit.viewmodel)
            implementation(libs.orbit.compose)

            implementation(libs.sonner)
            implementation(libs.compose.placeholder.material3)
            implementation(libs.filekit.dialog)
            implementation(libs.compose.dnd)
            implementation(libs.koog.agents)
            implementation(libs.reveal.core)
            implementation(libs.reveal.shapes)

            implementation(project.dependencies.project(":composeApp-backend"))
            implementation(project.dependencies.project(":widgetApp"))
            implementation(project.dependencies.project(":util"))
            //方便切换到闭源后端
            implementation(project.dependencies.project(":eoa-lib:network-html-api"))
            implementation(project.dependencies.project(":eoa-lib:network-test-api"))


            implementation(project.dependencies.project(":lib:ktor-platform-engine"))
            implementation(project.dependencies.project(":lib:ics-generator"))
            implementation(project.dependencies.project(":lib:calender-exporter-v2"))
            implementation(project.dependencies.project(":second-class"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.sweet.api.runtime)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activityCompose)
            implementation(libs.kotlinx.coroutines.android)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.androidx.sqlite.bundled)

            implementation(libs.mkmb.platform.windows)
            implementation(libs.mkmb.platform.linux)
            implementation(libs.mkmb.platform.macos)
        }
    }
}

configurations.configureEach {
    exclude(group = "io.ktor", module = "ktor-client-cio")
    resolutionStrategy.eachDependency {
        if (requested.group == "io.ktor") {
            useVersion(libs.versions.ktor.get())
            because("Kotlin/Native linking fails when ai.koog pulls older Ktor server modules than the app's Ktor client stack")
        }
    }
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

        buildTypes {
            release {
                proguard {
                    isEnabled = false
                }
            }
        }
    }
}

buildConfig {
    // BuildConfig configuration here.
    // https://github.com/gmazzo/gradle-buildconfig-plugin#usage-in-kts
    packageName("top.kagg886.eoa.config")
    buildConfigField("APP_DESUGAR_ENABLED",useDesugarApi)
    buildConfigField("APP_VERSION_CODE", appVersionCode)
    buildConfigField("APP_VERSION_NAME", appVersion)
    buildConfigField("GIT_COMMIT_SHA", "123456")
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

// ------------------ IOS Packages Build ------------------

// context path is rootProject.dir("iosApp")
fun ipaArguments(
    destination: String = "generic/platform=iOS",
    sdk: String = "iphoneos",
): Array<String> = arrayOf(
    "xcodebuild",
    "-project", "iosApp.xcodeproj",
    "-scheme", "iosApp",
    "-destination", destination,
    "-sdk", sdk,
    "CODE_SIGNING_ALLOWED=NO",
    "CODE_SIGNING_REQUIRED=NO",
)

val buildReleaseArchive = tasks.register("buildReleaseArchive", Exec::class) {
    group = "build"
    description = "Builds the iOS framework for Release"
    workingDir(rootProject.file("iosApp"))

    val output = layout.buildDirectory.dir("archives/release/iosApp.xcarchive")
    outputs.dir(output)
    commandLine(
        *ipaArguments(),
        "archive",
        "-configuration",
        "Release",
        "-archivePath",
        output.get().asFile.absolutePath,
    )
}

tasks.register("buildReleaseIpa", BuildIpaTask::class) {
    description = "Manually packages the .app from the .xcarchive into an unsigned .ipa"
    group = "build"

    // Adjust these paths as needed
    archiveDir = layout.buildDirectory.dir("archives/release/iosApp.xcarchive")
    outputIpa = layout.buildDirectory.file("archives/release/iosApp.ipa")
    dependsOn(buildReleaseArchive)
}

@CacheableTask
abstract class BuildIpaTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputDirectory
    abstract val archiveDir: DirectoryProperty

    @get:OutputFile
    abstract val outputIpa: RegularFileProperty

    @TaskAction
    fun buildIpa() {
        // 1. Locate the .app in the .xcarchive
        val appDir = archiveDir.get().asFile.resolve("Products/Applications/SYLU-EOA.app")
        if (!appDir.exists()) {
            throw GradleException("Could not find iosApp.app in archive at: ${appDir.absolutePath}")
        }

        // 2. Create a temporary Payload folder
        val payloadDir = File(temporaryDir, "Payload").apply { mkdirs() }
        val destApp = File(payloadDir, "SYLU-EOA.app")

        // 3. Copy the .app into Payload/
        appDir.copyRecursively(destApp, overwrite = true)

        // 4. Zip the Payload folder
        val zipFile = File(temporaryDir, "SYLU-EOA.zip")
        zipDirectory(payloadDir, zipFile)

        // 5. Rename .zip to .ipa
        val ipaFile = outputIpa.get().asFile
        ipaFile.parentFile.mkdirs()
        if (ipaFile.exists()) ipaFile.delete()
        zipFile.renameTo(ipaFile)

        logger.lifecycle("Created unsigned IPA at: ${ipaFile.absolutePath}")
    }

    /**
     * Zips the given [sourceDir] (including all subdirectories) into [outputFile].
     */
    private fun zipDirectory(sourceDir: File, outputFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOut ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(sourceDir.parentFile).path
                    val zipEntry = ZipEntry(relativePath)
                    zipOut.putNextEntry(zipEntry)
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }
    }
}

//FIXME: from Koog sample app
configurations.all {
    exclude(group = "io.netty", module = "*")
}
