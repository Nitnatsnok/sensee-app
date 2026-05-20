package app.sensee.ai.core

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Human-maintained description of the versioned [EnrichmentResponseV1] wire
 * shape: one [Field] per [EnrichmentItemV1] property, carrying its [ShapeType]
 * and the per-field guidance prose.
 *
 * Single source of truth for the schema. Both representations are derived from
 * [fields], so they cannot diverge:
 * - [jsonSkeleton] — the compact shape example embedded in the provider prompt.
 * - [jsonSchema] — a JSON Schema for providers that accept a `json_schema`
 *   `response_format` (the de-facto OpenAI-compatible standard).
 *
 * `EnrichmentSchemaTest` asserts every wire field has an entry so the DTO and
 * the schema cannot silently drift (ADR-005: schema changes are deliberate).
 *
 * Cross-cutting instructions (sense splitting, language autodetection, example
 * marking) are NOT per-field and stay in the provider prompt builder, not here.
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
                        "sense. Wrap the exact occurrence in a SINGLE [[ ]] span, " +
                        "exactly as it appears — inflected (\"She [[came across]] the " +
                        "letters.\"), separated (\"He [[turned the light on]].\") or " +
                        "multi-word (\"It was [[a piece of cake]].\"); never split the " +
                        "span into several [[ ]]",
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
                        "connotation(neutral_connotation/positive_connotation/" +
                        "pejorative/euphemistic), " +
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

    /**
     * JSON Schema for the full [EnrichmentResponseV1] envelope. Non-strict by
     * design: optional fields stay optional, matching the forward-compatible
     * DTO and the prompt's "omit unknown fields" guidance (ADR-005).
     */
    public val jsonSchema: String =
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
                            put("items", schemaOf(ShapeType.ObjectOf(fields), ""))
                        },
                    )
                },
            )
            put("required", buildJsonArray { add("items") })
            put("additionalProperties", false)
        }.toString()
}
