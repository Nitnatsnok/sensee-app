plugins {
    alias(libs.plugins.sensee.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.feature.startup.domain)
                api(libs.kotlinx.coroutines.core)
                implementation(projects.shared.core.decompose)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.startup.presentation.api"
    }
}
