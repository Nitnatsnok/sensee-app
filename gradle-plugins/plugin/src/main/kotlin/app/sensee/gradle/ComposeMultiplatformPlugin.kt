package app.sensee.gradle

import app.sensee.gradle.dsl.ensurePlugin
import app.sensee.gradle.dsl.requiredLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import java.io.File

class ComposeMultiplatformPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            ensurePlugin("org.jetbrains.compose")
            ensurePlugin("org.jetbrains.kotlin.plugin.compose")
            ensurePlugin("app.sensee.gradle.quality.compose")

            enableComposePreviewTooling()
            configureComposeStability()
            registerComposeStabilityReportTask()
            registerWithRootComposeStabilityAggregation()
        }
}

// IDE @Preview rendering needs the ui-tooling renderer on the Android runtime classpath.
// The `com.android.kotlin.multiplatform.library` plugin has no `debug` variant, so the
// usual `debugImplementation` seam is unavailable — `androidRuntimeClasspath` is the
// documented hook for KMP library modules (Compose Multiplatform 1.10+). Centralized
// here so every Compose KMP-library module gets it without per-module boilerplate.
private fun Project.enableComposePreviewTooling() {
    pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
        dependencies.addProvider("androidRuntimeClasspath", requiredLibrary("compose-uiTooling"))
    }
}

private fun Project.configureComposeStability() {
    val stabilityConfigurationFilesProvider =
        provider {
            resolveComposeStabilityConfigurationFiles().map { stabilityConfigurationFile ->
                val relativePath = stabilityConfigurationFile.relativeTo(rootProject.projectDir).path.replace('\\', '/')
                rootProject.layout.projectDirectory.file(relativePath)
            }
        }

    // Metrics/reports are an opt-in diagnostic produced only by the stability-report
    // task. Setting the destinations unconditionally makes the Compose compiler write
    // metrics JSON on *every* compilation, including non-JVM ones — and the writer
    // throws `IOException: Invalid file path` on the JS/Wasm targets (module-name →
    // file-path on Windows). Gate the destinations on an actually-requested report run;
    // `stabilityConfigurationFiles` stays always (it only affects inference, no file I/O).
    val reportsRequested = isComposeStabilityRefreshRequested()
    pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
        extensions.configure<ComposeCompilerGradlePluginExtension> {
            stabilityConfigurationFiles.addAll(stabilityConfigurationFilesProvider)
            if (reportsRequested) {
                reportsDestination.set(layout.buildDirectory.dir(COMPOSE_OUTPUT_DESTINATION))
                metricsDestination.set(layout.buildDirectory.dir(COMPOSE_OUTPUT_DESTINATION))
            }
        }
    }
}

private fun Project.registerComposeStabilityReportTask() {
    val composeStabilityRefreshRequested = isComposeStabilityRefreshRequested()

    tasks.configureEach {
        if (name.isComposeStabilityReportCompilationTaskName()) {
            outputs.upToDateWhen {
                !composeStabilityRefreshRequested
            }
        }
    }

    tasks.register(COMPOSE_STABILITY_REPORT_TASK_NAME) {
        group = "verification"
        description = "Generates Compose compiler stability reports for this module."

        dependsOn(
            tasks.matching { task ->
                task.name.isComposeStabilityReportCompilationTaskName()
            },
        )
    }
}

private fun Project.registerWithRootComposeStabilityAggregation() {
    if (this == rootProject) {
        return
    }

    val modulePath = projectDir.relativeTo(rootProject.projectDir).path.replace(File.separatorChar, '/')
    val moduleReportTask = tasks.named(COMPOSE_STABILITY_REPORT_TASK_NAME)

    rootProject.tasks.named(COMPOSE_STABILITY_REPORT_TASK_NAME) {
        dependsOn(moduleReportTask)
    }

    rootProject.tasks.named<Sync>(COLLECT_COMPOSE_STABILITY_REPORTS_TASK_NAME) {
        dependsOn(moduleReportTask)

        from(layout.buildDirectory.dir(COMPOSE_OUTPUT_DESTINATION)) {
            into(modulePath)
        }
    }
}

