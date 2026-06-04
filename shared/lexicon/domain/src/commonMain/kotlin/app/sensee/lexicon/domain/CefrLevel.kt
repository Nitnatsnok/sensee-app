package app.sensee.lexicon.domain

/**
 * CEFR proficiency level of a [Sense] (A1..C2). A lexical attribute of the
 * sense, surfaced by enrichment reference data; it is deliberately NOT part of
 * sense identity (see `deriveSenseContentKey`), so two captures that differ
 * only in CEFR are still the same sense.
 */
public enum class CefrLevel {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2,
    ;

    public companion object {
        /**
         * Lenient parse for wire/extension values: case-insensitive match by
         * name, trimmed; an unknown or blank id resolves to `null` rather than
         * failing, so an unexpected provider value is dropped, not fatal.
         */
        public fun fromId(id: String?): CefrLevel? {
            val normalized = id?.trim()?.uppercase().orEmpty()
            return entries.firstOrNull { it.name == normalized }
        }
    }
}
