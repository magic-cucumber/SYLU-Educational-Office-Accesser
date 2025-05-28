plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

group = "top.kagg886.util"
version = "1.0"

android {
    namespace = "top.kagg886.util"

    compileSdk = 35
    defaultConfig {
        minSdk = 21
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
        commonMain {
            dependencies {
                implementation(libs.kermit)
                implementation(libs.okio)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

