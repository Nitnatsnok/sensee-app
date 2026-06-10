plugins {
    alias(libs.plugins.sensee.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(projects.shared.core.decompose)
                api(projects.shared.core.presentation)
                implementation(projects.shared.feature.home.presentation.navigationApi)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.home.presentation.api"
    }
}
