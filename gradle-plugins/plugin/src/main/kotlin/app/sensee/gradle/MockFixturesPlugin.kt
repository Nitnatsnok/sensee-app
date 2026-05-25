package app.sensee.gradle

import app.sensee.gradle.dsl.ensurePlugin
import app.sensee.gradle.dsl.kotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceTask
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Generates a `MockFixtureSet` implementation from JSON files under
 * `src/commonMain/mockFixtures/`. Apply alongside `app.sensee.gradle.kmp-library`
 * and configure with the `mockFixtures { … }` extension.
 */
class MockFixturesPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            ensurePlugin("org.jetbrains.kotlin.multiplatform")

            val extension = extensions.create<MockFixturesExtension>("mockFixtures")
            val fixturesDir = layout.projectDirectory.dir("src/commonMain/mockFixtures")
            val outputDir = layout.buildDirectory.dir("generated/source/mockFixtures/commonMain/kotlin")

            val generate =
                tasks.register<MockFixturesCodegenTask>("generateMockFixtures") {
                    group = "codegen"
                    description = "Generates a MockFixtureSet from src/commonMain/mockFixtures/*.json."
                    this.fixturesDir.set(fixturesDir)
                    this.outputDir.set(outputDir)
                    packageName.set(extension.packageName)
                    className.set(extension.className)
                    classKdoc.set(extension.classKdoc)
                }

            kotlinMultiplatform {
                sourceSets.named("commonMain") {
                    kotlin.srcDir(generate.map { it.outputDir })
                }
            }

            // Kotlin compile tasks pick up the generated dir as a srcDir, but the
            // implicit task dependency does not cover sibling consumers
            // (ktlint/source-set tooling). Bind explicitly so every consumer
            // sees the generated file.
            tasks.withType<KotlinCompilationTask<*>>().configureEach { dependsOn(generate) }
            tasks.withType<SourceTask>().configureEach { dependsOn(generate) }
        }
}
