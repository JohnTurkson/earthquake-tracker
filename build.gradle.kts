import com.ncorti.ktfmt.gradle.tasks.KtfmtCheckTask
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask

plugins {
    base
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

ktfmt {
    kotlinLangStyle()
    maxWidth.set(100)
    trailingCommaManagementStrategy.set(
        com.ncorti.ktfmt.gradle.TrailingCommaManagementStrategy.COMPLETE
    )
}

val rootScripts = fileTree(rootDir) { include("*.kts") }

tasks.named<KtfmtCheckTask>("ktfmtCheckScripts") { source = rootScripts }

tasks.named<KtfmtFormatTask>("ktfmtFormatScripts") { source = rootScripts }

val kotlinFiles =
    fileTree(rootDir) {
        include("**/*.kt", "**/*.kts")
        exclude("*.kts")
        exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**", "**/.git/**")
        exclude("**/.idea/**", "**/.agents/**", "**/.codex/**")
    }

val ktfmtCheckSources =
    tasks.register<KtfmtCheckTask>("ktfmtCheckSources") {
        description =
            "Checks formatting of Kotlin sources and module scripts throughout the repository."
        source = kotlinFiles
    }
val ktfmtFormatSources =
    tasks.register<KtfmtFormatTask>("ktfmtFormatSources") {
        description = "Formats Kotlin sources and module scripts throughout the repository."
        source = kotlinFiles
    }

tasks.named("ktfmtCheck") { dependsOn(ktfmtCheckSources) }

tasks.named("ktfmtFormat") { dependsOn(ktfmtFormatSources) }

tasks.named("check") { dependsOn("ktfmtCheck") }

subprojects {
    tasks
        .matching { it.name == "check" }
        .configureEach { dependsOn(rootProject.tasks.named("ktfmtCheck")) }
}
