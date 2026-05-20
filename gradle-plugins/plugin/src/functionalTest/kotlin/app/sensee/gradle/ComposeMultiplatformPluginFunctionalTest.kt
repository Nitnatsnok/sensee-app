package app.sensee.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeMultiplatformPluginFunctionalTest {
    @Test
    fun `plugin wires compose compiler stability configuration from baseline module and dependencies`() {
        val repositoryRoot = locateRepositoryRoot()
        val testProjectDir = Files.createTempDirectory("compose-multiplatform-functional-test")

        try {
            writeTestProject(testProjectDir, repositoryRoot)

            val result =
                GradleRunner
                    .create()
                    .withProjectDir(testProjectDir.toFile())
                    .withPluginClasspath()
                    .withArguments("verifyComposeStabilityContract", "--stacktrace")
                    .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":verifyComposeStabilityContract")?.outcome)
        } finally {
            testProjectDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `plugin registers compose module in root aggregation tasks without subprojects block`() {
        val repositoryRoot = locateRepositoryRoot()
        val testProjectDir = Files.createTempDirectory("compose-multiplatform-root-aggregation-test")

        try {
            writeRootAggregationTestProject(testProjectDir, repositoryRoot)

            val result =
                GradleRunner
                    .create()
                    .withProjectDir(testProjectDir.toFile())
                    .withPluginClasspath()
                    .withArguments("verifyRootAggregation", "--stacktrace")
                    .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":verifyRootAggregation")?.outcome)
        } finally {
            testProjectDir.toFile().deleteRecursively()
        }
    }

    private fun writeTestProject(
        testProjectDir: Path,
        repositoryRoot: Path,
    ) {
        testProjectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }

            rootProject.name = "functional-test-project"
            include(":dep")
            """.trimIndent(),
        )

        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            import org.gradle.api.tasks.Sync

            plugins {
                id("app.sensee.gradle.kmp-library")
                id("app.sensee.gradle.compose-multiplatform")
                id("org.jetbrains.compose") apply false
                id("org.jetbrains.kotlin.plugin.compose") apply false
            }

            tasks.register("collectComposeStabilityReports", Sync::class) {
                into(layout.buildDirectory.dir("reports/compose_compiler"))
            }

            kotlin {
                android {
                    namespace = "app.sensee.test"
                }

                sourceSets {
                    commonMain {
                        dependencies {
                            implementation(project(":dep"))
                        }
                    }
                }
            }

            tasks.register("verifyComposeStabilityContract") {
                doLast {
                    check(pluginManager.hasPlugin("org.jetbrains.compose"))
                    check(pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.compose"))

                    check(tasks.names.contains("composeStabilityReport")) {
                        "Expected composeStabilityReport task to be registered"
                    }

                    val collectTask = tasks.named("collectComposeStabilityReports").get() as Sync
                    check(collectTask.destinationDir.canonicalFile == file("build/reports/compose_compiler").canonicalFile) {
                        "Expected collectComposeStabilityReports to target build/reports/compose_compiler"
                    }
                }
            }
            """.trimIndent(),
        )

        testProjectDir.resolve("dep").createDirectories()
        testProjectDir.resolve("dep/build.gradle.kts").writeText(
            """
            plugins {
                id("app.sensee.gradle.kmp-library")
            }

            kotlin {
                android {
                    namespace = "app.sensee.test.dep"
                }
            }
            """.trimIndent(),
        )

        testProjectDir.resolve("config/compose").createDirectories()
        testProjectDir.resolve("config/compose/stability.conf").writeText(
            """
            com.example.BaselineStable
            """.trimIndent(),
        )
        testProjectDir.resolve("compose-stability.conf").writeText(
            """
            com.example.RootStable
            """.trimIndent(),
        )
        testProjectDir.resolve("dep/compose-stability.conf").writeText(
            """
            com.example.DependencyStable
            """.trimIndent(),
        )

        val gradleDir = testProjectDir.resolve("gradle").createDirectories()
        repositoryRoot
            .resolve("gradle/libs.versions.toml")
            .copyTo(gradleDir.resolve("libs.versions.toml"))
    }

    private fun writeRootAggregationTestProject(
        testProjectDir: Path,
        repositoryRoot: Path,
    ) {
        testProjectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }

            rootProject.name = "root-aggregation-functional-test-project"
            include(":compose-module")
            """.trimIndent(),
        )

        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            import org.gradle.api.tasks.Sync

            plugins {
                id("org.jetbrains.compose") apply false
                id("org.jetbrains.kotlin.plugin.compose") apply false
            }

            tasks.register("collectComposeStabilityReports", Sync::class) {
                into(layout.buildDirectory.dir("reports/compose_compiler"))
            }

            tasks.register("composeStabilityReport") {
                dependsOn("collectComposeStabilityReports")
            }

            tasks.register("verifyRootAggregation") {
                doLast {
                    val rootAggregateTask = tasks.named("composeStabilityReport").get()
                    val composeModuleReportTask = project(":compose-module").tasks.named("composeStabilityReport").get()
                    val rootDependencies = rootAggregateTask.taskDependencies.getDependencies(rootAggregateTask)

                    check(rootDependencies.contains(composeModuleReportTask)) {
                        "Expected root composeStabilityReport to depend on :compose-module:composeStabilityReport"
                    }

                    val reportsTask = tasks.named("collectComposeStabilityReports").get() as Sync

                    check(reportsTask.destinationDir.canonicalFile == file("build/reports/compose_compiler").canonicalFile)
                }
            }
            """.trimIndent(),
        )

        testProjectDir.resolve("compose-module").createDirectories()
        testProjectDir.resolve("compose-module/build.gradle.kts").writeText(
            """
            plugins {
                id("app.sensee.gradle.kmp-library")
                id("app.sensee.gradle.compose-multiplatform")
            }

            kotlin {
                android {
                    namespace = "app.sensee.test.composemodule"
                }
            }
            """.trimIndent(),
        )

        val gradleDir = testProjectDir.resolve("gradle").createDirectories()
        repositoryRoot
            .resolve("gradle/libs.versions.toml")
            .copyTo(gradleDir.resolve("libs.versions.toml"))
    }

    private fun locateRepositoryRoot(): Path {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath()

        while (current.parent != null) {
            if (
                current.resolve("settings.gradle.kts").exists() &&
                current.resolve("gradle/libs.versions.toml").exists() &&
                current.resolve("apps").exists() &&
                current.resolve("shared").exists()
            ) {
                return current
            }
            current = current.parent
        }

        error("Could not locate repository root from ${System.getProperty("user.dir")}")
    }
}
