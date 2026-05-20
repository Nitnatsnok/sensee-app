plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.tts.core)
                api(projects.shared.tts.system)
                api(projects.shared.tts.elevenlabs)
                api(projects.shared.tts.openai)
                api(projects.shared.tts.playback)
                implementation(projects.shared.tts.cache)
                implementation(projects.shared.database)
                implementation(projects.shared.core.observability)
                implementation(projects.shared.core.network)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.platform)
                implementation(projects.shared.settings.domain)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.metro.runtime)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
            }
        }
    }
    android {
        namespace = "app.sensee.tts.integration"
    }
}
