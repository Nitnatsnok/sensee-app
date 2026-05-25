package app.sensee.ai.core

/** The default modifier set wired by the integration module and reused by tests. */
public val DefaultEnrichmentRequestModifiers: Set<EnrichmentRequestModifier> =
    setOf(
        IdentityModifier,
        FormatModifier,
        SenseSplittingModifier,
        LanguageModifier,
        SenseCoverageModifier,
        PolysemyHintsModifier,
        PreferredTopicsModifier,
        PhrasalVerbCoverageModifier,
    )

/** "You are a lexicographer." — provider role assertion. */
public object IdentityModifier : EnrichmentRequestModifier {
    override val id: String = "identity"
    override val phase: PromptPhase = PromptPhase.Identity

    override fun contribute(context: EnrichmentRequestContext): PromptContribution =
        PromptContribution(systemFragments = listOf("You are a lexicographer."))
}

/**
 * Owns the wire-shape directives: response skeleton, per-field guidance, and
 * the "omit unknown" rule. The taxonomy-bound enum/oneOf constraints are
 * carried separately via the structured-output schema (see
 * [EnrichmentSchema.buildJsonSchema]).
 */
public object FormatModifier : EnrichmentRequestModifier {
    override val id: String = "format"
    override val phase: PromptPhase = PromptPhase.Format

    override fun contribute(context: EnrichmentRequestContext): PromptContribution {
        val fieldGuide =
            EnrichmentSchema.fields.joinToString(separator = "\n") {
                "- ${it.serialName}: ${it.guidance}"
            }
        val fragment =
            "Return ONLY JSON matching this schema: ${EnrichmentSchema.jsonSkeleton}.\n" +
                "Fields:\n$fieldGuide\n" +
                "Omit unknown fields."
        return PromptContribution(systemFragments = listOf(fragment))
    }
}

/**
 * Generic sense splitting/merging rules. Term-specific polysemy nudges live in
 * [PolysemyHintsModifier] so this prose stays the same across every call.
 */
public object SenseSplittingModifier : EnrichmentRequestModifier {
    override val id: String = "sense-splitting"
    override val phase: PromptPhase = PromptPhase.CrossCutting

    override fun contribute(context: EnrichmentRequestContext): PromptContribution =
        PromptContribution(
            systemFragments =
                listOf(
                    "Each item is exactly one distinct sense — never merge senses into " +
                        "one blob. Before producing the final JSON, identify the common " +
                        "learner-relevant sense inventory of the input and return ALL common " +
                        "distinct senses, not only the most frequent one — do not stop after " +
                        "the first valid sense. For phrasal/prepositional verbs, idioms, " +
                        "phrases and fixed expressions check whether the unit has multiple " +
                        "common meanings, constructions or argument patterns. Return one item " +
                        "only when there is genuinely only one common learner-relevant sense; " +
                        "do not invent rare, obsolete or artificial senses to inflate the " +
                        "count. A meaning-changing nuance is a separate sense, not a label. " +
                        "YOU detect the lexical unit type (word, inflected form, phrasal / " +
                        "prepositional / phrasal-prepositional verb, phrase, idiom, " +
                        "collocation, fixed expression) — the user never declares it. " +
                        "A particle or preposition that changes the meaning makes a SEPARATE " +
                        "item (look at / look after / look for are different senses), never " +
                        "an alternative; a preposition that keeps the meaning is " +
                        "preposition_government; a fixed part of the unit (look down on) " +
                        "belongs in surface_form. Conversely, do NOT over-split: one sense " +
                        "whose complement may be a person or a thing stays ONE item — write " +
                        "the slot generically as <someone/something>, never split by object " +
                        "type. Every item has at least one example, one per significant " +
                        "construction. For an irregular verb include irregular_forms.",
                ),
        )
}

/** Study/native language directives, autodetect, [[ ]] example marking. */
public object LanguageModifier : EnrichmentRequestModifier {
    override val id: String = "language"
    override val phase: PromptPhase = PromptPhase.CrossCutting