private fun Project.resolveComposeStabilityConfigurationFiles(): List<File> {
    val files = linkedSetOf<File>()

    rootProject.layout.projectDirectory
        .file(COMPOSE_STABILITY_BASELINE_PATH)
        .asFile
        .takeIf(File::isFile)
        ?.let(files::add)

    projectDir
        .resolve(COMPOSE_STABILITY_CONFIG_FILE_NAME)
        .takeIf(File::isFile)
        ?.let(files::add)

    collectDirectComposeStabilityDependencyProjects()
        .map { dependencyProject -> dependencyProject.projectDir.resolve(COMPOSE_STABILITY_CONFIG_FILE_NAME) }
        .filter(File::isFile)
        .forEach(files::add)

    return files.toList()
}

private fun Project.collectDirectComposeStabilityDependencyProjects(): Set<Project> {
    val dependencyProjects = linkedSetOf<Project>()

    configurations
        .filter { configuration -> configuration.name.isComposeStabilityDependencyConfiguration() }
        .forEach { configuration ->
            configuration.dependencies.forEach { dependency ->
                val projectDependency = dependency as? ProjectDependency ?: return@forEach
                val dependencyProject = rootProject.findProject(projectDependency.path) ?: return@forEach

                dependencyProjects += dependencyProject
            }
        }

    return dependencyProjects
}

private fun String.isComposeStabilityDependencyConfiguration(): Boolean {
    val lowerCaseName = lowercase()

    if (COMPOSE_STABILITY_DEPENDENCY_EXCLUDED_NAME_PARTS.any(lowerCaseName::contains)) {
        return false
    }

    return COMPOSE_STABILITY_DEPENDENCY_ALLOWED_SUFFIXES.any(this::endsWith)
}

private fun String.isComposeStabilityReportCompilationTaskName(): Boolean =
    this in COMPOSE_STABILITY_REPORT_COMPILATION_TASK_NAMES

private fun Project.isComposeStabilityRefreshRequested(): Boolean =
    gradle.startParameter.taskNames.any(String::isComposeStabilityRefreshTaskName)

private fun String.isComposeStabilityRefreshTaskName(): Boolean =
    this == COMPOSE_STABILITY_REPORT_TASK_NAME ||
        this.endsWith(":$COMPOSE_STABILITY_REPORT_TASK_NAME") ||
        this == COLLECT_COMPOSE_STABILITY_REPORTS_TASK_NAME ||
        this.endsWith(":$COLLECT_COMPOSE_STABILITY_REPORTS_TASK_NAME")

private const val COMPOSE_STABILITY_BASELINE_PATH = "config/compose/stability.conf"
private const val COMPOSE_STABILITY_CONFIG_FILE_NAME = "compose-stability.conf"
private const val COMPOSE_OUTPUT_DESTINATION = "reports/compose_compiler"
private const val COMPOSE_STABILITY_REPORT_TASK_NAME = "composeStabilityReport"
private const val COLLECT_COMPOSE_STABILITY_REPORTS_TASK_NAME = "collectComposeStabilityReports"

private val COMPOSE_STABILITY_DEPENDENCY_EXCLUDED_NAME_PARTS =
    setOf(
        "test",
        "androidtest",
        "lint",
        "detekt",
        "ktlint",
    )

private val COMPOSE_STABILITY_DEPENDENCY_ALLOWED_SUFFIXES =
    setOf(
        "Implementation",
        "Api",
        "CompileOnly",
    )

private val COMPOSE_STABILITY_REPORT_COMPILATION_TASK_NAMES =
    setOf(
        "compileCommonMainKotlinMetadata",
        "compileKotlinJvm",
        "compileAndroidMain",
        "compileDebugKotlin",
        "compileReleaseKotlin",
    )
