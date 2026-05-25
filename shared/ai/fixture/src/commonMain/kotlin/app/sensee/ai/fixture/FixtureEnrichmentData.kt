package app.sensee.ai.fixture

import app.sensee.ai.core.EnrichmentSuggestion
import app.sensee.ai.core.GrammarTagHint
import app.sensee.ai.core.IrregularFormsHint
import app.sensee.ai.core.PrepositionGovernmentHint
import app.sensee.ai.core.UsageLabelHint

/**
 * Curated deterministic demo answers for the offline fixture. A few well-known
 * terms expand into a realistic multi-sense set with the full field shape
 * (structured surface form, grammar tags, prepositions, usage) so the capture
 * flow — multi-select, the per-sense detail, the rich card — is exercisable
 * with no API key. An unknown term falls back to a single generic suggestion.
 *
 * Field language follows the seam contract: `translation`, `explanation`,
 * `usageNote` are native (Russian); `examples` are study-language with the
 * studied unit marked by `[[ ]]`.
 */
internal object FixtureEnrichmentData {
    fun suggestionsFor(term: String): List<EnrichmentSuggestion> = curated[term.lowercase()] ?: listOf(generic(term))

    private fun generic(term: String): EnrichmentSuggestion =
        EnrichmentSuggestion(
            translation = "[$term]",
            surfaceForm = term,
            unitType = "phrase",
            baseLemma = term,
            explanation = "Пример значения для «$term» (офлайн-фикстура).",
            examples = listOf("This is a sample sentence using [[$term]]."),
        )

    private val curated: Map<String, List<EnrichmentSuggestion>> =
        mapOf(
            "come across" to
                listOf(
                    EnrichmentSuggestion(
                        translation = "наткнуться",
                        surfaceForm = "come across <something>",
                        unitType = "phrasal_verb",
                        baseLemma = "come",
                        explanation = "случайно обнаружить или встретить что-либо",
                        examples =
                            listOf(
                                "I [[came across]] an old photo in a drawer.",
                                "We [[came across]] a tiny village on the way.",
                            ),
                        complementation = listOf("noun"),
                        grammarTags =
                            listOf(
                                GrammarTagHint("separability", "inseparable"),
                                GrammarTagHint("transitivity", "transitive"),
                            ),
                        irregularForms = IrregularFormsHint("come", "came", "come"),
                    ),
                    EnrichmentSuggestion(
                        translation = "производить впечатление",
                        surfaceForm = "come across [as]",
                        unitType = "phrasal_verb",
                        baseLemma = "come",
                        explanation = "казаться обладающим неким качеством",
                        examples =
                            listOf(
                                "She [[comes across]] [[as]] very confident.",
                                "He [[came across]] really well in the interview.",
                            ),
                        governedPrepositions = listOf(PrepositionGovernmentHint(listOf("as"))),
                        complementation = listOf("adjective", "noun"),
                        usageNote = "обычно о впечатлении, которое человек производит на других",
                        grammarTags =
                            listOf(
                                GrammarTagHint("separability", "inseparable"),
                                GrammarTagHint("transitivity", "intransitive"),
                            ),
                        irregularForms = IrregularFormsHint("come", "came", "come"),
                    ),
                ),
            "run" to
                listOf(
                    EnrichmentSuggestion(
                        translation = "бежать",
                        surfaceForm = "run",
                        unitType = "irregular_verb",
                        baseLemma = "run",
                        explanation = "двигаться быстро, перебирая ногами",
                        examples =
                            listOf(
                                "She had to [[run]] to catch the last train.",
                                "They [[ran]] across the field.",
                            ),
                        complementation = listOf("intransitive"),
                        grammarTags = listOf(GrammarTagHint("transitivity", "intransitive")),
                        irregularForms = IrregularFormsHint("run", "ran", "run"),
                    ),
                    EnrichmentSuggestion(
                        translation = "управлять",
                        surfaceForm = "run",
                        unitType = "irregular_verb",
                        baseLemma = "run",
                        explanation = "руководить делом или организацией",
                        examples =
                            listOf(
                                "She [[runs]] a small bakery downtown.",
                                "He [[ran]] the company for ten years.",
                            ),
                        complementation = listOf("noun"),
                        usageLabels = listOf(UsageLabelHint("domain", "business")),
                        grammarTags = listOf(GrammarTagHint("transitivity", "transitive")),
                        irregularForms = IrregularFormsHint("run", "ran", "run"),
                    ),
                ),
            "work" to
                listOf(
                    EnrichmentSuggestion(
                        translation = "работа",
                        surfaceForm = "work",
                        unitType = "noun",
                        baseLemma = "work",
                        explanation = "деятельность, занятие; то, что нужно сделать",
                        examples = listOf("I have a lot of [[work]] to do today."),
                        grammarTags = listOf(GrammarTagHint("countability", "uncountable")),
                    ),
                    EnrichmentSuggestion(
                        translation = "работать",
                        surfaceForm = "work",
                        unitType = "verb",
                        baseLemma = "work",
                        explanation = "выполнять работу; функционировать",
                        examples =
                            listOf(
                                "She [[works]] at a hospital.",
                                "This old radio still [[works]].",
                            ),
                        governedPrepositions =
                            listOf(
                                PrepositionGovernmentHint(
                                    alternatives = listOf("at", "for", "on"),
                                    example = "work at a hospital · work for a company · work on a project",
                                ),
                            ),
                        complementation = listOf("intransitive", "prepositional_phrase"),
                        grammarTags = listOf(GrammarTagHint("transitivity", "intransitive")),
                    ),
                ),
        )
}
