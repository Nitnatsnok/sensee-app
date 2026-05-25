import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(libs.gradle.plugin.kotlin)
    implementation(libs.gradle.plugin.kotlin.multiplatform)
    implementation(libs.gradle.plugin.kotlin.multiplatform.androidLibrary)
    implementation(libs.gradle.plugin.compose)
    implementation(libs.gradle.plugin.compose.compiler)
    implementation(libs.gradle.plugin.android.lint)
    implementation(libs.gradle.detekt)
    implementation(libs.gradle.ktlint)
    implementation(libs.gradle.dependencyAnalysis)
    implementation(libs.kotlinx.serialization.json)
}

gradlePlugin {
    plugins {
        register("kmpWebToolchain") {
            id = "app.sensee.gradle.kmp-web-toolchain"
            implementationClass = "app.sensee.gradle.KmpWebToolchainPlugin"
        }

        register("kmpLibrary") {
            id = "app.sensee.gradle.kmp-library"
            implementationClass = "app.sensee.gradle.KmpLibraryPlugin"
        }

        register("composeMultiplatform") {
            id = "app.sensee.gradle.compose-multiplatform"
            implementationClass = "app.sensee.gradle.ComposeMultiplatformPlugin"
        }

        register("mockFixtures") {
            id = "app.sensee.gradle.mock-fixtures"
            implementationClass = "app.sensee.gradle.MockFixturesPlugin"
        }
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest()

            dependencies {
                implementation(libs.gradle.plugin.kotlin)
                implementation(libs.gradle.plugin.kotlin.multiplatform)
                implementation(libs.gradle.plugin.kotlin.multiplatform.androidLibrary)
                implementation(libs.gradle.plugin.android)
            }
        }

        val functionalTest by registering(JvmTestSuite::class) {
            useKotlinTest()

            dependencies {
                implementation(gradleTestKit())
                implementation(project())
                implementation(libs.gradle.plugin.kotlin)
                implementation(libs.gradle.plugin.kotlin.multiplatform)
                implementation(libs.gradle.plugin.kotlin.multiplatform.androidLibrary)
                implementation(libs.gradle.plugin.android)
            }

            targets {
                all {
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

ktlint {
    version.set(libs.versions.ktlint.engine)

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
