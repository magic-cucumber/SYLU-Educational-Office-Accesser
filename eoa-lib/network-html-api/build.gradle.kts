import java.security.MessageDigest


plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)

    alias(libs.plugins.ksp)
}

group = "top.kagg886.sylu_eoa.api.v3"
version = "1.0"

kotlin {
    library(module = "sylu_eoa.api.v3")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.okio)
            implementation(libs.ksoup)

            implementation(libs.mkmb.core)
            implementation(libs.sweet.api.runtime)

            api(project.dependencies.project(":lib:ktor-platform-engine"))
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)

            implementation(project.dependencies.project(":util"))
            implementation(project.dependencies.project(":lib:rsa"))
            api(project.dependencies.project(":eoa-lib:network-core"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    with(libs.sweet.compiler) {
        add("kspAndroid", this)
        add("kspJvm", this)
        add("kspIosArm64", this)
        add("kspIosSimulatorArm64", this)
    }
}
