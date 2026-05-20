plugins {
    alias(libs.plugins.sensee.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.collections.immutable)
                api(projects.shared.core.presentation)
                api(projects.shared.core.decompose)
                implementation(projects.shared.feature.library.presentation.navigationApi)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.library.presentation.api"
    }
}
