package app.sensee.verification.languagetool

import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.verification.core.ExampleCheckRequest
import app.sensee.verification.core.ExampleCheckResult
import app.sensee.verification.core.ExampleIssue
import app.sensee.verification.core.ExampleLocation
import app.sensee.verification.core.ExampleQualityChecker
import app.sensee.verification.core.FindingSeverity
import app.sensee.verification.core.LexicalSourceRef
import app.sensee.verification.core.SentenceHint
import app.sensee.verification.core.SuggestedAction
import app.sensee.verification.core.VerifierAvailability
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath

/**
 * Grammar/style checker over the LanguageTool `/v2/check` endpoint. The
 * adapter flattens [SentenceHint.plainText], submits it, then maps each
 * match back to a structured [ExampleIssue]. The location strategy:
 *
 *  * a match that falls entirely inside one [SentenceHint.Segment] becomes
 *    [ExampleLocation.WithinSegment] with the segment-local range — the
 *    orchestrator can render the underline without re-parsing;
 *  * a match that crosses segment boundaries becomes
 *    [ExampleLocation.PlainTextSpan] so the orchestrator can decide whether
 *    to surface or normalize.
 *
 * If the first replacement on a match exists, the adapter produces a
 * [SuggestedAction.RewriteExample] candidate that re-builds the
 * [SentenceHint] with the substitution applied at plain-text level. The
 * rewrite always carries `requiresTargetReannotation = true` because the
 * substitution may have shifted Target boundaries by character count; the
 * orchestrator is responsible for the manual re-annotation step before
 * applying the rewrite.
 */
public class LanguageToolExampleChecker(
    private val httpClient: HttpClient,
    private val clock: () -> Long = { 0L },
    private val baseUrl: String = DEFAULT_BASE_URL,
) : ExampleQualityChecker {
    override suspend fun check(request: ExampleCheckRequest): ExampleCheckResult {
        if (request.sentence.segments.isEmpty()) return notAvailable("empty sentence")
        val plain = request.sentence.plainText()
        if (plain.isBlank()) return notAvailable("blank plain text")
        if (!request.policy.allowNetwork) return notAvailable("network lookups disabled")
        return fetchMatches(plain, request.studyLanguageTag)?.let { matches ->
            shape(request.sentence, matches)
        } ?: degraded("network failure")
    }

    private suspend fun fetchMatches(
        text: String,
        languageTag: String,
    ): List<LanguageToolMatchDto>? =
        runCatchingCancellable {
            val response =
                httpClient.submitForm(
                    url = buildUrl(),
                    formParameters =
                        Parameters.build {
                            append("text", text)
                            append("language", normalizeLang(languageTag))
                            append("enabledOnly", "false")
                        },
                )
            response.body<LanguageToolCheckResponse>().matches
        }.getOrElse {
            null
        }

    private fun shape(
        sentence: SentenceHint,
        matches: List<LanguageToolMatchDto>,
    ): ExampleCheckResult {
        if (matches.isEmpty()) {
            return ExampleCheckResult(
                availability = VerifierAvailability.Available,
                issues = emptyList(),
                rewrite = null,
                sources = listOf(ref()),
            )
        }
        val offsets = segmentOffsets(sentence)
        val plainTextLength = sentence.plainText().length
        val issues = matches.map { it.toIssue(offsets, plainTextLength) }
        val rewrite = buildRewrite(sentence, matches)
        return ExampleCheckResult(
            availability = VerifierAvailability.Available,
            issues = issues,
            rewrite = rewrite,
            sources = listOf(ref()),
        )
    }

    private fun LanguageToolMatchDto.toIssue(
        offsets: List<IntRange>,
        plainTextLength: Int,
    ): ExampleIssue =
        ExampleIssue(
            code = rule?.id?.ifBlank { null } ?: "languagetool",
            severity = severityFor(rule?.issueType),
            location = locateMatch(offset, length, offsets, plainTextLength),
            message = message.ifBlank { rule?.description.orEmpty() },
            sources = listOf(ref()),
        )

    private fun buildRewrite(
        sentence: SentenceHint,
        matches: List<LanguageToolMatchDto>,
    ): SuggestedAction.RewriteExample? {
        // All-or-nothing: only rewrite when every match carries a replacement.
        val ordered = matches.sortedByDescending { it.offset }
        if (ordered.any {
                it.replacements
                    .firstOrNull()
                    ?.value
                    .isNullOrBlank()
            }
        ) {
            return null
        }
        // Apply matches in reverse offset order so earlier offsets stay valid
        // as later ranges shrink/grow under replacement. `StringBuilder.replace`
        // is JVM-only, so use KMP-portable delete+insert.
        val builder = StringBuilder(sentence.plainText())
        ordered.forEach { match ->
            val replacement = match.replacements.first().value
            val start = match.offset.coerceIn(0, builder.length)
            val end = (match.offset + match.length).coerceIn(start, builder.length)
            builder.deleteRange(start, end)
            builder.insert(start, replacement)
        }
        return SuggestedAction.RewriteExample(
            draft = SentenceHint(listOf(SentenceHint.Segment.Text(builder.toString()))),
            // Target boundaries may have shifted; the orchestrator must
            // re-annotate before applying.
            requiresTargetReannotation = true,
        )
    }

    private fun buildUrl(): String =
        URLBuilder(baseUrl)
            .apply { encodedPath = "$encodedPath/check" }
            .buildString()

    private fun ref(): LexicalSourceRef =
        LexicalSourceRef(
            sourceId = LanguageToolSource.ID,
            fetchedAtEpochMillis = clock(),
        )

    private fun degraded(reason: String): ExampleCheckResult =
        ExampleCheckResult(
            availability = VerifierAvailability.Degraded(reason),
            issues = emptyList(),
            rewrite = null,
            sources = emptyList(),
        )

    private fun notAvailable(reason: String): ExampleCheckResult =
        ExampleCheckResult(
            availability = VerifierAvailability.Unavailable(reason),
            issues = emptyList(),
            rewrite = null,
            sources = emptyList(),
        )

    public companion object {
        public const val DEFAULT_BASE_URL: String = "https://api.languagetool.org/v2"
    }
}

