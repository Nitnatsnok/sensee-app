plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.mockFixtures)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.metro)
}

mockFixtures {
    packageName.set("app.sensee.grammar.data")
    className.set("GrammarTaxonomyMockFixtures")
    classKdoc.set(
        """
        The single source of the `practice/grammar/taxonomy` fixture. Every id
        carries per-language labels (BCP-47 tag → `{long, short: { style -> abbr }}`);
        the resolver picks the right language/form/style per lookup. No other
        module may contribute this key. Adding a new UI language or short-form
        style is a fixture extension here — adding a new tag under `labels` or a
        new key under `short` for every entry — not a code change in `domain`.

        Short-form styles:
         - `lexicographic`: dictionary convention (`[T]`/`[I]`/`[C]`/`[U]`/`inf.`/
           `past`/`p.p.`), used by the vocabulary-capture sense card.
         - `pedagogical`: textbook convention familiar to Russian ESL learners
           (`vt.`/`vi.`/`count.`/`uncount.`/`V1`/`V2`/`V3`/`V-ing`), used by the
           practice card.
        """.trimIndent(),
    )
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.grammar.domain)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.metro.runtime)
                implementation(projects.shared.core.mockBackend)
                implementation(projects.shared.core.network)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
    android {
        namespace = "app.sensee.grammar.data"
    }
}
