import dev.whyoleg.sweetspi.gradle.*

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.kotlinx.serialization)


    alias(libs.plugins.ksp)
    alias(libs.plugins.sweet.api)
}

group = "top.kagg886.sylu_eoa.api.test"
version = "1.0"

android {
    namespace = "top.kagg886.sylu_eoa.api.test"

    compileSdk = 35

    defaultConfig {
        minSdk = 28
        targetSdk = 35
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

kotlin {
    withSweetSpi()
    jvmToolchain(22)
    jvm()

    iosArm64()
    iosSimulatorArm64()

    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":eoa-lib:network-core"))
            implementation(libs.sweet.api.runtime)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}