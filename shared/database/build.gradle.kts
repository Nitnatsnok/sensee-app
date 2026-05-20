plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.metro)
}

evaluationDependsOn(":shared:feature:library:database-schema")
evaluationDependsOn(":shared:feature:practice:database-schema")
evaluationDependsOn(":shared:feature:vocabulary-editor:database-schema")
evaluationDependsOn(":shared:settings:database-schema")
evaluationDependsOn(":shared:tts:database-schema")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.core.platform)
                api(projects.shared.feature.library.databaseSchema)
                api(projects.shared.feature.practice.databaseSchema)
                api(projects.shared.feature.vocabularyEditor.databaseSchema)
                api(projects.shared.settings.databaseSchema)
                api(projects.shared.tts.databaseSchema)
                api(libs.sqldelight.runtime)
                implementation(projects.shared.core.observability)
                implementation(libs.metro.runtime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.sqldelight.extensions.coroutines)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.sqldelight.driver.android)
            }
        }

        nativeMain {
            dependencies {
                implementation(libs.sqldelight.driver.native)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.sqldelight.driver.sqlite)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.sqldelight.driver.sqlite)
            }
        }

        webMain {
            dependencies {
                implementation(libs.sqldelight.driver.webWorker)
                implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.3.2"))
                implementation(npm("sql.js", "1.8.0"))
                implementation(devNpm("copy-webpack-plugin", "9.1.0"))
            }
        }
    }

    android {
        namespace = "app.sensee.database"
    }
}

sqldelight {
    databases {
        register("SenseeDatabase") {
            packageName.set("app.sensee.database")
            generateAsync.set(true)
            // Build-time check: walk every .sqm migration from a snapshot and assert
            // the resulting schema matches the current .sq files. Catches "bumped
            // schema but forgot to add a migration".
            verifyMigrations.set(true)
            // Versioned schema snapshots live next to the .sq files. Regenerate via
            // `./gradlew :shared:database:generateSenseeDatabaseSchema` after every
            // schema bump and commit the result.
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            dependency(project(":shared:feature:library:database-schema"))
            dependency(project(":shared:feature:practice:database-schema"))
            dependency(project(":shared:feature:vocabulary-editor:database-schema"))
            dependency(project(":shared:settings:database-schema"))
            dependency(project(":shared:tts:database-schema"))
        }
    }
}
