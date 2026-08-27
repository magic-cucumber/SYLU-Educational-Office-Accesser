import org.gradle.api.Project

/**
 * ================================================
 * Author:     886kagg
 * Created on: 2025/9/16 13:28
 * ================================================
 */

val Project.useDesugarApi: Boolean
    get() = (System.getenv("APP_DESUGAR") ?: System.getProperty("APP_DESUGAR") ?: findProperty("app.desugar"))
        .toString().toBooleanStrictOrNull() ?: false

val Project.useProguard: Boolean
    get() = (System.getenv("APP_PROGUARD") ?: System.getProperty("APP_PROGUARD") ?: findProperty("app.proguard"))
        .toString().toBooleanStrictOrNull() ?: false

private fun Project.requiredStringProperty(name: String): String {
    val environmentName = name.replace('.', '_').uppercase()
    return (System.getenv(environmentName)
        ?: System.getProperty(environmentName)
        ?: findProperty(name))?.toString()
        ?: error("Missing required Gradle property: $name")
}

val Project.messageMail: String
    get() = requiredStringProperty("message.mail")

val Project.messageWebsiteUrl: String
    get() = requiredStringProperty("message.website.url")

val Project.messageQQGroupUrl: String
    get() = requiredStringProperty("message.qqgroup.url")

val Project.messageQQGroupLabel: String
    get() = requiredStringProperty("message.qqgroup.label")

val Project.messageGiteeHost: String
    get() = requiredStringProperty("message.gitee.host")

val Project.messageApiEndpoint: String
    get() = requiredStringProperty("message.api.endpoint")
