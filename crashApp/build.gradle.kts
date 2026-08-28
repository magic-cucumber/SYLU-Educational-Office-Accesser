

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.kotlinx.serialization)
}

group = "top.kagg886.crashApp"
version = "1.0"

kotlin {
    library(
        module = "crashApp",
        android = { androidResources.enable = true },
        ios = {
            binaries.framework {
                baseName = "CrashApp"
                isStatic = true
                linkerOpts += "-lsqlite3"
            }
        },
    )

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.material.icons.extended)
            implementation(libs.materialKolor)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime)

            implementation(libs.orbit.core)
            implementation(libs.orbit.viewmodel)
            implementation(libs.orbit.compose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.filekit.dialog)


            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.base)
            implementation(libs.cryptography.provider.optimal)


            implementation(project.dependencies.project(":lib:ktor-platform-engine"))
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.io.okio)

            implementation(project.dependencies.project(":composeApp-backend"))
            implementation(project.dependencies.project(":util"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.activityCompose)
            implementation(libs.kotlinx.coroutines.android)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}
