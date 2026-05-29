package app.sensee.verification.freeDictionary

import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.verification.core.Confidence
import app.sensee.verification.core.LexicalEntryLookup
import app.sensee.verification.core.LexicalEntryLookupResult
import app.sensee.verification.core.LexicalExistence
import app.sensee.verification.core.LexicalSourceRef
import app.sensee.verification.core.LexicalVerificationQuery
import app.sensee.verification.core.NormalizationCandidate
import app.sensee.verification.core.NormalizationKind
import app.sensee.verification.core.NormalizationOutcome
import app.sensee.verification.core.PartOfSpeechHint
import app.sensee.verification.core.PronunciationInfo
import app.sensee.verification.core.PronunciationVariant
import app.sensee.verification.core.VerifierAvailability
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlinx.serialization.SerializationException

/**
 * Adapter over dictionaryapi.dev. The seam rule "never throw across" holds:
 * any network failure or schema drift maps to [VerifierAvailability.Degraded]
 * or [VerifierAvailability.Unavailable] with an empty payload — the caller's
 * aggregator carries the other sources unaffected.
 *
 * Pronunciation rides too because the same endpoint returns IPA + audio in a
 * single round trip; splitting it across two adapters would double the
 * remote call without benefit.
 *
 * The adapter respects [LexicalVerificationQuery.policy] `allowNetwork`. The
 * orchestrator passes `allowNetwork = false` for offline runs (or when a
 * privacy setting disallows third-party lookups); in that mode this returns
 * `Unavailable` immediately, without contacting the service.
 */
public class FreeDictionaryLexicalAdapter(
    private val httpClient: HttpClient,
    private val clock: () -> Long = { 0L },
    private val baseUrl: String = DEFAULT_BASE_URL,
) : LexicalEntryLookup {
    override suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult {
        if (!query.policy.allowNetwork) {
            return notAvailable("network lookups disabled by policy")
        }
        if (query.studyLanguageTag.lowercase() !in SUPPORTED_LANGUAGE_TAGS) {
            return notAvailable("free-dictionary supports English only, got ${query.studyLanguageTag}")
        }
        val term = query.text.trim()
        if (term.isEmpty()) return notAvailable("empty term")
        if (' ' in term) {
            // Free Dictionary returns 404 for multi-word units; do not waste
            // a request and do not call them missing — they may still exist
            // in another source (e.g. the curated family/sense data) the
            // aggregator consults.
            return notAvailable("free-dictionary does not index multi-word units")
        }
        return runCatchingNetwork(term) { response ->
            val entries = parseEntries(response) ?: return@runCatchingNetwork degraded("malformed payload")
            if (entries.isEmpty()) return@runCatchingNetwork notFound(term)
            confirmed(term, entries)
        }
    }

    private suspend fun runCatchingNetwork(
        term: String,
        block: suspend (HttpResponse) -> LexicalEntryLookupResult,
    ): LexicalEntryLookupResult =
        runCatchingCancellable {
            val response: HttpResponse = httpClient.get(buildUrl(term))
            block(response)
        }.getOrElse { failure ->
            when (failure) {
                is ClientRequestException ->
                    if (failure.response.status == HttpStatusCode.NotFound) {
                        notFound(term)
                    } else {
                        degraded("HTTP ${failure.response.status.value}")
                    }
                else -> degraded(failure.message ?: failure::class.simpleName ?: "network error")
            }
        }

    private fun buildUrl(term: String): String =
        URLBuilder(baseUrl)
            // `appendPathSegments(encodeSlash = true)` encodes the term into a
            // single path segment: `?`, `#`, control chars AND `/` (so `..` /
            // `a/b` cannot escape into the URL structure as path traversal or
            // extra segments). `encodeSlash` defaults to false, which would
            // leak slashes; the caller's trim+lowercase guards none of this.
            .apply { appendPathSegments(term, encodeSlash = true) }
            .buildString()

    private suspend fun parseEntries(response: HttpResponse): List<FreeDictionaryEntryDto>? =
        try {
            response.body<List<FreeDictionaryEntryDto>>()
        } catch (_: SerializationException) {
            null
        }

    private fun confirmed(
        term: String,
        entries: List<FreeDictionaryEntryDto>,
    ): LexicalEntryLookupResult {
        val ref = ref(term)
        val firstEntryWord = entries.firstOrNull()?.word
        val headword = firstEntryWord?.trim().orNullIfBlank() ?: term
        return LexicalEntryLookupResult(
            availability = VerifierAvailability.Available,
            existence = LexicalExistence.Confirmed,
            normalized = normalizationOutcome(term, headword, ref),
            entryType = null,
            partsOfSpeech = extractPartsOfSpeech(entries),
            pronunciation = extractPronunciation(entries),
            confidence = Confidence.Medium,
            sources = listOf(ref),
        )
    }

    private fun notFound(term: String): LexicalEntryLookupResult =
        emptyResult(
            availability = VerifierAvailability.Available,
            existence = LexicalExistence.NotFound,
            sources = listOf(ref(term)),
            confidence = Confidence.Medium,
        )

    private fun degraded(reason: String): LexicalEntryLookupResult =
        emptyResult(
            availability = VerifierAvailability.Degraded(reason),
            existence = LexicalExistence.Unknown,
        )

    private fun notAvailable(reason: String): LexicalEntryLookupResult =
        emptyResult(
            availability = VerifierAvailability.Unavailable(reason),
            existence = LexicalExistence.Unknown,
        )

    private fun ref(term: String): LexicalSourceRef =
        LexicalSourceRef(
            sourceId = FreeDictionarySource.ID,
            entryId = term,
            url = buildUrl(term),
            fetchedAtEpochMillis = clock(),
        )

    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://api.dictionaryapi.dev/api/v2/entries/en"
        private val SUPPORTED_LANGUAGE_TAGS: Set<String> = setOf("en", "en-us", "en-gb")
    }
}

