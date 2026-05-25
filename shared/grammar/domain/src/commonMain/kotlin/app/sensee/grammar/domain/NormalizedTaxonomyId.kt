package app.sensee.grammar.domain

/**
 * Canonical form of a taxonomy `id` for lookup. Strips non-alphanumerics and
 * lowercases — so `Phrasal_Verb`, `phrasal-verb`, and `phrasalverb` all hash
 * to the same bucket. Storage and wire payloads keep the original spelling;
 * normalization happens only on read.
 */
public fun String.normalizedTaxonomyId(): String = filter { it.isLetterOrDigit() }.lowercase()
