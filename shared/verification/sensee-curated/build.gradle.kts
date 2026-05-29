plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.mockFixtures)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.metro)
}

mockFixtures {
    packageName.set("app.sensee.verification.senseeCurated.remote")
    className.set("SenseeCuratedMockFixtures")
    classKdoc.set(
        """
        Sensee-curated lexical reference served by the (mock) backend. One
        editable JSON catalog per facet under `mockFixtures/verification/`:
        `frequency` (lemma → zipf), `cefr` (lemma → level), `senses`
        (lemma → glosses), `family` (head lemma → units). Edit/add coverage by
        editing these files; the real backend will serve the same shapes.
        """.trimIndent(),
    )
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.verification.core)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.mockBackend)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
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
                implementation(libs.ktor.client.mock)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }
    }
    android {
        namespace = "app.sensee.verification.senseeCurated"
    }
}