    override fun contribute(context: EnrichmentRequestContext): PromptContribution {
        val request = context.request
        val fragment =
            "Auto-detect the input language: it may be ${request.studyLanguageTag} " +
                "or ${request.nativeLanguageTag}. Always return ${request.studyLanguageTag} " +
                "senses; for ${request.nativeLanguageTag} input find the matching " +
                "${request.studyLanguageTag} variants. 'translation', 'explanation' " +
                "and 'usage_note' are in ${request.nativeLanguageTag} (the learner " +
                "picks senses by them); 'examples' are in ${request.studyLanguageTag} " +
                "with the studied unit in [[ ]]."
        return PromptContribution(systemFragments = listOf(fragment))
    }
}

/** Per-[SenseCoverage] guidance shaping how many senses the provider returns. */
public object SenseCoverageModifier : EnrichmentRequestModifier {
    override val id: String = "sense-coverage"
    override val phase: PromptPhase = PromptPhase.Coverage

    override fun contribute(context: EnrichmentRequestContext): PromptContribution {
        val rule =
            when (context.request.senseCoverage) {
                SenseCoverage.Minimal ->
                    "Coverage: return only the most important sense(s) for a quick add."
                SenseCoverage.Common ->
                    "Coverage: usually return 2-5 items for polysemous words, phrasal " +
                        "verbs, idioms, phrases and fixed expressions; return all common " +
                        "learner-relevant senses, avoiding rare or obsolete ones unless " +
                        "important for learners."
                SenseCoverage.Comprehensive ->
                    "Coverage: return as many useful distinct dictionary senses as " +
                        "practical, still avoiding rare or obsolete senses unless " +
                        "important for learners."
            }
        return PromptContribution(systemFragments = listOf(rule))
    }
}

/**
 * Per-term nudges for units models reliably collapse to one sense. Narrow
 * allowlist, not a dictionary; the LLM client's corrective retry reuses the
 * hint via [polysemyHintFor].
 */
public object PolysemyHintsModifier : EnrichmentRequestModifier {
    override val id: String = "polysemy-hints"
    override val phase: PromptPhase = PromptPhase.Coverage

    private val knownPolysemyHints: Map<String, String> =
        mapOf(
            "come across" to
                "The input \"come across\" is polysemous. Return separate items for: " +
                "find/meet by chance; seem/give an impression, often " +
                "\"come across as <adjective/noun>\"; be communicated/understood, often " +
                "\"<message/meaning/idea> comes across\".",
        )

    /** Hint for [term], or `null` if not curated. Trim + lowercase normalize the key. */
    public fun polysemyHintFor(term: String): String? =
        knownPolysemyHints[
            term
                .trim()
                .lowercase(),
        ]

    override fun contribute(context: EnrichmentRequestContext): PromptContribution {
        val hint = polysemyHintFor(context.request.term) ?: return PromptContribution.EMPTY
        return PromptContribution(systemFragments = listOf(hint))
    }
}

/** Soft topic-preference steering for examples only; empty list = no-op. */
public object PreferredTopicsModifier : EnrichmentRequestModifier {
    override val id: String = "preferred-topics"
    override val phase: PromptPhase = PromptPhase.Shaping

    override fun contribute(context: EnrichmentRequestContext): PromptContribution {
        val topics = context.request.topicPreferences.filter { it.isNotBlank() }
        if (topics.isEmpty()) return PromptContribution.EMPTY
        val fragment =
            "The learner is interested in these topics: ${topics.joinToString(", ")}. " +
                "When a sense naturally allows it, prefer 'examples' set in those topics; " +
                "never force an unnatural or misleading context, and never let topic " +
                "steering distort the sense, translation, grammar or any non-example field."
        return PromptContribution(systemFragments = listOf(fragment))
    }
}

/** Opt-in inclusion of phrasal-verb senses for non-phrasal-verb inputs. */
public object PhrasalVerbCoverageModifier : EnrichmentRequestModifier {
    override val id: String = "phrasal-verb-coverage"
    override val phase: PromptPhase = PromptPhase.Preferences

    override fun contribute(context: EnrichmentRequestContext): PromptContribution {
        if (!context.preferences.includePhrasalVerbs) return PromptContribution.EMPTY
        return PromptContribution(
            systemFragments =
                listOf(
                    "Also include phrasal-verb senses derived from the input when they " +
                        "are common and learner-relevant; never invent rare or artificial ones.",
                ),
        )
    }
}
