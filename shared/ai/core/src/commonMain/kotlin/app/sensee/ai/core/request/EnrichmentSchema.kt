package app.sensee.ai.core.request

import app.sensee.ai.core.contract.AiEnrichmentExtension
import app.sensee.ai.core.wire.EnrichmentItemV1
import app.sensee.ai.core.wire.EnrichmentResponseMapper
import app.sensee.ai.core.wire.EnrichmentResponseV1
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Single source of truth for the [EnrichmentResponseV1] wire shape. Drives
 * both [jsonSkeleton] (prompt example) and [buildJsonSchema] (JSON Schema for
 * `json_schema` response_format).
 */
public object EnrichmentSchema {
    public sealed interface ShapeType {
        public data object Text : ShapeType

        public data class ArrayOf(
            val element: ShapeType,
        ) : ShapeType

        public data class ObjectOf(
            val fields: List<Field>,
        ) : ShapeType
    }

    /**
     * Taxonomy-bound constraint a built-in field carries into [buildJsonSchema].
     * Lives on the [Field] declaration so the constraint travels with the field
     * instead of a separate name-keyed lookup; the builder reads the matching
     * [EnrichmentTaxonomy] slice. Extension fields are always [None].
     */
    public sealed interface FieldConstraint {
        /** No taxonomy constraint; the field keeps its structural [ShapeType]. */
        public data object None : FieldConstraint

        /** A single string constrained to the taxonomy's unit-type ids. */
        public data object UnitType : FieldConstraint

        /** An array of strings constrained to the taxonomy's complement ids. */
        public data object Complementation : FieldConstraint

        /** An array of {axis, value} pairs, one `oneOf` branch per usage axis. */
        public data object UsageLabels : FieldConstraint

        /** An array of {category, form} pairs, one `oneOf` branch per grammar category. */
        public data object GrammarTags : FieldConstraint
    }

    public data class Field(
        val serialName: String,
        val shape: ShapeType,
        val guidance: String = "",
        val constraint: FieldConstraint = FieldConstraint.None,
    )

    private fun text(
        serialName: String,
        guidance: String = "",
        constraint: FieldConstraint = FieldConstraint.None,
    ): Field = Field(serialName, ShapeType.Text, guidance, constraint)

