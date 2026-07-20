import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/8/18 16:45
 * ================================================
 */

fun KotlinMultiplatformExtension.library(
    module: String,
    enableAndroidResources: Boolean = false,
    jvm: KotlinJvmTarget.() -> Unit = {},
    ios: KotlinNativeTarget.() -> Unit = {},
) {
    jvmToolchain(22)

    (this as ExtensionAware).extensions.configure(
        "android",
        Action<KotlinMultiplatformAndroidLibraryTarget> {
            namespace = "top.kagg886.$module"
            compileSdk = 37
            minSdk = if (project.useDesugarApi) 23 else 28
            withHostTest {}

            if (enableAndroidResources) {
                androidResources.enable = true
            }
        },
    )

    jvm(jvm)
    iosArm64 {
        compilerOptions { freeCompilerArgs.add("-Xpartial-linkage=disable") }
        ios()
    }
    iosSimulatorArm64 {
        compilerOptions { freeCompilerArgs.add("-Xpartial-linkage=disable") }
        ios()
    }

//    wasmJs {
//        browser()
//    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.addAll("kotlin.time.ExperimentalTime")
    }
}
