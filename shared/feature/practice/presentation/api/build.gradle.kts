plugins {
    alias(libs.plugins.sensee.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.collections.immutable)
                api(libs.decompose)
                api(projects.shared.core.presentation)
                api(projects.shared.core.decompose)
                api(projects.shared.feature.library.domain)
                api(projects.shared.feature.practice.domain)
                implementation(projects.shared.feature.practice.presentation.navigationApi)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.practice.presentation.api"
    }
}
