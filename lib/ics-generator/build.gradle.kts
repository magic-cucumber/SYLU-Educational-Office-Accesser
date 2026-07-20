
plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)
}

group = "top.kagg886.ics"
version = "1.0"

kotlin {
    library(module = "ics")

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
