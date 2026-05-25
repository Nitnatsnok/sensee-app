package app.sensee.ai.core

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Single source of truth for the [EnrichmentResponseV1] wire shape: one
 * [Field] per [EnrichmentItemV1] property. Both representations derive from
 * [fields]:
 * - [jsonSkeleton] — compact shape example, embedded in the user prompt.
 * - [buildJsonSchema] — JSON Schema (`json_schema` response_format). The four
 *   taxonomy-bound fields get enum/oneOf from caller-supplied neutral sets,
 *   so a structured-output provider cannot return an id outside the loaded
 *   taxonomy.
 *
 * `EnrichmentSchemaTest` asserts the wire DTO and this catalog stay aligned
 * (ADR-005: schema changes are deliberate). Cross-cutting prompt rules live
 * in [EnrichmentRequestModifier]s, not here.
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

    public data class Field(
        val serialName: String,
        val shape: ShapeType,
        val guidance: String = "",
    )

    private fun text(
        serialName: String,
        guidance: String = "",
    ): Field = Field(serialName, ShapeType.Text, guidance)

    /** Order matches [EnrichmentItemV1] so [jsonSkeleton] stays stable. */
    public val fields: List<Field> =
        listOf(
            text("translation", "short native-language equivalent of this sense"),
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
            ),
            text("base_lemma", "dictionary base form of the unit"),
            text(
                "explanation",
                "concise meaning of this sense, in the NATIVE language — the learner " +
                    "disambiguates and picks senses by it",
            ),
            Field(
                serialName = "examples",
                shape = ShapeType.ArrayOf(ShapeType.Text),
                guidance =
                    "at least one; one per significant construction/variant of THIS " +
                        "sense. Wrap the exact occurrence in [[ ]] span(s), " +
                        "exactly as it appears — inflected (\"She [[came across]] the " +
                        "letters.\"), separated phrasal verbs (\"He [[turned]] the light " +
                        "[[on]].\") or multi-word (\"It was [[a piece of cake]].\"). Use " +
                        "multiple spans only when one occurrence is discontinuous; never " +
                        "split a contiguous occurrence into several spans",
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
                        "with an optional example. A preposition/particle that CHANGES " +
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
        "{\"version\":1,\"items\":[${compact(ShapeType.ObjectOf(fields))}]}"

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

    private const val UNIT_TYPE = "unit_type"
    private const val COMPLEMENTATION = "complementation"
    private const val USAGE_LABELS = "usage_labels"
    private const val GRAMMAR_TAGS = "grammar_tags"

    /**
     * JSON Schema for the [EnrichmentResponseV1] envelope. Empty sets/maps
     * collapse to the structural shape (free string or `{key, value}` object)
     * so a failed taxonomy fetch still produces a usable schema. Non-strict:
     * optional DTO fields stay optional, matching the "omit unknown fields"
     * prompt rule (ADR-005).
     */
    @Suppress("ProfiledLongParameterList")
    public fun buildJsonSchema(
        unitTypeIds: Set<String>,
        complementIds: Set<String>,
        usageAxesAndValues: Map<String, Set<String>>,
        grammarCategoriesAndForms: Map<String, Set<String>>,
        extensions: Set<AiEnrichmentExtension> = emptySet(),
    ): JsonElement =
        buildJsonObject {
            put("type", "object")
            put(
                "properties",
                buildJsonObject {
                    put("version", buildJsonObject { put("type", "integer") })
                    put(
                        "items",
                        buildJsonObject {
                            put("type", "array")
                            put(
                                "items",
                                itemSchema(
                                    unitTypeIds = unitTypeIds,
                                    complementIds = complementIds,
                                    usageAxesAndValues = usageAxesAndValues,
                                    grammarCategoriesAndForms = grammarCategoriesAndForms,
                                    extensions = extensions,
                                ),
                            )
                        },
                    )
                },
            )
            put("required", buildJsonArray { add("items") })
            put("additionalProperties", false)
        }

    @Suppress("ProfiledLongParameterList")
    private fun itemSchema(
        unitTypeIds: Set<String>,
        complementIds: Set<String>,
        usageAxesAndValues: Map<String, Set<String>>,
        grammarCategoriesAndForms: Map<String, Set<String>>,
        extensions: Set<AiEnrichmentExtension>,
    ): JsonElement =
        buildJsonObject {
            put("type", "object")
            put(
                "properties",
                buildJsonObject {
                    val builtInNames = fields.mapTo(mutableSetOf()) { it.serialName }
                    fields.forEach { field ->
                        put(
                            field.serialName,
                            constrainedFieldSchema(
                                field = field,
                                unitTypeIds = unitTypeIds,
                                complementIds = complementIds,
                                usageAxesAndValues = usageAxesAndValues,
                                grammarCategoriesAndForms = grammarCategoriesAndForms,
                            ),
                        )
                    }
                    // Built-ins win on key collision: ADR-006 mandates disjoint
                    // ownership but a misconfigured extension shouldn't be able
                    // to overwrite the wire DTO shape silently.
                    extensions
                        .flatMap { it.fields() }
                        .forEach { field ->
                            if (field.serialName in builtInNames) return@forEach
                            put(field.serialName, schemaOf(field.shape, field.guidance))
                        }
                },
            )
            put("additionalProperties", false)
        }

    private fun constrainedFieldSchema(
        field: Field,
        unitTypeIds: Set<String>,
        complementIds: Set<String>,
        usageAxesAndValues: Map<String, Set<String>>,
        grammarCategoriesAndForms: Map<String, Set<String>>,
    ): JsonElement =
        when (field.serialName) {
            UNIT_TYPE -> stringEnumSchema(unitTypeIds, field.guidance)
            COMPLEMENTATION ->
                buildJsonObject {
                    put("type", "array")
                    put("items", stringEnumSchema(complementIds, guidance = ""))
                    if (field.guidance.isNotBlank()) put("description", field.guidance)
                }
            USAGE_LABELS ->
                arrayOfTaggedPairs(
                    discriminatorKey = "axis",
                    valueKey = "value",
                    allowedByDiscriminator = usageAxesAndValues,
                    guidance = field.guidance,
                )
            GRAMMAR_TAGS ->
                arrayOfTaggedPairs(
                    discriminatorKey = "category",
                    valueKey = "form",
                    allowedByDiscriminator = grammarCategoriesAndForms,
                    guidance = field.guidance,
                )
            else -> schemaOf(field.shape, field.guidance)
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
