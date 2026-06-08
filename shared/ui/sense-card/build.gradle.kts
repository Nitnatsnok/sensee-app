plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.core.compose)
                implementation(projects.shared.core.presentation)
                implementation(projects.shared.ui.designSystem)
                implementation(projects.shared.lexicon.domain)
                implementation(projects.shared.grammar.domain)
                implementation(libs.kotlinx.collections.immutable)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
    android {
        namespace = "app.sensee.ui.senseCard"
    }
}
