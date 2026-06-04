package app.sensee.lexicon.domain

/**
 * Grouping key for the lemma family a sense belongs to (head-lemma, lowercased):
 * `come across` and the verb `come` both resolve to `come`, so the catalog
 * groups every `come*` unit under one lemma page (ADR-001).
 *
 * Resolves head → base → surface form, falling back to the translation only for
 * a degenerate draft with no surface form yet. Unlike [deriveSenseContentKey]
 * this is a *grouping* key, not an identity key: distinct senses share it.
 */
public fun deriveLemmaKey(sense: Sense): String {
    val candidate =
        sequenceOf(sense.headLemma, sense.baseLemma, sense.surfaceForm?.display())
            .filterNotNull()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: sense.translation.trim()
    return candidate.lowercase()
}
