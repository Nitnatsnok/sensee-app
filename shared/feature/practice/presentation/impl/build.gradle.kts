plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.decompose)
                implementation(libs.decompose.extensions.compose)
                implementation(projects.shared.feature.library.domain)
                implementation(projects.shared.feature.practice.domain)
                implementation(projects.shared.feature.practice.presentation.api)
                implementation(projects.shared.feature.practice.presentation.navigationApi)
                implementation(projects.shared.core.compose)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.decompose)
                implementation(libs.metro.runtime)
                implementation(projects.shared.core.observability)
                implementation(projects.shared.srs.core)
                implementation(projects.shared.tts.core)
                implementation(projects.shared.ui.designSystem)
                implementation(projects.shared.ui.adaptive)
                implementation(projects.shared.ui.learningDeck)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(projects.shared.core.testKit)
                implementation(projects.shared.srs.testKit)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.practice.presentation.impl"
    }
}
