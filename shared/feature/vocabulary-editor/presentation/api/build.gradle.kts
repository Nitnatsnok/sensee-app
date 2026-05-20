plugins {
    alias(libs.plugins.sensee.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.core.decompose)
                api(projects.shared.core.presentation)
                implementation(projects.shared.feature.vocabularyEditor.presentation.navigationApi)
                api(projects.shared.feature.vocabularyEditor.domain)
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
        namespace = "app.sensee.feature.vocabularyEditor.presentation.api"
    }
}
