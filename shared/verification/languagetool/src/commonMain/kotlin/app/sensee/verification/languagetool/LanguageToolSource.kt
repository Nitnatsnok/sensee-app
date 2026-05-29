package app.sensee.verification.languagetool

import app.sensee.verification.core.AttributionPolicy
import app.sensee.verification.core.LexicalSource
import app.sensee.verification.core.LicensePolicy

/**
 * LanguageTool grammar/style checker (https://languagetool.org). Supports a
 * free public endpoint plus self-hosted deployment. The adapter sends study
 * example sentences for analysis — content the user did not author but the
 * AI generated for them — so it is treated as content suitable for an LLM
 * context surface. Self-hosting eliminates the third-party exposure
 * entirely; the user can flip the privacy flag to `false` to disable both.
 */
public object LanguageToolSource {
    public const val ID: String = "languagetool"

    public val descriptor: LexicalSource =
        LexicalSource.Adapter(
            id = ID,
            displayName = "LanguageTool grammar checker",
            attribution =
                AttributionPolicy(
                    required = true,
                    displayString = "Grammar checks via LanguageTool",
                    mustLinkBack = true,
                ),
            license =
                LicensePolicy(
                    storeContentAllowed = false,
                    maxCacheTtlMillis = null,
                    usableAsLlmContext = true,
                    noteIfAny = "LGPL-2.1+ tool; public API rate-limited — self-host for production volume.",
                ),
            versionTag = "v2",
        )
}
