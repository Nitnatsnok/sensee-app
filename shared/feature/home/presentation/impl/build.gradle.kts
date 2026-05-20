plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.core.decompose)
                implementation(libs.decompose.extensions.compose)
                implementation(libs.metro.runtime)
                implementation(projects.shared.feature.home.presentation.api)
                implementation(projects.shared.feature.home.presentation.navigationApi)
                implementation(projects.shared.ui.designSystem)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.home.presentation.impl"
    }
}
