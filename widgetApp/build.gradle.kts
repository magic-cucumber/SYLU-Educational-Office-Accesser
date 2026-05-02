plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.library")
}

group = "top.kagg886.widgetapp"
version = "1.0"

android("widgetApp")

kotlin {
    library(ios = {
        binaries.framework {
            baseName = "WidgetApp"
            isStatic = true
            linkerOpts += "-lsqlite3"
        }
    })

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.material.icons.extended)
            implementation(libs.materialKolor)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.filekit.dialog)

            implementation(project(":composeApp-backend"))
            implementation(project(":util"))
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