private fun severityFor(issueType: String?): FindingSeverity =
    when (issueType?.lowercase()) {
        "misspelling", "grammar", "typographical" -> FindingSeverity.Error
        "style", "register", "redundancy" -> FindingSeverity.Warning
        else -> FindingSeverity.Info
    }

private fun segmentOffsets(sentence: SentenceHint): List<IntRange> {
    val offsets = mutableListOf<IntRange>()
    var cursor = 0
    sentence.segments.forEach { segment ->
        val end = cursor + segment.text.length
        offsets += cursor until end
        cursor = end
    }
    return offsets
}

private fun normalizeLang(languageTag: String): String =
    when (languageTag.lowercase()) {
        "en" -> "en-US"
        else -> languageTag
    }

private fun locateMatch(
    offset: Int,
    length: Int,
    offsets: List<IntRange>,
    plainTextLength: Int,
): ExampleLocation {
    // Clamp upstream offsets to the plain-text range so a malformed LT
    // response (negative offset, length past EOF, UTF-16-vs-code-point
    // mismatch) cannot ship a negative or reversed `IntRange` into the
    // domain (I5). Empty/clamped-empty spans degrade to WholeSentence rather
    // than emit a misleading WithinSegment range; a zero-length match (LT
    // emits these for some insertion-style rules) is treated as a caret
    // position at `offset` and is attributed to the segment that STARTS at
    // that caret — not the previous segment whose `last == caret - 1`.
    val safeStart = offset.coerceIn(0, plainTextLength)
    val safeEnd = (offset + length).coerceIn(safeStart, plainTextLength)
    if (length == 0) {
        return locateCaret(safeStart, offsets, plainTextLength)
    }
    if (safeStart == safeEnd) {
        return ExampleLocation.WholeSentence
    }
    val span = safeStart until safeEnd
    val segment =
        offsets.withIndex().firstOrNull { (_, segmentRange) ->
            segmentRange.first <= span.first && segmentRange.last >= span.last
        }
    return if (segment != null) {
        val segmentRange = segment.value
        val relativeStart = span.first - segmentRange.first
        val relativeEnd = span.last - segmentRange.first
        ExampleLocation.WithinSegment(segmentIndex = segment.index, range = relativeStart..relativeEnd)
    } else {
        ExampleLocation.PlainTextSpan(range = safeStart..(safeEnd - 1))
    }
}

private fun locateCaret(
    caret: Int,
    offsets: List<IntRange>,
    plainTextLength: Int,
): ExampleLocation {
    // A caret at position `c` belongs to the segment that STARTS at `c` (so
    // LT's "missing word" insertion attaches to the following segment). If no
    // segment starts there, fall back to the segment that contains the caret
    // via half-open semantics (`first <= c < first + length`). End-of-text
    // carets degrade to WholeSentence so the orchestrator does not try to
    // render a zero-width underline at the trailing edge of the last segment.
    if (caret >= plainTextLength) return ExampleLocation.WholeSentence
    offsets.withIndex().firstOrNull { (_, range) -> range.first == caret }?.let {
        return ExampleLocation.WithinSegment(segmentIndex = it.index, range = 0..0)
    }
    val container = offsets.withIndex().firstOrNull { (_, range) -> caret in range.first..range.last }
    return if (container != null) {
        val relative = caret - container.value.first
        ExampleLocation.WithinSegment(segmentIndex = container.index, range = relative..relative)
    } else {
        ExampleLocation.WholeSentence
    }
}
