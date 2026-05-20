package app.sensee.ai.core

/**
 * Anti-corruption mapping from the versioned wire DTO to neutral suggestions
 * (ADR-005). Never throws: a malformed or partial provider response degrades
 * into a usable result instead of corrupting feature domain or crashing the
 * flow. An item with no usable translation is not a candidate and is dropped.
 */
public object EnrichmentResponseMapper {
    public fun map(response: EnrichmentResponseV1): EnrichmentResult {
        if (response.version != EnrichmentResponseV1.SCHEMA_VERSION) {
            return EnrichmentResult(
                EnrichmentAvailability.Degraded(
                    "unsupported enrichment schema version ${response.version}",
                ),
            )
        }
        val suggestions = response.items.mapNotNull(::toSuggestion)
        return result(response, suggestions)
    }

    private fun toSuggestion(item: EnrichmentItemV1): EnrichmentSuggestion? {
        val translation = item.translation?.trim().orEmptyIfBlank() ?: return null
        return EnrichmentSuggestion(
            translation = translation,
            surfaceForm = item.surfaceForm?.trim().orEmptyIfBlank(),
            unitType = item.unitType?.trim().orEmptyIfBlank(),
            baseLemma = item.baseLemma?.trim().orEmptyIfBlank(),
            explanation = item.explanation?.trim().orEmptyIfBlank(),
            examples = item.examples.mapNotNull { it.trim().orEmptyIfBlank() },
            governedPrepositions =
                item.prepositionGovernment.mapNotNull { group ->
                    val alternatives = group.alternatives.mapNotNull { it.trim().orEmptyIfBlank() }
                    if (alternatives.isEmpty()) {
                        return@mapNotNull null
                    }
                    PrepositionGovernmentHint(alternatives, group.example?.trim().orEmptyIfBlank())
                },
            complementation = item.complementation.mapNotNull { it.trim().orEmptyIfBlank() },
            usageLabels =
                item.usageLabels.mapNotNull { label ->
                    val axis = label.axis.trim().orEmptyIfBlank() ?: return@mapNotNull null
                    val value = label.value.trim().orEmptyIfBlank() ?: return@mapNotNull null
                    UsageLabelHint(axis, value)
                },
            usageNote = item.usageNote?.trim().orEmptyIfBlank(),
            grammarTags =
                item.grammarTags.mapNotNull { tag ->
                    val category = tag.category.trim().orEmptyIfBlank() ?: return@mapNotNull null
                    val form = tag.form.trim().orEmptyIfBlank() ?: return@mapNotNull null
                    GrammarTagHint(category, form)
                },
            irregularForms = item.irregularForms?.let(::toIrregularFormsHint),
        )
    }

    private fun toIrregularFormsHint(forms: IrregularFormsDtoV1): IrregularFormsHint? {
        val base = forms.base.trim().orEmptyIfBlank()
        val past = forms.past.trim().orEmptyIfBlank()
        val pastParticiple = forms.pastParticiple.trim().orEmptyIfBlank()
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

    private fun String?.orEmptyIfBlank(): String? = this?.takeIf { it.isNotBlank() }
}
