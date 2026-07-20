

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)


    alias(libs.plugins.ksp)
}

group = "top.kagg886.eoa.second"
version = "1.0"

kotlin {
    library(module = "eoa.second")

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
            implementation(project.dependencies.project(":lib:ktor-platform-engine"))
            implementation(project.dependencies.project(":lib:rsa"))
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)

            implementation(project.dependencies.project(":util"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