    /** Order matches [EnrichmentItemV1] so [jsonSkeleton] stays stable. */
    public val fields: List<Field> =
        listOf(
            text(
                "translation",
                "short native-language equivalent of this sense; not the " +
                    "study-language lemma, not an infinitive gloss, not a " +
                    "comma-separated list of study-language options",
            ),
            text(
                "surface_form",
                "the unit as written for THIS sense: fixed parts as plain words " +
                    "(look down on), an optional particle as [as], a governed/argument " +
                    "slot as <something>. This single field carries phrasal-verb " +
                    "particles, idiom/phrase fixed words and argument slots — there is " +
                    "no separate construction object",
            ),
            text(
                "unit_type",
                "YOU classify the sense (the user never declares it). Plain words: " +
                    "their part of speech (noun, verb, irregular_verb, adjective, " +
                    "adverb, pronoun, determiner, numeral, article, preposition, " +
                    "conjunction, interjection). Multi-word: phrasal_verb, idiom, " +
                    "phrase. A prepositional / phrasal-prepositional verb is " +
                    "phrasal_verb with its fixed parts in surface_form",
                constraint = FieldConstraint.UnitType,
            ),
            text("base_lemma", "dictionary base form of the unit"),
            text(
                "head_lemma",
                "the family head's base form: for a phrasal verb (`come across`) or " +
                    "verbal idiom (`come of age`) this is the verb head's lemma " +
                    "(`come`); for a single-word unit it equals base_lemma; for an " +
                    "idiom with no clear head omit",
            ),
            Field(
                serialName = "components",
                shape =
                    ShapeType.ArrayOf(
                        ShapeType.ObjectOf(listOf(text("text"), text("role"), text("salience"))),
                    ),
                guidance =
                    "structural breakdown of a multi-word unit, ordered as the unit " +
                        "is written. Each entry is {text, role, salience}; role is one " +
                        "of: head, particle, preposition, fixed_object, modifier, other; " +
                        "salience ranks the part's significance to the meaning — primary " +
                        "(the semantic head, e.g. come in `come across`), secondary (a " +
                        "meaning-shaping particle/preposition, e.g. across), incidental " +
                        "(a fixed grammatical filler, e.g. the in `kick the bucket`). " +
                        "Example: `come across` → [{text:come, role:head, " +
                        "salience:primary}, {text:across, role:particle, " +
                        "salience:secondary}]. Include optional fixed words as plain " +
                        "text without brackets when they are part of the lexical " +
                        "pattern; never include angle-bracket argument slots like " +
                        "<someone> or <verb>. Omit for single-word units",
            ),
            text(
                "explanation",
                "concise meaning of this sense, in the NATIVE language — the learner " +
                    "disambiguates and picks senses by it",
            ),
            Field(
                serialName = "examples",
                shape =
                    ShapeType.ArrayOf(
                        ShapeType.ObjectOf(
                            listOf(
                                text("sentence"),
                                text("translation"),
                                Field(
                                    serialName = "alignment",
                                    shape =
                                        ShapeType.ArrayOf(
                                            ShapeType.ObjectOf(
                                                listOf(text("source"), text("target")),
                                            ),
                                        ),
                                ),
                            ),
                        ),
                    ),
                guidance =
                    "at least one; one per significant construction/variant of THIS " +
                        "sense. Each entry is {sentence, translation, alignment}. " +
                        "`sentence` is the study-language example — wrap the exact " +
                        "occurrence in [[ ]] span(s), exactly as it appears — inflected " +
                        "(\"She [[came across]] the letters.\"), separated phrasal verbs " +
                        "(\"He [[turned]] the light [[on]].\") or multi-word " +
                        "(\"It was [[a piece of cake]].\"). Use multiple spans only when " +
                        "one occurrence is discontinuous; never split a contiguous " +
                        "occurrence into several spans. `translation` is the " +
                        "native-language rendering of the same sentence. `alignment` is " +
                        "the pre-segmented phrase-pair list mapping study-language chunks " +
                        "to their native-language counterparts. It is not a target-only " +
                        "gloss: provide natural phrase chunks in reading order, usually " +
                        "3-8 chunks for a normal sentence; together they should cover the " +
                        "whole study-language sentence and the whole native-language " +
                        "translation. Include the chunk containing the studied unit; " +
                        "alignment.source keeps the surface words without [[ ]] markers. " +
                        "Do not return a single alignment chunk unless the sentence is " +
                        "genuinely one phrase",
            ),
            Field(
                serialName = "synonyms",
                shape = ShapeType.ArrayOf(ShapeType.Text),
                guidance =
                    "near-synonyms for THIS sense in the study language (the unit's own " +
                        "language), closest first; omit if none are genuinely close",
            ),
            Field(
                serialName = "antonyms",
                shape = ShapeType.ArrayOf(ShapeType.Text),
                guidance =
                    "opposites for THIS sense in the study language; omit when the sense " +
                        "has no real antonym",
            ),
            Field(
                serialName = "collocations",
                shape = ShapeType.ArrayOf(ShapeType.Text),
                guidance =
                    "characteristic collocations for THIS sense — frequent multi-word " +
                        "partners as short study-language phrases (e.g. for the 'severe' " +
                        "sense of heavy: heavy rain, heavy traffic); omit if none are " +
                        "distinctive",
            ),
            Field(
                serialName = "word_family",
                shape =
                    ShapeType.ArrayOf(
                        ShapeType.ObjectOf(listOf(text("lemma"), text("unit_type"))),
                    ),
                guidance =
                    "derivational family of base_lemma: dictionary words built from the " +
                        "same root, each {lemma, unit_type}; not synonyms, translations " +
                        "or inflected forms (e.g. for decide: " +
                        "{decision, noun}, {decisive, adjective}, {decisively, adverb}). " +
                        "This is the lemma→derivative link the app shows. Omit for " +
                        "multi-word units (phrasal verbs, idioms, phrases) and when no " +
                        "common relatives exist",
            ),
            Field(
                serialName = "preposition_government",
                shape =
                    ShapeType.ArrayOf(
                        ShapeType.ObjectOf(
                            listOf(
                                Field("alternatives", ShapeType.ArrayOf(ShapeType.Text)),
                                text("example"),
                            ),
                        ),
                    ),
                guidance =
                    "ONLY governed, meaning-preserving prepositions: group ones " +
                        "interchangeable for THIS sense (e.g. different from/to/than) " +
                        "with an optional example. Alternatives are bare prepositions " +
                        "only, not full phrases or translated glosses. A preposition/particle that CHANGES " +
                        "the meaning (look at vs look after vs look for) is a separate " +
                        "sense item, not an alternative; a fixed part of the unit " +
                        "(look down on) goes in surface_form, not here",
            ),
            Field(
                serialName = "complementation",
                shape = ShapeType.ArrayOf(ShapeType.Text),
                guidance =
                    "what this sense takes, ids from: noun, gerund, to_infinitive, " +
                        "bare_infinitive, that_clause, wh_clause, adjective, " +
                        "prepositional_phrase, intransitive",
                constraint = FieldConstraint.Complementation,
            ),
            Field(
                serialName = "usage_labels",
                shape =
                    ShapeType.ArrayOf(
                        ShapeType.ObjectOf(listOf(text("axis"), text("value"))),
                    ),
                guidance =
                    "axis/value pairs from: " +
                        "register(formal/informal/slang/literary/neutral), " +
                        "region(bre/ame/ause/cane), " +
                        "domain(law/medicine/it/science/business), " +
                        "connotation(neutral_connotation/approving/disapproving/euphemistic), " +
                        "temporality(current/dated/archaic/obsolete)",
                constraint = FieldConstraint.UsageLabels,
            ),
            text(
                "usage_note",
                "short free selectional restriction in the NATIVE language, only " +
                    "when one applies (e.g. \"только о жирах и маслах\")",
            ),
            Field(
                serialName = "grammar_tags",
                shape =
                    ShapeType.ArrayOf(
                        ShapeType.ObjectOf(listOf(text("category"), text("form"))),
                    ),
                guidance =
                    "category/form pairs as lexicographic ids: e.g. " +
                        "{verb_irregular, infinitive} for an irregular verb, " +
                        "{separability, inseparable} for an inseparable phrasal verb, " +
                        "{expression_type, fixed} for an idiom/fixed expression",
                constraint = FieldConstraint.GrammarTags,
            ),
            Field(
                serialName = "irregular_forms",
                shape =
                    ShapeType.ObjectOf(
                        listOf(text("base"), text("past"), text("past_participle")),
                    ),
                guidance =
                    "irregular verb only: the stored principal parts " +
                        "base/past/past_participle (come/came/come); other forms are " +
                        "rule-derived and not returned",
            ),
        )

