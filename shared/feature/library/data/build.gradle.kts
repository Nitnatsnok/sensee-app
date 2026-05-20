plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.sqldelight.extensions.coroutines)
                implementation(libs.metro.runtime)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.mockBackend)
                implementation(projects.shared.core.network)
                implementation(projects.shared.core.platform)
                implementation(projects.shared.database)
                implementation(projects.shared.feature.library.domain)
                implementation(projects.shared.feature.vocabularyEditor.domain)
                implementation(projects.shared.srs.core)
                implementation(projects.shared.srs.engine)
                implementation(projects.shared.srs.fsrs)
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
                implementation(libs.ktor.client.mock)
                implementation(libs.sqldelight.driver.sqlite)
                implementation(projects.shared.core.testKit)
                implementation(projects.shared.srs.testKit)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.library.data"
    }
}
