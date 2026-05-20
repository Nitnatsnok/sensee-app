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

class KmpLibraryPluginFunctionalTest {
    @Test
    fun `plugin applies wrapped plugins and wires kotlin sensee DSL`() {
        val repositoryRoot = locateRepositoryRoot()
        val versions = loadPluginVersions(repositoryRoot)
        val testProjectDir = Files.createTempDirectory("kmp-library-functional-test")

        try {
            writeTestProject(testProjectDir, repositoryRoot, versions)

            val result =
                GradleRunner
                    .create()
                    .withProjectDir(testProjectDir.toFile())
                    .withPluginClasspath()
                    .withArguments("verifyPluginContract", "--stacktrace")
                    .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":verifyPluginContract")?.outcome)
        } finally {
            testProjectDir.toFile().deleteRecursively()
        }
    }

    private fun writeTestProject(
        testProjectDir: Path,
        repositoryRoot: Path,
        versions: PluginVersions,
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
            """.trimIndent(),
        )

        testProjectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("app.sensee.gradle.kmp-library")
                id("org.jetbrains.kotlin.multiplatform") version "${versions.kotlin}" apply false
                id("com.android.kotlin.multiplatform.library") version "${versions.agp}" apply false
                id("com.android.lint") version "${versions.agp}" apply false
            }

            kotlin {
                android {
                    namespace = "app.sensee.test"
                }
            }

            tasks.register("verifyPluginContract") {
                doLast {
                    check(pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform"))
                    check(pluginManager.hasPlugin("com.android.kotlin.multiplatform.library"))
                    check(pluginManager.hasPlugin("com.android.lint"))
                    check(pluginManager.hasPlugin("app.sensee.gradle.kmp-web-toolchain"))
                    check(pluginManager.hasPlugin("app.sensee.gradle.quality"))

                    val expectedTasks = setOf(
                        "compileKotlinIosArm64",
                        "compileKotlinIosSimulatorArm64",
                        "compileKotlinJvm",
                        "compileKotlinJs",
                        "compileKotlinWasmJs",
                        "jsBrowserTest",
                        "wasmJsBrowserTest",
                        "detekt",
                        "ktlintCheck",
                        "lintJvm",
                    )
                    val actualTasks = tasks.names

                    check(actualTasks.containsAll(expectedTasks)) {
                        "Missing tasks: ${'$'}{expectedTasks - actualTasks}, actual: ${'$'}actualTasks"
                    }

                    check(tasks.names.any { it.contains("android", ignoreCase = true) }) {
                        "Expected Android-related tasks to be registered"
                    }
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

    private fun loadPluginVersions(repositoryRoot: Path): PluginVersions {
        val catalogText = repositoryRoot.resolve("gradle/libs.versions.toml").toFile().readText()
        return PluginVersions(
            kotlin = requireCatalogVersion(catalogText, "kotlin"),
            agp = requireCatalogVersion(catalogText, "agp"),
        )
    }

    private fun requireCatalogVersion(
        catalogText: String,
        key: String,
    ): String {
        var inVersionsSection = false

        catalogText.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line == "[versions]" -> inVersionsSection = true
                inVersionsSection && line.startsWith("[") -> return@forEach
                inVersionsSection && line.startsWith("$key = ") -> {
                    return line.substringAfter('"').substringBeforeLast('"')
                }
            }
        }

        error("Could not find version '$key' in libs.versions.toml")
    }

    private data class PluginVersions(
        val kotlin: String,
        val agp: String,
    )
}
