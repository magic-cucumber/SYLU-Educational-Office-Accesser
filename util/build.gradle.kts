@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)
}

group = "top.kagg886.util"
version = "1.0"

kotlin {
    applyHierarchyTemplate {
        common {
            group("nonIos") {
                withCompilations { compilation ->
                    compilation.target.name == "android" || compilation.target.name == "jvm"
                }
            }
            group("ios") {
                withIos()
            }
        }
    }
    library(module = "util")

    sourceSets {
        commonMain {
            dependencies {
                api(libs.compose.foundation)
                api(libs.kermit)
                api(libs.okio)
                implementation(libs.kmp.zip)
                implementation(libs.kmp.zip.okio)
                api(libs.ktor.client.logging)
                api(libs.kotlinx.serialization.json)
                api(libs.mkmb.core)
                api(libs.kotlinx.datetime)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
