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
