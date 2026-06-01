package app.sensee.verification.datamuse

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
import app.sensee.verification.core.VerifierAvailability
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder

/**
 * Spelling-normalization adapter over Datamuse `/words?sp=`. The seam rule
 * "never throw across" holds — network failures degrade to
 * [VerifierAvailability.Degraded] / [VerifierAvailability.Unavailable] with
 * empty payload, never an exception.
 *
 * Result mapping:
 *
 *  * top hit equals the input (case-insensitive) — existence is `Confirmed`
 *    with no normalization candidate (Datamuse already agrees).
 *  * top hit differs from the input with a strong score —
 *    [LexicalExistence.NotFound] plus a [NormalizationKind.SpellFix]
 *    candidate at [Confidence.Medium] so the aggregator can surface a
 *    "did you mean…" hint without overriding a primary dictionary.
 *  * no hits — `Degraded("no spelling suggestions")` so the aggregator does
 *    not treat the silence as a definitive NotFound.
 *
 * Respects [LexicalVerificationQuery.policy] `allowNetwork`. English only.
 * Multi-word inputs are rejected up front — Datamuse `sp` is per-token.
 */
public class DatamuseLexicalAdapter(
    private val httpClient: HttpClient,
    private val clock: () -> Long = { 0L },
    private val baseUrl: String = DEFAULT_BASE_URL,
) : LexicalEntryLookup {
    override suspend fun lookup(query: LexicalVerificationQuery): LexicalEntryLookupResult {
        if (!query.policy.allowNetwork) return notAvailable("network disabled by policy")
        if (query.studyLanguageTag.lowercase() !in SUPPORTED_LANGUAGE_TAGS) {
            return notAvailable("datamuse supports English only, got ${query.studyLanguageTag}")
        }
        val term = query.text.trim()
        if (term.isEmpty()) return notAvailable("empty term")
        if (' ' in term) return notAvailable("datamuse spell suggestions are per-token only")
        return fetchHits(term)?.let { hits -> shape(term, hits) } ?: degraded("network failure")
    }

    private suspend fun fetchHits(term: String): List<DatamuseHitDto>? =
        runCatchingCancellable {
            val response: HttpResponse =
                httpClient.get(URLBuilder(baseUrl).buildString()) {
                    parameter("sp", term)
                    parameter("max", MAX_RESULTS.toString())
                }
            response.body<List<DatamuseHitDto>>()
        }.getOrElse {
            null
        }

    private fun shape(
        term: String,
        hits: List<DatamuseHitDto>,
    ): LexicalEntryLookupResult {
        val ref = ref(term)
        val nonEmpty = hits.filter { it.word.isNotBlank() }
        if (nonEmpty.isEmpty()) return degraded("no spelling suggestions")
        val top = nonEmpty.first()
        val exact = top.word.equals(term, ignoreCase = true)
        if (exact) {
            return LexicalEntryLookupResult(
                availability = VerifierAvailability.Available,
                existence = LexicalExistence.Confirmed,
                normalized = NormalizationOutcome.EMPTY,
                entryType = null,
                partsOfSpeech = emptyList(),
                confidence = Confidence.Low,
                sources = listOf(ref),
            )
        }
        // Top hit differs — pitch it as a spell-fix candidate. We only fire
        // when the top result clears the strong-suggestion threshold; weak
        // matches stay silent so we do not pull a primary dictionary down.
        val strong = top.score >= STRONG_SUGGESTION_SCORE
        val normalization =
            if (strong) {
                NormalizationOutcome(
                    canonical = top.word,
                    candidates =
                        listOf(
                            NormalizationCandidate(
                                text = top.word,
                                kind = NormalizationKind.SpellFix,
                                source = ref,
                                confidence = Confidence.Medium,
                            ),
                        ),
                )
            } else {
                NormalizationOutcome.EMPTY
            }
        return LexicalEntryLookupResult(
            availability = VerifierAvailability.Available,
            existence = if (strong) LexicalExistence.NotFound else LexicalExistence.Unknown,
            normalized = normalization,
            entryType = null,
            partsOfSpeech = emptyList(),
            confidence = if (strong) Confidence.Medium else Confidence.Low,
            sources = listOf(ref),
        )
    }

    private fun ref(term: String): LexicalSourceRef =
        LexicalSourceRef(
            sourceId = DatamuseSource.ID,
            entryId = term,
            // Build the attribution URL the same way as the request so a term
            // with reserved characters is encoded, not pasted raw.
            url = URLBuilder(baseUrl).apply { parameters.append("sp", term) }.buildString(),
            fetchedAtEpochMillis = clock(),
        )

    private fun degraded(reason: String): LexicalEntryLookupResult =
        LexicalEntryLookupResult(
            availability = VerifierAvailability.Degraded(reason),
            existence = LexicalExistence.Unknown,
            normalized = NormalizationOutcome.EMPTY,
            entryType = null,
            partsOfSpeech = emptyList(),
            confidence = Confidence.Low,
            sources = emptyList(),
        )

    private fun notAvailable(reason: String): LexicalEntryLookupResult =
        LexicalEntryLookupResult(
            availability = VerifierAvailability.Unavailable(reason),
            existence = LexicalExistence.Unknown,
            normalized = NormalizationOutcome.EMPTY,
            entryType = null,
            partsOfSpeech = emptyList(),
            confidence = Confidence.Low,
            sources = emptyList(),
        )

    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://api.datamuse.com/words"
        public const val STRONG_SUGGESTION_SCORE: Long = 50L
        public const val MAX_RESULTS: Int = 5
        private val SUPPORTED_LANGUAGE_TAGS: Set<String> = setOf("en", "en-us", "en-gb")
    }
}
