package app.sensee.verification.freeDictionary

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of the free-dictionary success response (https://dictionaryapi.dev).
 * The endpoint returns a JSON array of entries (one term may have multiple
 * etymologies); each entry carries POS-grouped meanings and phonetics. Fields
 * not used by the adapter are omitted from this DTO — kotlinx-serialization
 * with the default `ignoreUnknownKeys = true` (set by the network factory)
 * keeps that forward-compatible if the API grows.
 */
@Serializable
internal data class FreeDictionaryEntryDto(
    @SerialName("word") val word: String,
    @SerialName("phonetic") val phonetic: String? = null,
    @SerialName("phonetics") val phonetics: List<FreeDictionaryPhoneticDto> = emptyList(),
    @SerialName("meanings") val meanings: List<FreeDictionaryMeaningDto> = emptyList(),
)

@Serializable
internal data class FreeDictionaryPhoneticDto(
    @SerialName("text") val text: String? = null,
    @SerialName("audio") val audio: String? = null,
)

@Serializable
internal data class FreeDictionaryMeaningDto(
    @SerialName("partOfSpeech") val partOfSpeech: String? = null,
    @SerialName("definitions") val definitions: List<FreeDictionaryDefinitionDto> = emptyList(),
)

@Serializable
internal data class FreeDictionaryDefinitionDto(
    @SerialName("definition") val definition: String? = null,
    @SerialName("example") val example: String? = null,
)
