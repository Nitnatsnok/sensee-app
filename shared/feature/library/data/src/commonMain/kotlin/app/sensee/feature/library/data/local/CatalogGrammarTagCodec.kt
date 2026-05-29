package app.sensee.feature.library.data.local

import app.sensee.core.observability.logging.AppLogger
import app.sensee.feature.library.data.remote.GrammarTagDto
import app.sensee.grammar.domain.GrammarTag
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// Codec for the lean `grammar_tags_json` column: domain tags <-> storage DTOs.
// The catalog stores a Sense's grammar tags as a small JSON array next to the
// full enrichment payload so list queries do not have to decode the whole Sense.
private val grammarTagListSerializer = ListSerializer(GrammarTagDto.serializer())

internal fun encodeGrammarTags(
    json: Json,
    tags: List<GrammarTagDto>,
): String = json.encodeToString(grammarTagListSerializer, tags)

// Malformed payload is treated as "no tags" so a single bad card doesn't break
// the whole deck; the failure is logged so it surfaces in diagnostics instead
// of disappearing silently.
internal fun parseGrammarTags(
    json: Json,
    raw: String,
    cardId: String,
    logger: AppLogger,
): List<GrammarTag> =
    try {
        json
            .decodeFromString(grammarTagListSerializer, raw)
            .mapNotNull(GrammarTagDto::toDomain)
    } catch (failure: SerializationException) {
        logger.warn(failure) { "Malformed grammar_tags_json for card $cardId; falling back to no tags" }
        emptyList()
    }

private fun GrammarTagDto.toDomain(): GrammarTag? =
    GrammarTag.resolve(
        categoryId = category,
        formId = form,
        allowedFormsByCategory = GrammarTag.knownAllowedFormsByCategory,
    )

// Domain tag -> storage DTO for the lean grammar_tags_json column. Ids round-trip
// (an Unknown branch still carries its id), so a tag the client does not know
// is stored verbatim rather than dropped.
internal fun GrammarTag.toDto(): GrammarTagDto = GrammarTagDto(category = category.id, form = form.id)
