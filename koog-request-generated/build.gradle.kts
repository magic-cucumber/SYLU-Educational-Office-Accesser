plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

group = "top.kagg886.eoa.demo"
version = "1.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.koog.agents)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.logging)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt") // 注意：文件名是 main.kt，类名通常是 MainKt
}
