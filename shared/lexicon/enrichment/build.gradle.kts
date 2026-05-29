plugins {
    alias(libs.plugins.sensee.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.ai.core)
                api(projects.shared.grammar.domain)
                api(projects.shared.lexicon.domain)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
    android {
        namespace = "app.sensee.lexicon.enrichment"
    }
}
