plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.sqldelight.extensions.coroutines)
                implementation(libs.metro.runtime)
                implementation(projects.shared.core.platform)
                implementation(projects.shared.database)
                implementation(projects.shared.feature.library.domain)
                implementation(projects.shared.feature.practice.domain)
                implementation(projects.shared.srs.fsrsEngine)
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
        namespace = "app.sensee.feature.practice.data"
    }
}
