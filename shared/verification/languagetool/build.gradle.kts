plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.kotlinx.serialization)
    // No metro plugin: this network adapter has no @Inject/@Contributes/@Provides;
    // VerificationIntegrationProviders constructs it manually.
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.verification.core)
                implementation(projects.shared.core.coroutines)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
    }
    android {
        namespace = "app.sensee.verification.languagetool"
    }
}
