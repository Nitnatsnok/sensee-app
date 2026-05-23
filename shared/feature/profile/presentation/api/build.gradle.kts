plugins {
    alias(libs.plugins.sensee.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.core.decompose)
                api(projects.shared.core.presentation)
                api(projects.shared.settings.domain)
                api(libs.decompose)
                api(libs.kotlinx.collections.immutable)
                api(projects.shared.feature.profile.presentation.navigationApi)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.profile.presentation.api"
    }
}
