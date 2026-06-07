package app.sensee.lexicon.domain

/**
 * Content-derived dedup key for a confirmed [Sense]. Two captures that yield
 * an equal key are the same sense: repository merge keeps the first and drops
 * the rest so a re-capture does not duplicate the user's existing senses or
 * orphan their SRS state.
 *
 * Identity covers what disambiguates the sense for the learner: the surface
 * form, the part-of-speech tag and the native-language translation. Verifier
 * metadata such as [Sense.headLemma] and [Sense.baseLemma], plus wording
 * variations in [Sense.explanation], do not change identity; enriching a
 * previously manual/degraded sense must not create a duplicate.
 */
public fun deriveSenseContentKey(sense: Sense): String {
    // Lowercased like the translation below: a re-capture whose surface form
    // differs only in case is the same sense and must not duplicate.
    val surfaceForm =
        sense.surfaceForm
            ?.display()
            ?.lowercase()
            .orEmpty()
    return listOf(
        surfaceForm,
        sense.unitType?.id.orEmpty(),
        normalizeTranslation(sense.translation),
    ).joinToString(separator = "|", transform = ::stableIdPart)
}

// The translation is free text, so fold trivial variants of one sense to a single
// key: lowercase, collapse internal whitespace runs, and drop surrounding
// whitespace/punctuation — without merging genuinely different translations. The
// surface form is already normalized by display(); CEFR/lemma are not identity.
private fun normalizeTranslation(value: String): String =
    value
        .lowercase()
        .replace(WHITESPACE_RUN, " ")
        .trim { it.isWhitespace() || it in TRANSLATION_EDGE_PUNCTUATION }

private val WHITESPACE_RUN = Regex("\\s+")

private const val TRANSLATION_EDGE_PUNCTUATION = ".,;:!?\"'«»…"

private fun stableIdPart(value: String): String = "${value.length}:$value"
