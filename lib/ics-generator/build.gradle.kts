
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)
}

group = "top.kagg886.ics"
version = "1.0"

android("ics")

kotlin {
    library(
        android = {
            publishLibraryVariants("release")
        }
    )

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.datetime)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
