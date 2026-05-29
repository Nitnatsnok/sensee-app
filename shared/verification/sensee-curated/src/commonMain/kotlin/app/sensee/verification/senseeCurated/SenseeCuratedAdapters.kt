package app.sensee.verification.senseeCurated

import app.sensee.verification.core.CefrLevel
import app.sensee.verification.core.CefrLevelProvider
import app.sensee.verification.core.CefrResult
import app.sensee.verification.core.Confidence
import app.sensee.verification.core.DictionarySenseSummary
import app.sensee.verification.core.FamilyResult
import app.sensee.verification.core.FrequencyBand
import app.sensee.verification.core.FrequencyProvider
import app.sensee.verification.core.FrequencyResult
import app.sensee.verification.core.FrequencyScore
import app.sensee.verification.core.LemmaId
import app.sensee.verification.core.LexicalEntryTypeHint
import app.sensee.verification.core.LexicalFamilyProvider
import app.sensee.verification.core.LexicalLemma
import app.sensee.verification.core.LexicalSourceRef
import app.sensee.verification.core.LexicalUnitId
import app.sensee.verification.core.LexicalUnitInfo
import app.sensee.verification.core.LexicalVerificationQuery
import app.sensee.verification.core.PartOfSpeechHint
import app.sensee.verification.core.SenseInventoryProvider
import app.sensee.verification.core.SenseInventoryResult
import app.sensee.verification.core.SenseMapping
import app.sensee.verification.core.UnitComponentFact
import app.sensee.verification.core.UnitResolutionResult
import app.sensee.verification.core.VerifierAvailability
import dev.zacsweers.metro.Inject

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

internal const val SENSEE_LANGUAGE: String = "en"
internal val SENSEE_SUPPORTED_LANGUAGES: Set<String> = setOf("en", "en-us", "en-gb")
internal const val CATALOG_UNAVAILABLE: String = "Sensee curated backend unavailable"

internal fun senseeRef(
    term: String,
    senseId: String? = null,
): LexicalSourceRef =
    LexicalSourceRef(
        sourceId = SenseeCuratedSource.ID,
        entryId = term,
        senseId = senseId,
        fetchedAtEpochMillis = 0L,
    )

internal fun normaliseTerm(query: LexicalVerificationQuery): String? {
    if (query.studyLanguageTag.lowercase() !in SENSEE_SUPPORTED_LANGUAGES) return null
    return query.text
        .trim()
        .lowercase()
        .ifEmpty { null }
}

internal fun parseCefrLevel(raw: String?): CefrLevel? {
    val cleaned = raw?.trim()?.uppercase() ?: return null
    return CefrLevel.entries.firstOrNull { it.name == cleaned }
}

/** Zipf-band thresholds used by the curated frequency adapter. */
private object ZipfBands {
    const val TOP_1K_MIN: Double = 5.80
    const val TOP_3K_MIN: Double = 5.30
    const val TOP_5K_MIN: Double = 4.90
    const val TOP_10K_MIN: Double = 4.50
    const val TOP_20K_MIN: Double = 4.10
}

private fun bandFor(zipf: Double): FrequencyBand =
    when {
        zipf >= ZipfBands.TOP_1K_MIN -> FrequencyBand.Top1k
        zipf >= ZipfBands.TOP_3K_MIN -> FrequencyBand.Top3k
        zipf >= ZipfBands.TOP_5K_MIN -> FrequencyBand.Top5k
        zipf >= ZipfBands.TOP_10K_MIN -> FrequencyBand.Top10k
        zipf >= ZipfBands.TOP_20K_MIN -> FrequencyBand.Top20k
        else -> FrequencyBand.Beyond20k
    }

private fun entryTypeFor(type: String): LexicalEntryTypeHint =
    when (type) {
        "phrasal" -> LexicalEntryTypeHint("phrasal_verb")
        else -> LexicalEntryTypeHint(type)
    }

// ---------------------------------------------------------------------------
// Frequency
// ---------------------------------------------------------------------------

