
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")


    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

group = "top.kagg886.calender"
version = "1.0"

android("calender")

kotlin {
    library(
        android = {
            publishLibraryVariants("release")
        }
    )

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(compose.runtime)
            implementation(compose.foundation)

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
