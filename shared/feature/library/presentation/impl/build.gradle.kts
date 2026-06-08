plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.decompose)
                implementation(libs.decompose.extensions.compose)
                implementation(libs.metro.runtime)
                implementation(projects.shared.core.compose)
                implementation(projects.shared.feature.library.domain)
                implementation(projects.shared.feature.library.presentation.api)
                implementation(projects.shared.feature.library.presentation.navigationApi)
                implementation(projects.shared.ui.designSystem)
                implementation(projects.shared.ui.adaptive)
                implementation(projects.shared.ui.senseCard)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(projects.shared.feature.library.domain)
                implementation(projects.shared.core.testKit)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.library.presentation.impl"
    }
}
