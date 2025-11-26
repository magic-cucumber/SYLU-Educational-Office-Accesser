import java.security.MessageDigest


plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)

    alias(libs.plugins.ksp)
}

group = "top.kagg886.util.security"
version = "1.0"

android("util.security")

kotlin {
    library(
        android = {
            publishLibraryVariants("release")
        },
    )

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
        }
        jvmMain.dependencies {
            implementation(project(":util"))
        }
    }
}
