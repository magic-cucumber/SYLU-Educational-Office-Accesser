plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")


    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

group = "top.kagg886.calender.v2"
version = "1.0"

kotlin {
    library(module = "calender.v2")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)

            implementation(libs.kotlinx.coroutines.core)
            implementation(project.dependencies.project(":util"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.activityCompose)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