/** Sensee-curated [FrequencyProvider] over `verification/frequency`. */
@Inject
public class SenseeFrequencyProvider(
    private val catalogs: SenseeCuratedCatalogs,
) : FrequencyProvider {
    override suspend fun frequency(query: LexicalVerificationQuery): FrequencyResult {
        val term = normaliseTerm(query) ?: return unavailable("unsupported term")
        if (' ' in term) return unavailable("frequency dataset is per-lemma; multi-word units are out of scope")
        val catalog = catalogs.frequency() ?: return unavailable(CATALOG_UNAVAILABLE)
        val zipf = catalog.lemmas[term] ?: return degraded(term)
        return FrequencyResult(
            availability = VerifierAvailability.Available,
            score = FrequencyScore(zipf = zipf, band = bandFor(zipf)),
            confidence = Confidence.Medium,
            sources = listOf(senseeRef(term)),
        )
    }

    private fun unavailable(reason: String): FrequencyResult =
        FrequencyResult(VerifierAvailability.Unavailable(reason), null, Confidence.Low, emptyList())

    private fun degraded(term: String): FrequencyResult =
        FrequencyResult(
            availability = VerifierAvailability.Degraded("no curated frequency for '$term'"),
            score = null,
            confidence = Confidence.Low,
            sources = listOf(senseeRef(term)),
        )
}

// ---------------------------------------------------------------------------
// CEFR
// ---------------------------------------------------------------------------

/** Sensee-curated [CefrLevelProvider] over `verification/cefr`. */
@Inject
public class SenseeCefrLevelProvider(
    private val catalogs: SenseeCuratedCatalogs,
) : CefrLevelProvider {
    override suspend fun cefr(query: LexicalVerificationQuery): CefrResult {
        val term = normaliseTerm(query) ?: return unavailable("unsupported term")
        if (' ' in term) return unavailable("CEFR dataset is per-lemma; multi-word units are out of scope")
        val catalog = catalogs.cefr() ?: return unavailable(CATALOG_UNAVAILABLE)
        val raw = catalog.lemmas[term] ?: return degraded(term, "no curated CEFR for '$term'")
        val level = parseCefrLevel(raw) ?: return degraded(term, "unrecognised CEFR level '$raw' for '$term'")
        return CefrResult(
            availability = VerifierAvailability.Available,
            level = level,
            confidence = Confidence.Medium,
            sources = listOf(senseeRef(term)),
        )
    }

    private fun unavailable(reason: String): CefrResult =
        CefrResult(VerifierAvailability.Unavailable(reason), null, Confidence.Low, emptyList())

    private fun degraded(
        term: String,
        reason: String,
    ): CefrResult =
        CefrResult(
            availability = VerifierAvailability.Degraded(reason),
            level = null,
            confidence = Confidence.Low,
            sources = listOf(senseeRef(term)),
        )
}

// ---------------------------------------------------------------------------
// Sense inventory
// ---------------------------------------------------------------------------

/** Sensee-curated [SenseInventoryProvider] over `verification/senses`. */
@Inject
public class SenseeSenseInventoryProvider(
    private val catalogs: SenseeCuratedCatalogs,
) : SenseInventoryProvider {
    override suspend fun senses(query: LexicalVerificationQuery): SenseInventoryResult {
        val term = normaliseTerm(query) ?: return unavailable("unsupported term")
        val catalog = catalogs.senses() ?: return unavailable(CATALOG_UNAVAILABLE)
        val entries = catalog.lemmas[term].orEmpty()
        if (entries.isEmpty()) return degraded(term)
        val extras = entries.map { it.toSummary(term, catalog.defaultPos) }
        return SenseInventoryResult(
            availability = VerifierAvailability.Available,
            mapping =
                SenseMapping(
                    matched = emptyList(),
                    unmatchedAiSenses = emptyList(),
                    extraDictionarySenses = extras,
                    confidence = Confidence.Medium,
                ),
            sources = listOf(senseeRef(term)),
        )
    }

    private fun SenseEntryDto.toSummary(
        term: String,
        defaultPos: String?,
    ): DictionarySenseSummary =
        DictionarySenseSummary(
            ref = senseeRef(term, senseId = id),
            pos = (pos ?: defaultPos)?.takeIf { it.isNotBlank() }?.let(::PartOfSpeechHint),
            shortLabel = label,
            cefr = parseCefrLevel(cefr),
        )

    private fun unavailable(reason: String): SenseInventoryResult =
        SenseInventoryResult(VerifierAvailability.Unavailable(reason), SenseMapping.EMPTY, emptyList())

    private fun degraded(term: String): SenseInventoryResult =
        SenseInventoryResult(
            availability = VerifierAvailability.Degraded("no curated senses for '$term'"),
            mapping = SenseMapping.EMPTY,
            sources = listOf(senseeRef(term)),
        )
}

// ---------------------------------------------------------------------------
// Family
// ---------------------------------------------------------------------------

