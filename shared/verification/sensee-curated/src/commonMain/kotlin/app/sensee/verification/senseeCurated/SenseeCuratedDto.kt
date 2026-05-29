package app.sensee.verification.senseeCurated

import kotlinx.serialization.Serializable

// Wire shapes for the Sensee-curated reference catalogs. These mirror the
// hand-authored fixture JSON one-to-one so a catalog file stays the editable
// source of truth — the (mock) backend serves it verbatim.

/** `verification/frequency`: lemma → zipf score. */
@Serializable
internal data class FrequencyCatalogDto(
    val lemmas: Map<String, Double> = emptyMap(),
)

/** `verification/cefr`: lemma → CEFR level (`A1`..`C2`). */
@Serializable
internal data class CefrCatalogDto(
    val lemmas: Map<String, String> = emptyMap(),
)

/** `verification/senses`: lemma → ordered glosses. */
@Serializable
internal data class SenseCatalogDto(
    val defaultPos: String? = null,
    val lemmas: Map<String, List<SenseEntryDto>> = emptyMap(),
)

@Serializable
internal data class SenseEntryDto(
    val id: String,
    val label: String,
    val cefr: String? = null,
    /** Optional per-sense part of speech; falls back to [SenseCatalogDto.defaultPos]. */
    val pos: String? = null,
)

/** `verification/family`: head lemma → its unit family. */
@Serializable
internal data class FamilyCatalogDto(
    val families: Map<String, FamilyEntryDto> = emptyMap(),
)

@Serializable
internal data class FamilyEntryDto(
    val canonicalLemma: String,
    val units: List<FamilyUnitDto> = emptyList(),
)

@Serializable
internal data class FamilyUnitDto(
    /** `word` | `phrasal` | `idiom`. */
    val type: String,
    val display: String,
    /** Phrasal particle/preposition text + role. */
    val secondaryText: String? = null,
    val secondaryRole: String? = null,
    /** Idiom fixed object (e.g. "the ice" in "break the ice"). */
    val fixedObject: String? = null,
)
