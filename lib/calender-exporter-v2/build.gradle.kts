plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")


    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

group = "top.kagg886.calender.v2"
version = "1.0"

android("calender.v2")

kotlin {
    library(
        android = {
            publishLibraryVariants("release")
        }
    )

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)

            implementation(libs.kotlinx.coroutines.core)
            implementation(project(":util"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.activityCompose)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
