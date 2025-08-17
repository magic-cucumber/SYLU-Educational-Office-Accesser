plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")


    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

group = "top.kagg886.kolorpicker"
version = "1.0"

android("kolorpicker")

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
        }

        androidMain.dependencies {
            implementation(libs.androidx.activityCompose)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
