plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.grammar.domain)
                api(projects.shared.ai.core)
                api(libs.kotlinx.coroutines.core)
                implementation(projects.shared.core.observability)
                implementation(libs.metro.runtime)
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
        namespace = "app.sensee.feature.vocabularyEditor.domain"
    }
}
