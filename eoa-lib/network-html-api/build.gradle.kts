import java.security.MessageDigest


plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.kotlinx.serialization)


    alias(libs.plugins.ksp)
}

group = "top.kagg886.sylu_eoa.api.v3"
version = "1.0"

android {
    ndkVersion = "28.1.13356709"
    namespace = "top.kagg886.sylu_eoa.api.html"

    compileSdk = 35

    defaultConfig {
        minSdk = 28
        targetSdk = 35
        externalNativeBuild {
            cmake {
                targets += "cargo-build_eoa_security"
            }
        }
        ndk {
            // 只支持arm64和x64
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
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
    jvmToolchain(22)
    jvm()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { t ->
        t.apply {
            compilations.all {
                cinterops {
                    val eoa by creating {
                        defFile("src/iosMain/interop/libeoa_security.def")
                        packageName("eoa")
                        includeDirs("src/iosMain/interop/include")
                    }
                }
            }
        }
    }

    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.okio)
            implementation(libs.ksoup)

            implementation(libs.mkmb.core)
            implementation(libs.sweet.api.runtime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)

            implementation(project(":util"))
            api(project(":eoa-lib:network-core"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmTest.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosTest.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.ktor.client.okhttp)
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
        JvmDesktopPlatform.MACOS -> "libeoa_security.dylib"
        JvmDesktopPlatform.LINUX -> "libeoa_security.so"
        JvmDesktopPlatform.WINDOWS -> "eoa_security.dll"
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
        project.file("src/rust/target/release/gif-build.hash").writeText(hash)
        logger.lifecycle("rust lib hash is $hash")
    }
}

tasks.named<ProcessResources>("jvmProcessResources") {
    dependsOn(jvmMetadataGenerated)
    from(
        project.file("src/rust/target/release/$jvmPlatformLibraryName"),
        project.file("src/rust/target/release/gif-build.hash"),
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

dependencies {
    with(libs.sweet.compiler) {
        add("kspAndroid", this)
        add("kspJvm", this)
        add("kspIosArm64", this)
        add("kspIosSimulatorArm64", this)
    }
}
