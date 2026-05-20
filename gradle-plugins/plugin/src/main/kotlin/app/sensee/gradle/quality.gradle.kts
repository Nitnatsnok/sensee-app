package app.sensee.gradle

import app.sensee.gradle.dsl.androidApplication
import app.sensee.gradle.dsl.androidKmp
import app.sensee.gradle.dsl.androidLibrary
import app.sensee.gradle.dsl.requiredLibrary
import app.sensee.gradle.dsl.requiredVersion
import com.android.build.api.dsl.Lint
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("dev.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.autonomousapps.dependency-analysis")
}

dependencies {
    detektPlugins(requiredLibrary("detekt-faire"))
    rootProject.findProject(":quality:detekt-rules")?.let { rulesProject ->
        if (project != rulesProject) {
            detektPlugins(rulesProject)
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    ignoreFailures = false
    failOnSeverity = FailOnSeverity.Error

    config.setFrom(rootProject.files("config/detekt/detekt.yml"))

    source.setFrom(
        files(
            "src",
            "build.gradle.kts",
            "settings.gradle.kts",
        ),
    )
}

tasks.withType<Detekt>().configureEach {
    include("**/*.kt")
    include("**/*.kts")

    // Pattern-based excludes for the aggregate task. Per-source-set tasks
    // (detektMainJvm, detektCommonMainSourceSet, …) pull their source from the
    // Kotlin source set — which on this project includes SqlDelight-generated
    // code under build/generated/ — and ignore Ant-style patterns, so the
    // lambda-based `exclude { … }` below is what actually filters those.
    exclude("**/build/**")
    exclude("**/.gradle/**")
    exclude("**/generated/**")
    exclude("**/build/generated/**")
    exclude("**/build/generated-src/**")
    exclude { fileTreeElement ->
        val path = fileTreeElement.file.absolutePath.replace('\\', '/')
        "/build/generated/" in path || "/build/generated-src/" in path
    }

    reports {
        html.required.set(true)
        checkstyle.required.set(false)
        sarif.required.set(false)
        markdown.required.set(false)
    }
}

// Per-compilation TR tasks (detektMain<Target>) cover the same commonMain code
// as the syntactic per-source-set tasks (detekt<X>SourceSet), so the latter
// only triple-reports the former; disable them. `database-schema` modules are
// SqlDelight-generated only, so TR-detekt on them is all noise.
val isSchemaOnlyModule = project.path.endsWith(":database-schema")

tasks.withType<Detekt>().configureEach {
    val taskName = name
    if (taskName.endsWith("SourceSet") && taskName != "detekt") {
        enabled = false
    } else if (isSchemaOnlyModule && taskName != "detekt") {
        enabled = false
    }
}

// Detekt 2.x does not self-wire `check -> detekt`, and the aggregate `detekt`
// task does not depend on per-compilation TR tasks. Wire both so the AGENTS.md
// commands (`gradlew check`, `gradlew detekt`) actually include TR-only rules.
plugins.withType<LifecycleBasePlugin> {
    tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
        dependsOn(tasks.withType<Detekt>())
    }
}

tasks.matching { it.name == "detekt" }.configureEach {
    dependsOn(tasks.withType<Detekt>().matching { it.name != "detekt" })
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    include("**/*.kt")
    include("**/*.kts")

    exclude("**/build/**")
    exclude("**/.gradle/**")
    exclude("**/generated/**")
    exclude("**/build/generated/**")
    exclude("**/build/generated-src/**")
}

ktlint {
    version.set(requiredVersion("ktlint-engine").requiredVersion)

    verbose.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)

    reporters {
        reporter(ReporterType.PLAIN)
    }

    filter {
        exclude { fileTreeElement ->
            val path = fileTreeElement.file.path.replace('\\', '/')
            path.contains("/build/") ||
                path.contains("/.gradle/") ||
                path.contains("/generated/")
        }
    }
}

applyLintDefaultsToAllAndroidPlugins()

private fun Project.applyLintDefaultsToAllAndroidPlugins() {
    plugins.withId("com.android.application") { androidApplication { lint(::configureLintDefaults) } }
    plugins.withId("com.android.library") { androidLibrary { lint(::configureLintDefaults) } }
    plugins.withId("com.android.kotlin.multiplatform.library") { androidKmp { lint(::configureLintDefaults) } }
}

private fun configureLintDefaults(lint: Lint) {
    with(lint) {
        abortOnError = true
        warningsAsErrors = false

        checkDependencies = false

        // lintVital hangs on AGP 9.x KMP Android-library.
        checkReleaseBuilds = false

        htmlReport = true
        xmlReport = false
        sarifReport = false

        lintConfig = rootProject.file("config/lint/lint.xml")
    }
}
