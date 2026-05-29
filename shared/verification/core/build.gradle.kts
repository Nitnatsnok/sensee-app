plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // `suspend` is part of the Kotlin language; no kotlinx-coroutines
                // dep needed here. Contracts that genuinely need Flow/Channel
                // can add it back at that point.
                api(libs.kotlinx.serialization.core)
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
        namespace = "app.sensee.verification.core"
    }
}
