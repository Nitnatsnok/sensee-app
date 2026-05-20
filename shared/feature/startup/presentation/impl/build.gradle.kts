plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.kotlinx.coroutines.core)

                implementation(projects.shared.core.compose)
                implementation(projects.shared.core.decompose)
                implementation(libs.metro.runtime)
                implementation(projects.shared.core.presentation)
                implementation(projects.shared.core.tracing)
                implementation(projects.shared.feature.startup.presentation.api)
                implementation(projects.shared.grammar.data)
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
        namespace = "app.sensee.feature.startup.presentation.impl"
    }
}
