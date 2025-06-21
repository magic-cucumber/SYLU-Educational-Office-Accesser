
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.multiplatform)


    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

group = "top.kagg886.calender"
version = "1.0"

android {
    namespace = "top.kagg886.calender"

    compileSdk = 35
    defaultConfig {
        minSdk = 28
        targetSdk = 35
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

kotlin {
    jvmToolchain(22)

    jvm()
    iosArm64()
    iosSimulatorArm64()

    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(compose.runtime)
            implementation(compose.foundation)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activityCompose)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}