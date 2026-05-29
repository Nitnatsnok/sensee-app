plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.mockFixtures)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.metro)
}

mockFixtures {
    packageName.set("app.sensee.ai.curatedEnrichment.remote")
    className.set("CuratedEnrichmentMockFixtures")
    classKdoc.set(
        """
        Curated AI enrichment served by the (mock) backend. One JSON file per
        covered lemma under `mockFixtures/enrichment/<lemma-slug>.json`, each a
        wire `EnrichmentResponseV1`. Edit/add a lemma by editing/adding a file;
        the real backend will serve the same shape. A missing lemma is a 404
        (curated miss) and the router falls through to the LLM.
        """.trimIndent(),
    )
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.ai.core)
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
        namespace = "app.sensee.ai.curatedEnrichment"
    }
}
