plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
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
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
        }
    }
}
