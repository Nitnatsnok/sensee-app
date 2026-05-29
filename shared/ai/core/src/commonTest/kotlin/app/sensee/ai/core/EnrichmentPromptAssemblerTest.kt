package app.sensee.ai.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnrichmentPromptAssemblerTest {
    private val emptyContext =
        EnrichmentRequestContext(
            request = EnrichmentRequest(term = "run"),
            preferences = UserEnrichmentPreferences.EMPTY,
        )

    @Test
    fun `default modifiers carry unique phase-id pairs so the assembler order is stable`() {
        val keys = DefaultEnrichmentRequestModifiers.map { it.phase to it.id }

        assertEquals(keys.size, keys.toSet().size, "duplicate (phase, id) in default modifiers: $keys")
    }

    @Test
    fun `assembler emits fragments in phase order then id order within a phase`() {
        val a =
            stubModifier(id = "a", phase = PromptPhase.Coverage) {
                PromptContribution(systemFragments = listOf("coverage-a"))
            }
        val b =
            stubModifier(id = "b", phase = PromptPhase.Coverage) {
                PromptContribution(systemFragments = listOf("coverage-b"))
            }
        val ident =
            stubModifier(id = "z", phase = PromptPhase.Identity) {
                PromptContribution(systemFragments = listOf("identity-z"))
            }

        val assembled = EnrichmentPromptAssembler.assemble(setOf(b, ident, a), emptyContext)

        assertEquals(listOf("identity-z", "coverage-a", "coverage-b"), assembled.systemFragments)
    }

    @Test
    fun `assembler preserves user fragments alongside system fragments`() {
        val mixed =
            stubModifier(id = "mixed", phase = PromptPhase.Shaping) {
                PromptContribution(
                    systemFragments = listOf("sys"),
                    userFragments = listOf("usr"),
                )
            }

        val assembled = EnrichmentPromptAssembler.assemble(setOf(mixed), emptyContext)

        assertEquals(listOf("sys"), assembled.systemFragments)
        assertEquals(listOf("usr"), assembled.userFragments)
    }

    @Test
    fun `identity modifier contributes the lexicographer role`() {
        val contribution = IdentityModifier.contribute(emptyContext)

        assertEquals(listOf("You are a lexicographer."), contribution.systemFragments)
    }

    @Test
    fun `format modifier carries the json skeleton and a field guide`() {
        val contribution = FormatModifier.contribute(emptyContext)

        val text = contribution.systemFragments.single()
        assertTrue(text.contains(EnrichmentSchema.jsonSkeleton))
        assertTrue(text.contains("- surface_form:"))
        assertTrue(text.endsWith("Omit unknown fields."))
    }

    @Test
    fun `language modifier names study and native language tags from the request`() {
        val context =
            emptyContext.copy(
                request = EnrichmentRequest(term = "run", studyLanguageTag = "en", nativeLanguageTag = "ru"),
            )

        val text = LanguageModifier.contribute(context).systemFragments.single()

        assertTrue(text.contains("Auto-detect the input language"))
        assertTrue(text.contains("'examples' are in en"))
    }

    @Test
    fun `sense coverage modifier returns the rule matching the configured coverage`() {
        val minimal = coverageFragment(SenseCoverage.Minimal)
        val common = coverageFragment(SenseCoverage.Common)
        val comprehensive = coverageFragment(SenseCoverage.Comprehensive)

        assertTrue(minimal.contains("quick add"))
        assertTrue(common.contains("usually return 2-5"))
        assertTrue(comprehensive.contains("as many useful distinct dictionary senses"))
    }

    private fun coverageFragment(coverage: SenseCoverage): String {
        val context =
            emptyContext.copy(request = EnrichmentRequest(term = "run", senseCoverage = coverage))
        return SenseCoverageModifier
            .contribute(context)
            .systemFragments
            .single()
    }

    @Test
    fun `polysemy hints modifier fires only on known polysemous terms`() {
        val known = EnrichmentRequest(term = "Come Across")
        val unknown = EnrichmentRequest(term = "run")

        val knownContribution = PolysemyHintsModifier.contribute(emptyContext.copy(request = known))
        val unknownContribution = PolysemyHintsModifier.contribute(emptyContext.copy(request = unknown))

        assertEquals(1, knownContribution.systemFragments.size)
        assertTrue(knownContribution.systemFragments.single().contains("come across as <adjective/noun>"))
        assertEquals(emptyList(), unknownContribution.systemFragments)
    }

    @Test
    fun `preferred topics modifier stays silent on an empty topic list`() {
        val withTopics =
            emptyContext.copy(
                request = EnrichmentRequest(term = "run", topicPreferences = listOf("travel", "food")),
            )

        val none = PreferredTopicsModifier.contribute(emptyContext)
        val some = PreferredTopicsModifier.contribute(withTopics)

        assertEquals(emptyList(), none.systemFragments)
        assertTrue(some.systemFragments.single().contains("travel, food"))
    }

    @Test
    fun `phrasal verb coverage modifier stays dormant while preferences are empty`() {
        val off = PhrasalVerbCoverageModifier.contribute(emptyContext)
        val on =
            PhrasalVerbCoverageModifier.contribute(
                emptyContext.copy(preferences = UserEnrichmentPreferences(includePhrasalVerbs = true)),
            )

        assertEquals(emptyList(), off.systemFragments)
        assertTrue(on.systemFragments.single().contains("phrasal-verb senses"))
    }

    @Test
    fun `evidence modifier omits empty bullets for all-null pronunciation and sense entries`() {
        val evidence =
            EnrichmentEvidence(
                lemma = "run",
                pronunciations = listOf(PronunciationFact(accent = null, ipa = null)),
                knownSenseSummaries = listOf(SenseSummary(pos = null, shortLabel = null, cefr = null)),
            )
        val context = emptyContext.copy(request = EnrichmentRequest(term = "run", evidence = evidence))

        val text = EvidenceModifier.contribute(context).systemFragments.single()

        // The lemma still renders, but the all-null pronunciation/sense entries
        // must not leak empty bullet lines into the prompt.
        assertTrue(text.contains("- lemma: run"))
        assertFalse(text.contains("- pronunciation:"))
        assertFalse(text.contains("· "))
    }

    private fun stubModifier(
        id: String,
        phase: PromptPhase,
        contribute: (EnrichmentRequestContext) -> PromptContribution,
    ): EnrichmentRequestModifier =
        object : EnrichmentRequestModifier {
            override val id: String = id
            override val phase: PromptPhase = phase

            override fun contribute(context: EnrichmentRequestContext): PromptContribution = contribute(context)
        }
}
