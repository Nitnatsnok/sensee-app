plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.lexicon.domain)
                api(projects.shared.grammar.domain)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
    android {
        namespace = "app.sensee.lexicon.serialization"
    }
}
