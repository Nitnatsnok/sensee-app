package app.sensee.grammar.domain

/**
 * Pure neutral resolver: maps grammar/usage/complement ids to learner-facing
 * display labels. A snapshot — built once from a fetched taxonomy (the builder
 * lives in `shared/grammar/data`, which owns the wire DTO) and then queried by
 * UI. An unknown id returns `null` so the caller can fall back or hide it,
 * never crash. Keys mirror the neutral ids (canon `docs/pos-and-forms.adoc`).
 */
public class GrammarLabels(
    private val unitTypeLabels: Map<String, String>,
    private val categoryLabels: Map<String, String>,
    private val formLabels: Map<String, String>,
    private val usageValueLabels: Map<String, String>,
    private val complementLabels: Map<String, String>,
) {
    public fun unitType(type: GrammarUnitType): String? = unitTypeLabels[type.name.normalizedGrammarId()]

    public fun category(category: GrammarCategory): String? = categoryLabels[category.id]

    public fun form(tag: GrammarTag): String? = formLabels[key(tag.category.id, tag.form.id)]

    public fun usage(label: UsageLabel): String? = usageValueLabels[key(label.axis.id, label.value.id)]

    public fun complement(type: ComplementType): String? = complementLabels[type.id]

    public companion object {
        /** Empty resolver — every lookup is `null` (taxonomy not loaded yet). */
        public val EMPTY: GrammarLabels =
            GrammarLabels(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())

        public fun key(
            parent: String,
            child: String,
        ): String = "$parent/$child"
    }
}
