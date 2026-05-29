package app.sensee.verification.integration

import app.sensee.core.observability.analytics.NoOpAnalyticsTracker
import app.sensee.core.observability.crash.NoOpCrashReporter
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.diagnostics.DefaultAppDiagnostics
import app.sensee.core.observability.logging.DefaultAppLoggerFactory
import app.sensee.verification.core.AttributionPolicy
import app.sensee.verification.core.Confidence
import app.sensee.verification.core.ExampleCheckRequest
import app.sensee.verification.core.ExampleCheckResult
import app.sensee.verification.core.ExampleHint
import app.sensee.verification.core.ExampleQualityChecker
import app.sensee.verification.core.FamilyResult
import app.sensee.verification.core.FindingSeverity
import app.sensee.verification.core.FindingTarget
import app.sensee.verification.core.LemmaId
import app.sensee.verification.core.LexicalEntryLookup
import app.sensee.verification.core.LexicalEntryLookupResult
import app.sensee.verification.core.LexicalEntryTypeHint
import app.sensee.verification.core.LexicalExistence
import app.sensee.verification.core.LexicalFamilyProvider
import app.sensee.verification.core.LexicalSource
import app.sensee.verification.core.LexicalSourceRef
import app.sensee.verification.core.LexicalUnitId
import app.sensee.verification.core.LexicalUnitInfo
import app.sensee.verification.core.LexicalVerificationQuery
import app.sensee.verification.core.LicensePolicy
import app.sensee.verification.core.NormalizationCandidate
import app.sensee.verification.core.NormalizationKind
import app.sensee.verification.core.NormalizationOutcome
import app.sensee.verification.core.PartOfSpeechHint
import app.sensee.verification.core.SenseInventoryProvider
import app.sensee.verification.core.SenseInventoryResult
import app.sensee.verification.core.SenseMapping
import app.sensee.verification.core.SentenceHint
import app.sensee.verification.core.SuggestedAction
import app.sensee.verification.core.UnitResolutionResult
import app.sensee.verification.core.VerifierAvailability
import app.sensee.verification.languagetool.LanguageToolSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoutingLexicalVerifierTest {
    private fun diagnostics(): AppDiagnostics =
        DefaultAppDiagnostics(
            logger = DefaultAppLoggerFactory().tagged("Test"),
            crashReporter = NoOpCrashReporter,
            analyticsTracker = NoOpAnalyticsTracker,
        )

    @Test
    fun `aggregator merges two adapters' existence and POS evidence`() =
        runTest {
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = setOf(secondaryLookup, stubLookup),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            exampleQualityCheckers = emptySet(),
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = setOf(stubSource),
                    appDiagnostics = diagnostics(),
                )

            val report = verifier.verify(query("come across"))

            // Both adapters observed existence; both ride into the EvidenceSet
            // so the orchestrator can see they agree.
            val existenceSources = report.existence.observations.map { it.source.sourceId }
            assertTrue("secondary" in existenceSources)
            assertTrue(stubSource.id in existenceSources)
            assertEquals(LexicalExistence.Confirmed, report.existence.consensus)

            // POS evidence rides in from both lookups; entry-type consensus comes
            // from the one adapter that classified the unit.
            assertTrue(report.partsOfSpeech.observations.isNotEmpty())
            assertEquals(LexicalEntryTypeHint("phrasal_verb"), report.entryType.consensus)

            // The aggregator pulls the network adapter's source from the
            // catalog so downstream license-aware filters can find it.
            assertTrue(report.sources.any { it.id == stubSource.id })
        }

    @Test
    fun `an unknown term still surfaces the network adapter's NotFound as evidence`() =
        runTest {
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = setOf(notFoundLookup),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            exampleQualityCheckers = emptySet(),
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = setOf(stubSource),
                    appDiagnostics = diagnostics(),
                )

            val report = verifier.verify(query("plumbus"))

            // The network adapter says NotFound — the merged report carries
            // NotFound as evidence and overall availability is Available because
            // at least one source ran.
            assertEquals(LexicalExistence.NotFound, report.existence.consensus)
            assertEquals(VerifierAvailability.Available, report.availability)
        }

    @Test
    fun `an Available family-only provider rolls overall availability up to Available`() =
        runTest {
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = emptySet(),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            exampleQualityCheckers = emptySet(),
                            familyProviders = setOf(stubFamilyProvider),
                        ),
                    sourceCatalog = setOf(stubSource),
                    appDiagnostics = diagnostics(),
                )

            val baseQuery = query("plumbus")
            val familyQuery = baseQuery.copy(policy = baseQuery.policy.copy(includeFamily = true))
            val report = verifier.verify(familyQuery)

            // No entry-lookup contributor; the family adapter was the only
            // Available signal. Overall availability must promote to Available
            // — Degraded would mislead the UI.
            assertEquals(VerifierAvailability.Available, report.availability)
        }

    @Test
    fun `a failing adapter never leaks its exception across the seam`() =
        runTest {
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = setOf(stubLookup, throwingLookup),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            exampleQualityCheckers = emptySet(),
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = setOf(stubSource),
                    appDiagnostics = diagnostics(),
                )

            val report = verifier.verify(query("come"))

            // The healthy adapter's evidence still rides; the broken adapter is
            // silently demoted out of the EvidenceSet.
            assertEquals(LexicalExistence.Confirmed, report.existence.consensus)
        }

    @Test
    fun `empty contributors return Unavailable instead of pretending verification ran`() =
        runTest {
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = emptySet(),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            exampleQualityCheckers = emptySet(),
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = emptySet(),
                    appDiagnostics = diagnostics(),
                )

            val report = verifier.verify(query("plumbus"))

            assertTrue(report.availability is VerifierAvailability.Unavailable)
        }

    @Test
    fun `a degraded example checker degrades the report`() =
        runTest {
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = emptySet(),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            exampleQualityCheckers = setOf(degradedExampleChecker),
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = setOf(LanguageToolSource.descriptor),
                    appDiagnostics = diagnostics(),
                )

            val report = verifier.verify(queryWithExample("come"))

            assertTrue(report.availability is VerifierAvailability.Degraded)
        }

    @Test
    fun `an available example checker contributes availability and source metadata without findings`() =
        runTest {
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = emptySet(),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            exampleQualityCheckers = setOf(availableExampleChecker),
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = setOf(LanguageToolSource.descriptor),
                    appDiagnostics = diagnostics(),
                )

            val report = verifier.verify(queryWithExample("come"))

            assertEquals(VerifierAvailability.Available, report.availability)
            assertTrue(report.sources.any { it.id == LanguageToolSource.ID })
        }

    @Test
    fun `a contributing source missing from the catalog rides in as non persistable and is not dropped`() =
        runTest {
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = setOf(uncatalogedLookup),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            exampleQualityCheckers = emptySet(),
                            familyProviders = emptySet(),
                        ),
                    // Catalog deliberately omits the "ghost" source the adapter emits.
                    sourceCatalog = emptySet(),
                    appDiagnostics = diagnostics(),
                )

            val report = verifier.verify(query("come"))

            // The unregistered source is kept (one entry), carrying the
            // strictest license so the L2 persistence gate fails closed.
            assertEquals(
                listOf(false),
                report.sources.filter { it.id == "ghost" }.map { it.license.storeContentAllowed },
            )
        }

    @Test
    fun `merged sense mapping confidence reflects the most confident provider instead of the seed default`() =
        runTest {
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = emptySet(),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = setOf(mediumConfidenceSenseProvider),
                            exampleQualityCheckers = emptySet(),
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = setOf(stubSource),
                    appDiagnostics = diagnostics(),
                )

            val report = verifier.verify(query("come"))

            assertEquals(Confidence.Medium, report.senseMapping.confidence)
            assertFalse(report.senseMapping.confidence == Confidence.Low)
        }

    @Test
    fun `family resolution picks the highest-confidence provider's unit regardless of provider order`() =
        runTest {
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = emptySet(),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            exampleQualityCheckers = emptySet(),
                            // Lower-confidence provider is listed first; without a
                            // confidence-ranked merge it would win by Set order.
                            familyProviders = setOf(lowConfidenceFamily, highConfidenceFamily),
                        ),
                    sourceCatalog = setOf(stubSource),
                    appDiagnostics = diagnostics(),
                )

            val baseQuery = query("plumbus")
            val familyQuery = baseQuery.copy(policy = baseQuery.policy.copy(includeFamily = true))
            val report = verifier.verify(familyQuery)

            assertEquals("high", report.family?.resolvedUnit?.displayForm)
        }

    @Test
    fun `normalization candidates surface as headword findings`() =
        runTest {
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = setOf(spellingNormalizationLookup),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            exampleQualityCheckers = emptySet(),
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = setOf(stubSource),
                    appDiagnostics = diagnostics(),
                )

            val report = verifier.verify(query("acress"))

            assertEquals("across", report.normalized.canonical)
            val finding = report.findings.single()
            assertEquals("SPELLING_NORMALIZED", finding.code)
            assertEquals(FindingSeverity.Warning, finding.severity)
            assertEquals(FindingTarget.Headword, finding.target)
            assertEquals(listOf(SuggestedAction.ReplaceHeadword("across")), finding.suggestedActions)
        }

    private fun query(text: String): LexicalVerificationQuery =
        LexicalVerificationQuery(text = text, studyLanguageTag = "en")

    private fun queryWithExample(text: String): LexicalVerificationQuery =
        query(text).copy(
            examplesToValidate =
                listOf(
                    ExampleHint(
                        sentence = SentenceHint(listOf(SentenceHint.Segment.Text("They come across well."))),
                    ),
                ),
        )

    private val stubSource: LexicalSource =
        LexicalSource.Adapter(
            id = "stub",
            displayName = "Test stub",
            attribution = AttributionPolicy(required = false),
            license = LicensePolicy(usableAsLlmContext = true),
        )

    private val stubLookup: LexicalEntryLookup =
        object : LexicalEntryLookup {
            override suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult {
                val ref = LexicalSourceRef(sourceId = "stub", entryId = query.text, fetchedAtEpochMillis = 0L)
                return LexicalEntryLookupResult(
                    availability = VerifierAvailability.Available,
                    existence = LexicalExistence.Confirmed,
                    normalized = NormalizationOutcome.EMPTY,
                    entryType = LexicalEntryTypeHint("phrasal_verb"),
                    partsOfSpeech = listOf(PartOfSpeechHint("verb")),
                    confidence = Confidence.High,
                    sources = listOf(ref),
                )
            }
        }

    private val secondaryLookup: LexicalEntryLookup =
        object : LexicalEntryLookup {
            override suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult {
                val ref = LexicalSourceRef(sourceId = "secondary", entryId = query.text, fetchedAtEpochMillis = 0L)
                return LexicalEntryLookupResult(
                    availability = VerifierAvailability.Available,
                    existence = LexicalExistence.Confirmed,
                    normalized = NormalizationOutcome.EMPTY,
                    entryType = null,
                    partsOfSpeech = listOf(PartOfSpeechHint("verb")),
                    confidence = Confidence.Medium,
                    sources = listOf(ref),
                )
            }
        }

    private val notFoundLookup: LexicalEntryLookup =
        object : LexicalEntryLookup {
            override suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult {
                val ref = LexicalSourceRef(sourceId = "stub", entryId = query.text, fetchedAtEpochMillis = 0L)
                return LexicalEntryLookupResult(
                    availability = VerifierAvailability.Available,
                    existence = LexicalExistence.NotFound,
                    normalized = NormalizationOutcome.EMPTY,
                    entryType = null,
                    partsOfSpeech = emptyList(),
                    confidence = Confidence.Medium,
                    sources = listOf(ref),
                )
            }
        }

    private val throwingLookup: LexicalEntryLookup =
        object : LexicalEntryLookup {
            override suspend fun lookup(query: LexicalVerificationQuery): Nothing = error("adapter blew up")
        }

    private val spellingNormalizationLookup: LexicalEntryLookup =
        object : LexicalEntryLookup {
            override suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult {
                val ref = LexicalSourceRef(sourceId = "stub", entryId = query.text, fetchedAtEpochMillis = 0L)
                return LexicalEntryLookupResult(
                    availability = VerifierAvailability.Available,
                    existence = LexicalExistence.NotFound,
                    normalized =
                        NormalizationOutcome(
                            canonical = "across",
                            candidates =
                                listOf(
                                    NormalizationCandidate(
                                        text = "across",
                                        kind = NormalizationKind.SpellFix,
                                        source = ref,
                                        confidence = Confidence.High,
                                    ),
                                ),
                        ),
                    entryType = null,
                    partsOfSpeech = emptyList(),
                    confidence = Confidence.High,
                    sources = listOf(ref),
                )
            }
        }

    private val degradedExampleChecker: ExampleQualityChecker =
        object : ExampleQualityChecker {
            override suspend fun check(request: ExampleCheckRequest): ExampleCheckResult =
                ExampleCheckResult(
                    availability = VerifierAvailability.Degraded("network failure"),
                    issues = emptyList(),
                    rewrite = null,
                    sources = emptyList(),
                )
        }

    private val availableExampleChecker: ExampleQualityChecker =
        object : ExampleQualityChecker {
            override suspend fun check(request: ExampleCheckRequest): ExampleCheckResult =
                ExampleCheckResult(
                    availability = VerifierAvailability.Available,
                    issues = emptyList(),
                    rewrite = null,
                    sources =
                        listOf(
                            LexicalSourceRef(sourceId = LanguageToolSource.ID, fetchedAtEpochMillis = 0L),
                        ),
                )
        }

    private val stubFamilyProvider: LexicalFamilyProvider =
        object : LexicalFamilyProvider {
            override suspend fun resolveUnit(query: LexicalVerificationQuery): UnitResolutionResult {
                val ref = LexicalSourceRef(sourceId = "stub", entryId = query.text, fetchedAtEpochMillis = 0L)
                val unit =
                    LexicalUnitInfo(
                        id = LexicalUnitId.of("en", query.text, LexicalEntryTypeHint("word")),
                        displayForm = query.text,
                        entryType = LexicalEntryTypeHint("word"),
                        headLemma = LemmaId.of("en", query.text),
                        sources = listOf(ref),
                    )
                return UnitResolutionResult(
                    availability = VerifierAvailability.Available,
                    unit = unit,
                    confidence = Confidence.Medium,
                    sources = listOf(ref),
                )
            }

            override suspend fun family(
                lemma: LemmaId,
                cap: Int,
            ): FamilyResult =
                FamilyResult(
                    availability = VerifierAvailability.Available,
                    lemma = null,
                    units = emptyList(),
                    truncated = false,
                    sources = listOf(LexicalSourceRef(sourceId = "stub", fetchedAtEpochMillis = 0L)),
                )
        }

    private val uncatalogedLookup: LexicalEntryLookup =
        object : LexicalEntryLookup {
            override suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult {
                val ref = LexicalSourceRef(sourceId = "ghost", entryId = query.text, fetchedAtEpochMillis = 0L)
                return LexicalEntryLookupResult(
                    availability = VerifierAvailability.Available,
                    existence = LexicalExistence.Confirmed,
                    normalized = NormalizationOutcome.EMPTY,
                    entryType = null,
                    partsOfSpeech = emptyList(),
                    confidence = Confidence.Medium,
                    sources = listOf(ref),
                )
            }
        }

    private val mediumConfidenceSenseProvider: SenseInventoryProvider =
        object : SenseInventoryProvider {
            override suspend fun senses(query: LexicalVerificationQuery): SenseInventoryResult =
                SenseInventoryResult(
                    availability = VerifierAvailability.Available,
                    mapping = SenseMapping(confidence = Confidence.Medium),
                    sources = listOf(LexicalSourceRef(sourceId = "stub", fetchedAtEpochMillis = 0L)),
                )
        }

    private fun familyProvider(
        displayForm: String,
        confidence: Confidence,
    ): LexicalFamilyProvider =
        object : LexicalFamilyProvider {
            override suspend fun resolveUnit(query: LexicalVerificationQuery): UnitResolutionResult {
                val ref = LexicalSourceRef(sourceId = "stub", entryId = query.text, fetchedAtEpochMillis = 0L)
                val unit =
                    LexicalUnitInfo(
                        id = LexicalUnitId.of("en", displayForm, LexicalEntryTypeHint("word")),
                        displayForm = displayForm,
                        entryType = LexicalEntryTypeHint("word"),
                        headLemma = LemmaId.of("en", query.text),
                        sources = listOf(ref),
                    )
                return UnitResolutionResult(
                    availability = VerifierAvailability.Available,
                    unit = unit,
                    confidence = confidence,
                    sources = listOf(ref),
                )
            }

            override suspend fun family(
                lemma: LemmaId,
                cap: Int,
            ): FamilyResult =
                FamilyResult(
                    availability = VerifierAvailability.Available,
                    lemma = null,
                    units = emptyList(),
                    truncated = false,
                    sources = listOf(LexicalSourceRef(sourceId = "stub", fetchedAtEpochMillis = 0L)),
                )
        }

    private val lowConfidenceFamily: LexicalFamilyProvider = familyProvider("low", Confidence.Low)
    private val highConfidenceFamily: LexicalFamilyProvider = familyProvider("high", Confidence.High)
}
