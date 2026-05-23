plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.settings.domain)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.mockBackend)
                implementation(projects.shared.core.network)
                implementation(projects.shared.core.platform)
                implementation(projects.shared.core.secureStorage)
                implementation(projects.shared.database)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
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
                implementation(projects.shared.core.testKit)
                implementation(libs.sqldelight.driver.sqlite)
            }
        }
    }
    android {
        namespace = "app.sensee.settings.data"
    }
}
