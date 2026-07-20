plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
}

group = "top.kagg886.util.security"
version = "1.0"

kotlin {
    library(module = "util.security")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
        }
    }
}
