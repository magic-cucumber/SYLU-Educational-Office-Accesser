import com.android.build.gradle.LibraryExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/18 16:45
 * ================================================
 */

fun KotlinMultiplatformExtension.library(
    jvm: KotlinJvmTarget.() -> Unit = {},
    ios: KotlinNativeTarget.() -> Unit = {},
    android: KotlinAndroidTarget.() -> Unit = {},
) {
    jvmToolchain(22)

    jvm(jvm)
    iosArm64 {
        compilerOptions { freeCompilerArgs.add("-Xpartial-linkage=disable") }
        ios()
    }
    iosSimulatorArm64 {
        compilerOptions { freeCompilerArgs.add("-Xpartial-linkage=disable") }
        ios()
    }

    androidTarget(android)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.addAll("kotlin.time.ExperimentalTime")
    }
}


fun Project.android(module: String, configure: LibraryExtension.() -> Unit = {}) =
    (this as ExtensionAware).extensions.configure("android", Action<LibraryExtension> {
        namespace = "top.kagg886.$module"

        compileSdk = 35
        defaultConfig {
            minSdk = 28
        }

        buildTypes {
            release {
                isMinifyEnabled = false

                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
        }

        configure()
    })
