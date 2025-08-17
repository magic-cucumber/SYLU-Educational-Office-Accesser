plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)
}

group = "top.kagg886.util"
version = "1.0"

android("util")

kotlin {
    library(
        android = {
            publishLibraryVariants("release")
        }
    )

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kermit)
                api(libs.okio)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

