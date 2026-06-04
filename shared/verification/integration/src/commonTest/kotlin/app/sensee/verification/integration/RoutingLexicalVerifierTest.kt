package app.sensee.verification.integration

import app.sensee.core.observability.analytics.NoOpAnalyticsTracker
import app.sensee.core.observability.crash.NoOpCrashReporter
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.observability.diagnostics.DefaultAppDiagnostics
import app.sensee.core.observability.logging.DefaultAppLoggerFactory
import app.sensee.verification.core.contract.AttributionPolicy
import app.sensee.verification.core.contract.Confidence
import app.sensee.verification.core.contract.FamilyResult
import app.sensee.verification.core.contract.FindingSeverity
import app.sensee.verification.core.contract.FindingTarget
import app.sensee.verification.core.contract.LexicalEntryLookup
import app.sensee.verification.core.contract.LexicalEntryLookupResult
import app.sensee.verification.core.contract.LexicalEntryTypeHint
import app.sensee.verification.core.contract.LexicalExistence
import app.sensee.verification.core.contract.LexicalFamilyProvider
import app.sensee.verification.core.contract.LexicalSource
import app.sensee.verification.core.contract.LexicalSourceRef
import app.sensee.verification.core.contract.LexicalVerificationQuery
import app.sensee.verification.core.contract.LicensePolicy
import app.sensee.verification.core.contract.NormalizationCandidate
import app.sensee.verification.core.contract.NormalizationKind
import app.sensee.verification.core.contract.NormalizationOutcome
import app.sensee.verification.core.contract.PartOfSpeechHint
import app.sensee.verification.core.contract.SenseInventoryProvider
import app.sensee.verification.core.contract.SenseInventoryResult
import app.sensee.verification.core.contract.SuggestedAction
import app.sensee.verification.core.contract.UnitResolutionResult
import app.sensee.verification.core.contract.VerifierAvailability
import app.sensee.verification.core.grounding.SenseMapping
import app.sensee.verification.core.hierarchy.LemmaId
import app.sensee.verification.core.hierarchy.LexicalUnitId
import app.sensee.verification.core.hierarchy.LexicalUnitInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = emptySet(),
                    appDiagnostics = diagnostics(),
                )

            val report = verifier.verify(query("plumbus"))

            assertTrue(report.availability is VerifierAvailability.Unavailable)
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
    fun `verify propagates CancellationException instead of swallowing it into a degraded result`() =
        runTest {
            // The seam's "never throw across" rule has a single exception:
            // structured cancellation MUST unwind cleanly. runCatchingCancellable
            // wraps every adapter call to enforce this, and the test pins the
            // contract end-to-end. Without it, a host scope that cancels mid-
            // verify (user navigates away, parent component disposes) would
            // observe a fake degraded report instead of cancellation, leaking
            // the cancelled work into UI state.
            val hangingLookup =
                object : LexicalEntryLookup {
                    override suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult {
                        awaitCancellation()
                    }
                }
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = setOf(hangingLookup),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = setOf(stubSource),
                    appDiagnostics = diagnostics(),
                )

            assertFailsWith<CancellationException> {
                coroutineScope {
                    val job = async { verifier.verify(query("come")) }
                    job.cancel()
                    job.await()
                }
            }
        }

    @Test
    fun `a hung provider degrades via timeoutPerProviderMillis instead of stalling the report`() =
        runTest {
            val hangingLookup =
                object : LexicalEntryLookup {
                    override suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult =
                        awaitCancellation()
                }
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = setOf(hangingLookup),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = setOf(stubSource),
                    appDiagnostics = diagnostics(),
                )
            val baseQuery = query("come")
            val tinyTimeout =
                baseQuery.copy(policy = baseQuery.policy.copy(timeoutPerProviderMillis = 25L))

            val report = verifier.verify(tinyTimeout)

            // The hung leg degrades; with no other contributor the merged
            // availability is Degraded("no source returned Available").
            assertTrue(report.availability is VerifierAvailability.Degraded)
        }

    @Test
    fun `an Unavailable lookup that nonetheless carries normalization candidates is gated out`() =
        runTest {
            // I4: the report-init invariant forbids carrying evidence on an
            // all-Unavailable report. A misbehaving lookup that returns
            // Unavailable BUT populates normalized.candidates would otherwise
            // reach the merge helper and trip the require(!carriesEvidence)
            // check across the seam. The merge helper must gate Unavailable
            // contributors instead.
            val verifier =
                RoutingLexicalVerifier(
                    contributors =
                        VerificationContributors(
                            entryLookups = setOf(unavailableButNoisyLookup),
                            frequencyProviders = emptySet(),
                            cefrProviders = emptySet(),
                            senseInventoryProviders = emptySet(),
                            familyProviders = emptySet(),
                        ),
                    sourceCatalog = setOf(stubSource),
                    appDiagnostics = diagnostics(),
                )

            val report = verifier.verify(query("plumbus"))

            assertTrue(report.availability is VerifierAvailability.Unavailable)
            assertTrue(report.findings.isEmpty())
            assertTrue(report.normalized.candidates.isEmpty())
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

    // Misbehaving lookup: claims Unavailable yet still ships a normalization
    // candidate. Used by the I4 invariant test (the merge helper must drop it).
    private val unavailableButNoisyLookup: LexicalEntryLookup =
        object : LexicalEntryLookup {
            override suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult {
                val ref = LexicalSourceRef(sourceId = "stub", entryId = query.text, fetchedAtEpochMillis = 0L)
                return LexicalEntryLookupResult(
                    availability = VerifierAvailability.Unavailable("offline"),
                    existence = LexicalExistence.Unknown,
                    normalized =
                        NormalizationOutcome(
                            canonical = "should-not-surface",
                            candidates =
                                listOf(
                                    NormalizationCandidate(
                                        text = "should-not-surface",
                                        kind = NormalizationKind.SpellFix,
                                        source = ref,
                                        confidence = Confidence.High,
                                    ),
                                ),
                        ),
                    entryType = null,
                    partsOfSpeech = emptyList(),
                    confidence = Confidence.Low,
                    sources = listOf(ref),
                )
            }
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
