plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.ai.core)
                api(projects.shared.ai.fixture)
                api(projects.shared.ai.llm)
                implementation(projects.shared.settings.domain)
                implementation(projects.shared.core.network)
                implementation(projects.shared.core.observability)
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
        namespace = "app.sensee.ai.integration"
    }
}
