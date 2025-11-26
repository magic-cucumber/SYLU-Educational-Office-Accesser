

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)


    alias(libs.plugins.ksp)
}

group = "top.kagg886.eoa.second"
version = "1.0"

android("eoa.second")

kotlin {
    library(
        android = {
            publishLibraryVariants("release")
        }
    )

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.encoding)
            implementation(libs.ksoup)
            implementation(libs.okio)

            implementation(project(":lib:rsa"))
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)

            implementation(project(":util"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmTest.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosTest.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
    }
}
