package app.sensee.grammar.domain

/**
 * Display form variant of a [GrammarLabel]: [Long] is the descriptive form used
 * in detail panels and lists; [Short] is the abbreviation used in compact UI
 * surfaces (badges, chips). A missing short value falls back to the long form
 * — never crashes, never shows the raw id.
 */
public enum class GrammarLabelForm { Long, Short }

/**
 * Stylistic convention for the [GrammarLabelForm.Short] form. The capture sense
 * card uses the dictionary-style [Lexicographic] (`[T]`/`[I]`/`[C]`/`[U]`/
 * `inf.`/`past`/`p.p.`); the practice card uses the textbook-style
 * [Pedagogical] (`vt.`/`vi.`/`count.`/`uncount.`/`V1`/`V2`/`V3`). Both
 * conventions live alongside each other in the taxonomy; the consumer asks for
 * its preferred style per lookup, and may eventually be driven by a user
 * setting. A missing style for a given id resolves to `null` (caller falls
 * back to the long form or the raw id).
 */
public enum class GrammarLabelStyle { Lexicographic, Pedagogical }

/**
 * A single localized label for one grammar id: the descriptive [long] form and
 * a map of stylistic short variants. Both belong to the same language; the
 * outer map in [GrammarLabels] keys them by BCP-47 language tag.
 */
public data class GrammarLabel(
    val long: String,
    val short: Map<GrammarLabelStyle, String> = emptyMap(),
) {
    public fun value(
        form: GrammarLabelForm,
        style: GrammarLabelStyle = GrammarLabelStyle.Lexicographic,
    ): String =
        when (form) {
            GrammarLabelForm.Long -> long
            GrammarLabelForm.Short -> short[style]?.ifBlank { null } ?: long
        }
}

/**
 * Pure neutral resolver: maps grammar/usage/complement ids to learner-facing
 * display labels in one of the supported languages, in long or short form, in
 * the requested stylistic convention. A snapshot — built once from a fetched
 * taxonomy (the builder lives in `shared/grammar/data`, which owns the wire
 * DTO) and then queried by UI. An unknown id or missing language returns
 * `null` so the caller can fall back or hide it, never crash. Keys mirror the
 * neutral ids (canon `docs/domain/pos-and-forms.adoc`).
 *
 * The taxonomy carries every supported UI language at once and both short
 * styles — the same resolver serves the capture sense-card badges
 * ([GrammarLabelStyle.Lexicographic], study language) and the practice card
 * badges ([GrammarLabelStyle.Pedagogical], study language) as well as the
 * long-form detail panel (any language). Callers pass language + form +
 * style per lookup.
 *
 * [formByName] is the help-sheet lookup: a [GrammarForm] outside any
 * [GrammarTag] context (the practice help glossary lists forms in a flat
 * table). Resolves through the first category whose label was registered for
 * that form — the per-form abbreviation is consistent across categories.
 */
public class GrammarLabels(
    private val unitTypeLabels: Map<String, Map<String, GrammarLabel>>,
    private val categoryLabels: Map<String, Map<String, GrammarLabel>>,
    private val formLabels: Map<String, Map<String, GrammarLabel>>,
    private val formLabelsByName: Map<String, Map<String, GrammarLabel>>,
    private val usageValueLabels: Map<String, Map<String, GrammarLabel>>,
    private val complementLabels: Map<String, Map<String, GrammarLabel>>,
) {
    public fun unitType(
        type: GrammarUnitType,
        language: String,
        form: GrammarLabelForm = GrammarLabelForm.Long,
        style: GrammarLabelStyle = GrammarLabelStyle.Lexicographic,
    ): String? = unitTypeLabels[type.id.normalizedTaxonomyId()]?.lookup(language, form, style)

    public fun category(
        category: GrammarCategory,
        language: String,
        form: GrammarLabelForm = GrammarLabelForm.Long,
        style: GrammarLabelStyle = GrammarLabelStyle.Lexicographic,
    ): String? = categoryLabels[category.id]?.lookup(language, form, style)

    public fun form(
        tag: GrammarTag,
        language: String,
        form: GrammarLabelForm = GrammarLabelForm.Long,
        style: GrammarLabelStyle = GrammarLabelStyle.Lexicographic,
    ): String? = formLabels[key(tag.category.id, tag.form.id)]?.lookup(language, form, style)

    /**
     * Form lookup outside any [GrammarTag] context — used by the practice help
     * glossary to label a flat list of forms.
     */
    public fun formByName(
        form: GrammarForm,
        language: String,
        labelForm: GrammarLabelForm = GrammarLabelForm.Long,
        style: GrammarLabelStyle = GrammarLabelStyle.Lexicographic,
    ): String? = formLabelsByName[form.id]?.lookup(language, labelForm, style)

    public fun usage(
        label: UsageLabel,
        language: String,
        form: GrammarLabelForm = GrammarLabelForm.Long,
        style: GrammarLabelStyle = GrammarLabelStyle.Lexicographic,
    ): String? = usageValueLabels[key(label.axis.id, label.value.id)]?.lookup(language, form, style)

    public fun complement(
        type: ComplementType,
        language: String,
        form: GrammarLabelForm = GrammarLabelForm.Long,
        style: GrammarLabelStyle = GrammarLabelStyle.Lexicographic,
    ): String? = complementLabels[type.id]?.lookup(language, form, style)

    private fun Map<String, GrammarLabel>.lookup(
        language: String,
        form: GrammarLabelForm,
        style: GrammarLabelStyle,
    ): String? = this[language]?.value(form, style)

    public companion object {
        /** Empty resolver — every lookup is `null` (taxonomy not loaded yet). */
        public val EMPTY: GrammarLabels =
            GrammarLabels(
                unitTypeLabels = emptyMap(),
                categoryLabels = emptyMap(),
                formLabels = emptyMap(),
                formLabelsByName = emptyMap(),
                usageValueLabels = emptyMap(),
                complementLabels = emptyMap(),
            )

        public fun key(
            parent: String,
            child: String,
        ): String = "$parent/$child"
    }
}
