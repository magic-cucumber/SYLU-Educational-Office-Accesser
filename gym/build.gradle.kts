plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)

    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.ksp)
}

group = "top.kagg886.eoa.gym"
version = "1.0"

kotlin {
    library(module = "eoa.gym")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.encoding)
            implementation(libs.okio)
            implementation(libs.kermit)
            implementation(project.dependencies.project(":util"))
            implementation(project.dependencies.project(":lib:ktor-platform-engine"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
        }
    }
}
