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
    android: KotlinMultiplatformAndroidLibraryTarget.() -> Unit = {},
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

            android()
        },
    )

    jvm(jvm)
    iosArm64(ios)
    iosSimulatorArm64(ios)

//    wasmJs {
//        browser()
//    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.addAll("kotlin.time.ExperimentalTime")
    }
}