private fun extractPartsOfSpeech(entries: List<FreeDictionaryEntryDto>): List<PartOfSpeechHint> =
    entries
        .flatMap { it.meanings }
        .mapNotNull { it.partOfSpeech?.trim()?.takeIf(String::isNotEmpty) }
        .distinct()
        .map(::PartOfSpeechHint)

private fun extractPronunciation(entries: List<FreeDictionaryEntryDto>): PronunciationInfo? {
    val fallback = entries.firstNotNullOfOrNull { it.phonetic }
    val variants =
        entries
            .flatMap { it.phonetics }
            .mapNotNull { variant ->
                val ipa = variant.text?.trim().orNullIfBlank() ?: fallback
                if (ipa.isNullOrBlank() && variant.audio.isNullOrBlank()) {
                    null
                } else {
                    PronunciationVariant(
                        accent = null,
                        ipa = ipa,
                        audioUrl = variant.audio.orNullIfBlank(),
                    )
                }
            }
    return variants.takeIf { it.isNotEmpty() }?.let(::PronunciationInfo)
}

private fun normalizationOutcome(
    term: String,
    headword: String,
    ref: LexicalSourceRef,
): NormalizationOutcome =
    if (headword.equals(term, ignoreCase = true) && headword != term) {
        NormalizationOutcome(
            canonical = headword,
            candidates =
                listOf(
                    NormalizationCandidate(
                        text = headword,
                        kind = NormalizationKind.CaseFix,
                        source = ref,
                        confidence = Confidence.Medium,
                    ),
                ),
        )
    } else {
        NormalizationOutcome.EMPTY
    }

private fun emptyResult(
    availability: VerifierAvailability,
    existence: LexicalExistence,
    sources: List<LexicalSourceRef> = emptyList(),
    confidence: Confidence = Confidence.Low,
): LexicalEntryLookupResult =
    LexicalEntryLookupResult(
        availability = availability,
        existence = existence,
        normalized = NormalizationOutcome.EMPTY,
        entryType = null,
        partsOfSpeech = emptyList(),
        confidence = confidence,
        sources = sources,
    )

private fun String?.orNullIfBlank(): String? = this?.takeIf { it.isNotBlank() }
