plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.verification.core)
                api(projects.shared.verification.senseeCurated)
                api(projects.shared.verification.freeDictionary)
                api(projects.shared.verification.datamuse)
                api(projects.shared.verification.languagetool)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.network)
                implementation(projects.shared.core.observability)
                implementation(projects.shared.database)
                implementation(projects.shared.verification.databaseSchema)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.metro.runtime)
                implementation(libs.sqldelight.extensions.coroutines)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.sqldelight.driver.sqlite)
            }
        }
    }
    android {
        namespace = "app.sensee.verification.integration"
    }
}