/** Sensee-curated [LexicalFamilyProvider] over `verification/family`. */
@Inject
public class SenseeLexicalFamilyProvider(
    private val catalogs: SenseeCuratedCatalogs,
) : LexicalFamilyProvider {
    override suspend fun resolveUnit(query: LexicalVerificationQuery): UnitResolutionResult {
        val term = normaliseTerm(query) ?: return unavailableUnit("unsupported term")
        val catalog = catalogs.family() ?: return unavailableUnit(CATALOG_UNAVAILABLE)
        val match = catalog.findByDisplay(term) ?: return degradedUnit("not in curated families", term)
        val unit = match.unit.toUnitInfo(match.canonicalLemma)
        return UnitResolutionResult(
            availability = VerifierAvailability.Available,
            unit = unit,
            confidence = Confidence.Medium,
            sources = listOf(senseeRef(unit.displayForm)),
        )
    }

    override suspend fun family(
        lemma: LemmaId,
        cap: Int,
    ): FamilyResult {
        val canonical =
            lemma.value
                .substringAfterLast(':')
                .trim()
                .lowercase()
                .ifEmpty { null }
                ?: return emptyFamily(lemma)
        val catalog = catalogs.family() ?: return unavailableFamily(CATALOG_UNAVAILABLE)
        val entry = catalog.families[canonical] ?: return emptyFamily(lemma)
        if (entry.units.isEmpty()) return emptyFamily(lemma)
        val capped = entry.units.take(cap.coerceAtLeast(0))
        return FamilyResult(
            availability = VerifierAvailability.Available,
            lemma = lemmaOf(canonical),
            units = capped.map { it.toUnitInfo(entry.canonicalLemma) },
            truncated = entry.units.size > capped.size,
            sources = listOf(senseeRef(canonical)),
        )
    }

    private fun FamilyCatalogDto.findByDisplay(display: String): UnitMatch? {
        families.values.forEach { entry ->
            entry.units.firstOrNull { it.display.trim().lowercase() == display }?.let { unit ->
                return UnitMatch(entry.canonicalLemma, unit)
            }
        }
        return null
    }

    private fun FamilyUnitDto.toUnitInfo(canonicalLemma: String): LexicalUnitInfo {
        val entryType = entryTypeFor(type)
        return LexicalUnitInfo(
            id = LexicalUnitId.of(SENSEE_LANGUAGE, display, entryType),
            displayForm = display,
            entryType = entryType,
            headLemma = LemmaId.of(SENSEE_LANGUAGE, canonicalLemma),
            components = componentsFor(canonicalLemma),
            sources = listOf(senseeRef(display)),
        )
    }

    private fun FamilyUnitDto.componentsFor(canonicalLemma: String): List<UnitComponentFact> =
        buildList {
            add(UnitComponentFact(text = canonicalLemma, role = "head"))
            when {
                secondaryText != null && secondaryRole != null ->
                    add(UnitComponentFact(text = secondaryText, role = secondaryRole))
                fixedObject != null ->
                    add(UnitComponentFact(text = fixedObject, role = "fixed_object"))
            }
        }

    private fun lemmaOf(canonical: String): LexicalLemma =
        LexicalLemma(
            id = LemmaId.of(SENSEE_LANGUAGE, canonical),
            canonical = canonical,
            language = SENSEE_LANGUAGE,
            pos = null,
            sources = listOf(senseeRef(canonical)),
        )

    private fun unavailableUnit(reason: String): UnitResolutionResult =
        UnitResolutionResult(VerifierAvailability.Unavailable(reason), null, Confidence.Low, emptyList())

    private fun unavailableFamily(reason: String): FamilyResult =
        FamilyResult(VerifierAvailability.Unavailable(reason), null, emptyList(), truncated = false, emptyList())

    private fun degradedUnit(
        reason: String,
        term: String,
    ): UnitResolutionResult =
        UnitResolutionResult(
            availability = VerifierAvailability.Degraded(reason),
            unit = null,
            confidence = Confidence.Low,
            sources = listOf(senseeRef(term)),
        )

    private fun emptyFamily(lemma: LemmaId): FamilyResult =
        FamilyResult(
            availability = VerifierAvailability.Degraded("no curated family for '${lemma.value}'"),
            lemma = null,
            units = emptyList(),
            truncated = false,
            sources = listOf(senseeRef(lemma.value)),
        )

    private data class UnitMatch(
        val canonicalLemma: String,
        val unit: FamilyUnitDto,
    )
}
