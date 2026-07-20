import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val appVersion = project.findProperty("app.version") as String
val appVersionCode = (project.findProperty("app.code") as String).toInt()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "top.kagg886.eoa.androidApp"
    compileSdk = 37

    defaultConfig {
        applicationId = "top.kagg886.eoa.androidApp"
        minSdk = if (useDesugarApi) 23 else 28
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }

    packaging.resources.excludes += "META-INF/DEPENDENCIES"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
        isCoreLibraryDesugaringEnabled = useDesugarApi
    }

    kotlin {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_22)
    }

    signingConfigs {
        create("test") {
            storeFile = rootProject.file("composeApp/key.jks")
            storePassword = "123456"
            keyAlias = "kagg886"
            keyPassword = "123456"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = useProguard
            isShrinkResources = useProguard
            signingConfig = signingConfigs.getByName("test")
            ndk.abiFilters += "arm64-v8a"
        }
        debug {
            signingConfig = signingConfigs.getByName("test")
            ndk.abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }
}

dependencies {
    implementation(project.dependencies.project(":composeApp"))
    implementation(project.dependencies.project(":composeApp-backend"))
    implementation(project.dependencies.project(":util"))

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activityCompose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil)
    implementation(libs.filekit.dialog)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.materialKolor)
    implementation(libs.mkmb.core)

    androidTestImplementation(libs.androidx.uitest.junit4)
    debugImplementation(libs.androidx.uitest.testManifest)
    if (useDesugarApi) coreLibraryDesugaring(libs.desugar.jdk.libs)
}
