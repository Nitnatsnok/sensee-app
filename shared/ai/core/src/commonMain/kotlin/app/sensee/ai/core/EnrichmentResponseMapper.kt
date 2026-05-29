package app.sensee.ai.core

import kotlinx.serialization.json.JsonElement

/**
 * Anti-corruption mapping from the versioned wire DTO to neutral suggestions
 * (ADR-005). Never throws: a malformed or partial provider response degrades
 * into a usable result instead of corrupting feature domain or crashing the
 * flow. An item with no usable translation is not a candidate and is dropped.
 *
 * [itemExtensions] aligns by index with `response.items`. A shorter list →
 * no extensions for the tail; a longer one → trailing entries ignored.
 */
public object EnrichmentResponseMapper {
    public fun map(
        response: EnrichmentResponseV1,
        itemExtensions: List<Map<String, JsonElement>> = emptyList(),
    ): EnrichmentResult {
        if (response.version != EnrichmentResponseV1.SCHEMA_VERSION) {
            return EnrichmentResult(
                EnrichmentAvailability.Degraded(
                    "unsupported enrichment schema version ${response.version}",
                ),
            )
        }
        val pairs = response.items.zipPositional(itemExtensions)
        val suggestions = pairs.mapNotNull { (item, extensions) -> toSuggestion(item, extensions) }
        return result(response, suggestions)
    }

    private fun List<EnrichmentItemV1>.zipPositional(
        extensions: List<Map<String, JsonElement>>,
    ): List<Pair<EnrichmentItemV1, Map<String, JsonElement>>> =
        mapIndexed { index, item ->
            item to extensions.getOrElse(index) { emptyMap() }
        }

    private fun toSuggestion(
        item: EnrichmentItemV1,
        extensions: Map<String, JsonElement>,
    ): EnrichmentSuggestion? {
        val translation = item.translation?.trim().nullIfBlank() ?: return null
        return EnrichmentSuggestion(
            translation = translation,
            surfaceForm = item.surfaceForm?.trim().nullIfBlank(),
            unitType = item.unitType?.trim().nullIfBlank(),
            baseLemma = item.baseLemma?.trim().nullIfBlank(),
            headLemma = item.headLemma?.trim().nullIfBlank(),
            components = item.components.mapNotNull(::toComponentHint),
            explanation = item.explanation?.trim().nullIfBlank(),
            examples = item.examples.mapNotNull { it.toExample() },
            synonyms = item.synonyms.mapNotNull { it.trim().nullIfBlank() },
            antonyms = item.antonyms.mapNotNull { it.trim().nullIfBlank() },
            collocations = item.collocations.mapNotNull { it.trim().nullIfBlank() },
            wordFamily = item.wordFamily.mapNotNull(::toWordFamilyHint),
            governedPrepositions =
                item.prepositionGovernment.mapNotNull { group ->
                    val alternatives = group.alternatives.mapNotNull { it.trim().nullIfBlank() }
                    if (alternatives.isEmpty()) {
                        return@mapNotNull null
                    }
                    PrepositionGovernmentHint(alternatives, group.example?.trim().nullIfBlank())
                },
            complementation = item.complementation.mapNotNull { it.trim().nullIfBlank() },
            usageLabels =
                item.usageLabels.mapNotNull { label ->
                    val axis = label.axis.trim().nullIfBlank() ?: return@mapNotNull null
                    val value = label.value.trim().nullIfBlank() ?: return@mapNotNull null
                    UsageLabelHint(axis, value)
                },
            usageNote = item.usageNote?.trim().nullIfBlank(),
            grammarTags =
                item.grammarTags.mapNotNull { tag ->
                    val category = tag.category.trim().nullIfBlank() ?: return@mapNotNull null
                    val form = tag.form.trim().nullIfBlank() ?: return@mapNotNull null
                    GrammarTagHint(category, form)
                },
            irregularForms = item.irregularForms?.let(::toIrregularFormsHint),
            extensions = extensions,
        )
    }

    private fun toComponentHint(component: UnitComponentDtoV1): UnitComponentHint? {
        val text = component.text.trim().nullIfBlank() ?: return null
        val role = component.role.trim().nullIfBlank() ?: return null
        return UnitComponentHint(text, role, component.salience?.trim().nullIfBlank())
    }

    private fun toWordFamilyHint(entry: WordFamilyEntryDtoV1): WordFamilyHint? {
        val lemma = entry.lemma.trim().nullIfBlank() ?: return null
        val unitType = entry.unitType.trim().nullIfBlank() ?: return null
        return WordFamilyHint(lemma, unitType)
    }

    private fun toIrregularFormsHint(forms: IrregularFormsDtoV1): IrregularFormsHint? {
        val base = forms.base.trim().nullIfBlank()
        val past = forms.past.trim().nullIfBlank()
        val pastParticiple = forms.pastParticiple.trim().nullIfBlank()
        if (base == null || past == null || pastParticiple == null) {
            return null
        }
        return IrregularFormsHint(base, past, pastParticiple)
    }

    private fun result(
        response: EnrichmentResponseV1,
        suggestions: List<EnrichmentSuggestion>,
    ): EnrichmentResult {
        val dropped = response.items.size - suggestions.size
        val availability =
            if (dropped > 0) {
                EnrichmentAvailability.Degraded(
                    "partial enrichment: dropped $dropped of ${response.items.size} suggestions",
                )
            } else {
                EnrichmentAvailability.Available
            }
        return EnrichmentResult(availability, suggestions)
    }

    private fun String?.nullIfBlank(): String? = this?.takeIf { it.isNotBlank() }

    private fun EnrichmentExampleV1.toExample(): EnrichmentExample? {
        val trimmedSentence = sentence.trim().nullIfBlank() ?: return null
        val trimmedTranslation = translation?.trim().nullIfBlank()
        val mappedAlignment =
            alignment.mapNotNull { chunk ->
                val source = chunk.source.trim().nullIfBlank() ?: return@mapNotNull null
                val target = chunk.target.trim().nullIfBlank() ?: return@mapNotNull null
                AlignmentChunk(source, target)
            }
        return EnrichmentExample(
            sentence = trimmedSentence,
            translation = trimmedTranslation,
            alignment = mappedAlignment,
        )
    }
}