    /** Wire-field names of the built-in item shape; single source for collision checks. */
    public val builtInFieldNames: Set<String> = fields.mapTo(LinkedHashSet()) { it.serialName }

    private fun compact(shape: ShapeType): String =
        when (shape) {
            ShapeType.Text -> "\"string\""
            is ShapeType.ArrayOf -> "[${compact(shape.element)}]"
            is ShapeType.ObjectOf ->
                shape.fields.joinToString(",", "{", "}") {
                    "\"${it.serialName}\":${compact(it.shape)}"
                }
        }

    /** The compact JSON example handed to the model in the prompt. */
    public val jsonSkeleton: String =
        "{\"version\":${EnrichmentResponseV1.SCHEMA_VERSION},\"items\":[${compact(ShapeType.ObjectOf(fields))}]}"

    private fun schemaOf(
        shape: ShapeType,
        guidance: String,
    ): JsonElement =
        buildJsonObject {
            when (shape) {
                ShapeType.Text -> put("type", "string")
                is ShapeType.ArrayOf -> {
                    put("type", "array")
                    put("items", schemaOf(shape.element, ""))
                }
                is ShapeType.ObjectOf -> {
                    put("type", "object")
                    put(
                        "properties",
                        buildJsonObject {
                            shape.fields.forEach {
                                put(it.serialName, schemaOf(it.shape, it.guidance))
                            }
                        },
                    )
                    put("additionalProperties", false)
                }
            }
            if (guidance.isNotBlank()) put("description", guidance)
        }

    /**
     * JSON Schema for the [EnrichmentResponseV1] envelope. Empty taxonomy
     * sets/maps collapse to the structural shape so a failed taxonomy fetch
     * still yields a usable schema.
     *
     * Advisory by design: the LLM seam sends this in non-strict `json_schema`
     * mode (no `strict: true`, no item-level `required`). Hard correctness is
     * enforced downstream by [EnrichmentResponseMapper] and the feature-side
     * runtime taxonomy `resolve`, so partial/degraded answers still parse.
     */
    public fun buildJsonSchema(
        taxonomy: EnrichmentTaxonomy,
        extensions: Set<AiEnrichmentExtension> = emptySet(),
    ): JsonElement =
        buildJsonObject {
            put("type", "object")
            put(
                "properties",
                buildJsonObject {
                    put(
                        "version",
                        buildJsonObject {
                            put("type", "integer")
                            put("const", EnrichmentResponseV1.SCHEMA_VERSION)
                        },
                    )
                    put(
                        "items",
                        buildJsonObject {
                            put("type", "array")
                            put("items", itemSchema(taxonomy, extensions))
                        },
                    )
                },
            )
            put(
                "required",
                buildJsonArray {
                    add("version")
                    add("items")
                },
            )
            put("additionalProperties", false)
        }

