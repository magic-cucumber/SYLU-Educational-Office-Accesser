import java.security.MessageDigest


plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlinx.serialization)

    alias(libs.plugins.ksp)
}

group = "top.kagg886.util.security"
version = "1.0"

android("util.security") {
    ndkVersion = "28.1.13356709"

    defaultConfig {
        externalNativeBuild {
            cmake {
                targets += "cargo-build_security"
                arguments += "-DGIT_EXECUTABLE=/usr/bin/git"
            }
        }
        ndk {
            // 只支持arm64和x64
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    externalNativeBuild {
        cmake {
            path = File("src/rust/CMakeLists.txt")
        }
    }
}

val kotlinArchToRustArch = mapOf(
    "iosArm64" to "aarch64-apple-ios",
    "iosSimulatorArm64" to "aarch64-apple-ios-sim",
)

kotlin {
    library(
        android = {
            publishLibraryVariants("release")
        },
        ios = {
            compilations.all {
                cinterops {
                    val eoa by creating {
                        defFile("src/iosMain/interop/libsecurity.def")
                        packageName("eoa")
                        includeDirs("src/iosMain/interop/include")
                    }
                }
            }
        }
    )

    sourceSets {
        commonMain.dependencies {
            implementation("dev.whyoleg.cryptography:cryptography-core:0.5.0")
            implementation("dev.whyoleg.cryptography:cryptography-provider-optimal:0.5.0")
        }
        jvmMain.dependencies {
            implementation(project(":util"))
        }
    }
}

enum class JvmDesktopPlatform {
    WINDOWS,
    LINUX,
    MACOS,
}

val currentJvmPlatform by lazy {
    val prop = System.getProperty("os.name")
    when {
        prop.startsWith("Mac") -> JvmDesktopPlatform.MACOS
        prop.startsWith("Linux") -> JvmDesktopPlatform.LINUX
        prop.startsWith("Win") -> JvmDesktopPlatform.WINDOWS
        else -> error("unsupported platform: $prop")
    }
}

val jvmPlatformLibraryName by lazy {
    when (currentJvmPlatform) {
        JvmDesktopPlatform.MACOS -> "libsecurity.dylib"
        JvmDesktopPlatform.LINUX -> "libsecurity.so"
        JvmDesktopPlatform.WINDOWS -> "security.dll"
    }
}

val jvmCargoBuildRelease = tasks.register<Exec>("jvmCargoBuildRelease") {
    val cmd = "cargo build --release --features jvm"
    workingDir = project.file("src/rust")
    when (currentJvmPlatform) {
        JvmDesktopPlatform.WINDOWS -> commandLine("cmd", "/c", cmd)
        JvmDesktopPlatform.LINUX -> commandLine("bash", "-c", cmd)
        JvmDesktopPlatform.MACOS -> commandLine("zsh", "-c", cmd)
    }
}

fun File.md5() = MessageDigest.getInstance("MD5").digest(readBytes()).joinToString("") {
    "%02x".format(it)
}

val jvmMetadataGenerated = tasks.register("jvmMetadataGenerated") {
    dependsOn(jvmCargoBuildRelease)
    doFirst {
        val hash = project.file("src/rust/target/release/$jvmPlatformLibraryName").md5()
        project.file("src/rust/target/release/security.hash").writeText(hash)
        logger.lifecycle("rust lib hash is $hash")
    }
}

tasks.named<ProcessResources>("jvmProcessResources") {
    dependsOn(jvmMetadataGenerated)
    from(
        project.file("src/rust/target/release/$jvmPlatformLibraryName"),
        project.file("src/rust/target/release/security.hash"),
    )
}

for ((kotlinArch, rustArch) in kotlinArchToRustArch) {
    val iosNativeCargoTask = tasks.register<Exec>("${kotlinArch}NativeCargoTask") {
        onlyIf { System.getProperty("os.name").startsWith("Mac") }
        workingDir = project.file("src/rust")
        commandLine("zsh", "-c", "cargo build --release --target $rustArch")
    }
    tasks.named("cinteropEoa${kotlinArch.replaceFirstChar(Char::uppercaseChar)}") {
        dependsOn(iosNativeCargoTask)
    }
}
