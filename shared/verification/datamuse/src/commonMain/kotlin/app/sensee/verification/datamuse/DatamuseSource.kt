package app.sensee.verification.datamuse

import app.sensee.verification.core.AttributionPolicy
import app.sensee.verification.core.LexicalSource
import app.sensee.verification.core.LicensePolicy

/**
 * Datamuse word-finding API (https://www.datamuse.com/api/). Free, no API
 * key, English-only. The adapter uses `/words?sp=<term>` to surface a
 * candidate normalization (spelling fix) when the user typed an unindexed
 * form. The aggregator pairs that signal with a primary dictionary's
 * existence answer — Datamuse on its own is not authoritative for "this
 * word does not exist".
 */
public object DatamuseSource {
    public const val ID: String = "datamuse"

    public val descriptor: LexicalSource =
        LexicalSource.Adapter(
            id = ID,
            displayName = "Datamuse word-finding API",
            attribution =
                AttributionPolicy(
                    required = true,
                    displayString = "Spelling suggestions via Datamuse",
                    mustLinkBack = true,
                ),
            license =
                LicensePolicy(
                    storeContentAllowed = true,
                    maxCacheTtlMillis = null,
                    usableAsLlmContext = true,
                    noteIfAny = "Free API; the cached signal is a factual spelling token.",
                ),
            versionTag = "v1",
        )
}
