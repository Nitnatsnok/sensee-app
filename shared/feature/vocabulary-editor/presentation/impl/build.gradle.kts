plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.core.compose)
                implementation(projects.shared.core.decompose)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.observability)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.metro.runtime)
                implementation(projects.shared.core.presentation)
                implementation(projects.shared.ai.core)
                implementation(projects.shared.feature.vocabularyEditor.domain)
                implementation(projects.shared.feature.vocabularyEditor.presentation.api)
                implementation(projects.shared.feature.vocabularyEditor.presentation.navigationApi)
                implementation(projects.shared.ui.designSystem)
                implementation(projects.shared.ui.senseCard)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(projects.shared.core.testKit)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.vocabularyEditor.presentation.impl"
    }
}
