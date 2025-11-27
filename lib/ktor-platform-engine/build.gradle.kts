@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
}

group = "top.kagg886.util.http"
version = "1.0"

android("util.http")

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("nonIos") {
                withJvm()
                withAndroidTarget()
            }
        }
    }
    library(
        android = {
            publishLibraryVariants("release")
        },
    )

    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
        }
        nonIosMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}


private val NamedDomainObjectContainer<org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet>.nonIosMain: NamedDomainObjectProvider<org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet>
    get() = named<org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet>("nonIosMain")
