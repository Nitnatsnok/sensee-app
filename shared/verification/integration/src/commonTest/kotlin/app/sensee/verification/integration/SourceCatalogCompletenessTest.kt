package app.sensee.verification.integration

import app.sensee.verification.datamuse.DatamuseSource
import app.sensee.verification.freeDictionary.FreeDictionarySource
import app.sensee.verification.languagetool.LanguageToolSource
import app.sensee.verification.senseeCurated.SenseeCuratedSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Guard against the "forgot to register descriptor" footgun in
 * [VerificationIntegrationProviders.provideKnownLexicalSources]:
 * a new adapter wired into a `Set<…>` multibinding but missing from the
 * catalog silently drops its evidence from `report.sources` at merge time.
 *
 * Maintenance contract: when you add a new adapter module under
 * `shared/verification/<module>` and register it in
 * `VerificationIntegrationProviders`, also add its `*Source.ID` to the
 * [EXPECTED_SOURCE_IDS] set below. The test then asserts the catalog matches
 * exactly — extras and missing items both fail.
 */
class SourceCatalogCompletenessTest {
    @Test
    fun `provideKnownLexicalSources matches the expected adapter source ids`() {
        val catalogIds = providers().provideKnownLexicalSources().map { it.id }.toSet()
        val missing = EXPECTED_SOURCE_IDS - catalogIds
        val extra = catalogIds - EXPECTED_SOURCE_IDS
        if (missing.isNotEmpty() || extra.isNotEmpty()) {
            fail(
                buildString {
                    append("Source catalog mismatch.")
                    if (missing.isNotEmpty()) append(" Missing from catalog: $missing.")
                    if (extra.isNotEmpty()) append(" Unexpected entries in catalog: $extra.")
                },
            )
        }
        assertEquals(EXPECTED_SOURCE_IDS, catalogIds)
    }

    @Test
    fun `every catalog descriptor declares a non-blank display name`() {
        val withoutDisplayName =
            providers()
                .provideKnownLexicalSources()
                .filter { it.displayName.isBlank() }
                .map { it.id }
        if (withoutDisplayName.isNotEmpty()) {
            fail("These descriptors lack a displayName: $withoutDisplayName")
        }
    }

    private fun providers(): VerificationIntegrationProviders = object : VerificationIntegrationProviders {}

    private companion object {
        // Add to this set whenever a new `<adapter>Source` descriptor is wired
        // into [VerificationIntegrationProviders]. Test failure is intentional —
        // it catches the silent-evidence-drop bug before it ships.
        val EXPECTED_SOURCE_IDS: Set<String> =
            setOf(
                FreeDictionarySource.ID,
                DatamuseSource.ID,
                LanguageToolSource.ID,
                SenseeCuratedSource.ID,
            )
    }
}
