plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.feature.vocabularyEditor.domain)
                implementation(projects.shared.lexicon.serialization)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.observability)
                implementation(projects.shared.database)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.sqldelight.extensions.coroutines)
                implementation(libs.metro.runtime)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.sqldelight.driver.sqlite)
                implementation(projects.shared.core.testKit)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.vocabularyEditor.data"
    }
}