    private fun itemSchema(
        taxonomy: EnrichmentTaxonomy,
        extensions: Set<AiEnrichmentExtension>,
    ): JsonElement =
        buildJsonObject {
            put("type", "object")
            put(
                "properties",
                buildJsonObject {
                    fields.forEach { field ->
                        put(field.serialName, fieldSchema(field, taxonomy))
                    }
                    // Built-ins win on key collision: ADR-006 mandates disjoint
                    // ownership but a misconfigured extension shouldn't be able
                    // to overwrite the wire DTO shape silently.
                    extensions
                        .flatMap { it.fields() }
                        .forEach { field ->
                            if (field.serialName in builtInFieldNames) return@forEach
                            put(field.serialName, schemaOf(field.shape, field.guidance))
                        }
                },
            )
            put("additionalProperties", false)
        }

    private fun fieldSchema(
        field: Field,
        taxonomy: EnrichmentTaxonomy,
    ): JsonElement =
        when (field.constraint) {
            FieldConstraint.None -> schemaOf(field.shape, field.guidance)
            FieldConstraint.UnitType -> stringEnumSchema(taxonomy.unitTypeIds, field.guidance)
            FieldConstraint.Complementation ->
                buildJsonObject {
                    put("type", "array")
                    put("items", stringEnumSchema(taxonomy.complementIds, guidance = ""))
                    if (field.guidance.isNotBlank()) put("description", field.guidance)
                }
            FieldConstraint.UsageLabels ->
                arrayOfTaggedPairs(
                    discriminatorKey = "axis",
                    valueKey = "value",
                    allowedByDiscriminator = taxonomy.usageAxesAndValues,
                    guidance = field.guidance,
                )
            FieldConstraint.GrammarTags ->
                arrayOfTaggedPairs(
                    discriminatorKey = "category",
                    valueKey = "form",
                    allowedByDiscriminator = taxonomy.grammarCategoriesAndForms,
                    guidance = field.guidance,
                )
        }

    private fun stringEnumSchema(
        ids: Set<String>,
        guidance: String,
    ): JsonElement =
        buildJsonObject {
            put("type", "string")
            if (ids.isNotEmpty()) {
                put("enum", buildJsonArray { ids.sorted().forEach { add(it) } })
            }
            if (guidance.isNotBlank()) put("description", guidance)
        }

    private fun arrayOfTaggedPairs(
        discriminatorKey: String,
        valueKey: String,
        allowedByDiscriminator: Map<String, Set<String>>,
        guidance: String,
    ): JsonElement =
        buildJsonObject {
            put("type", "array")
            put(
                "items",
                if (allowedByDiscriminator.isEmpty()) {
                    schemaOf(
                        shape =
                            ShapeType.ObjectOf(
                                listOf(text(discriminatorKey), text(valueKey)),
                            ),
                        guidance = "",
                    )
                } else {
                    taggedPairOneOf(
                        discriminatorKey = discriminatorKey,
                        valueKey = valueKey,
                        allowedByDiscriminator = allowedByDiscriminator,
                    )
                },
            )
            if (guidance.isNotBlank()) put("description", guidance)
        }

    private fun taggedPairOneOf(
        discriminatorKey: String,
        valueKey: String,
        allowedByDiscriminator: Map<String, Set<String>>,
    ): JsonElement =
        buildJsonObject {
            put(
                "oneOf",
                buildJsonArray {
                    val orderedEntries = allowedByDiscriminator.entries.sortedBy { it.key }
                    orderedEntries.forEach { (discriminator, values) ->
                        add(
                            buildJsonObject {
                                put("type", "object")
                                put(
                                    "properties",
                                    buildJsonObject {
                                        put(
                                            discriminatorKey,
                                            buildJsonObject { put("const", discriminator) },
                                        )
                                        put(valueKey, stringEnumSchema(values, guidance = ""))
                                    },
                                )
                                put(
                                    "required",
                                    buildJsonArray {
                                        add(discriminatorKey)
                                        add(valueKey)
                                    },
                                )
                                put("additionalProperties", false)
                            },
                        )
                    }
                },
            )
        }
}
