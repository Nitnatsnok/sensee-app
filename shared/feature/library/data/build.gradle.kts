plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.mockFixtures)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.metro)
}

mockFixtures {
    packageName.set("app.sensee.feature.library.data.remote")
    className.set("CatalogMockFixtures")
    classKdoc.set(
        """
        Library's catalog (mock backend). Three themed decks of ten cards each;
        four cards live in two decks to exercise true many-to-many — a shared
        card maps to one canonical sense (same source_ref) referenced by several
        decks' membership. The shared card JSON is duplicated across the deck
        fixtures and `ServiceDeckSenseFixtureTest` pins that it maps into the same
        Sense across the decks that reference it.
        """.trimIndent(),
    )
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.sqldelight.extensions.coroutines)
                implementation(libs.metro.runtime)
                implementation(projects.shared.lexicon.domain)
                implementation(projects.shared.lexicon.serialization)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.mockBackend)
                implementation(projects.shared.core.network)
                implementation(projects.shared.core.observability)
                implementation(projects.shared.core.platform)
                implementation(projects.shared.database)
                implementation(projects.shared.feature.library.domain)
                implementation(projects.shared.srs.core)
                implementation(projects.shared.srs.engine)
                implementation(projects.shared.srs.fsrs)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
                implementation(projects.shared.lexicon.domain)
                implementation(projects.shared.lexicon.serialization)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.ktor.client.mock)
                implementation(libs.sqldelight.driver.sqlite)
                implementation(projects.shared.core.testKit)
                implementation(projects.shared.lexicon.data)
                implementation(projects.shared.srs.testKit)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.library.data"
    }
}
