@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
}

group = "top.kagg886.util.http"
version = "1.0"

kotlin {
    library(module = "util.http")

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

    sourceSets {
        named("commonMain") {
            dependencies {
                api(libs.ktor.client.core)
            }
        }
        named("nonIosMain") {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        named("iosMain") {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
