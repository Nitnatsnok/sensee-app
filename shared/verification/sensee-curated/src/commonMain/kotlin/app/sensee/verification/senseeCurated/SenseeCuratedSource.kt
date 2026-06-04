package app.sensee.verification.senseeCurated

import app.sensee.verification.core.contract.AttributionPolicy
import app.sensee.verification.core.contract.LexicalSource
import app.sensee.verification.core.contract.LicensePolicy

/**
 * Source attribution for Sensee-curated lexical reference data (frequency,
 * CEFR level, sense inventory, word family). Hand-authored and licence-clean,
 * served at verification time by the (mock) backend over HTTP. The real backend
 * will serve the same data under the same source identity.
 */
public object SenseeCuratedSource {
    public const val ID: String = "sensee-curated"

    public val descriptor: LexicalSource =
        LexicalSource.Adapter(
            id = ID,
            displayName = "Sensee curated reference",
            attribution =
                AttributionPolicy(
                    required = false,
                    displayString = "Sensee-curated lexical reference",
                ),
            license =
                LicensePolicy(
                    storeContentAllowed = true,
                    maxCacheTtlMillis = null,
                    usableAsLlmContext = true,
                ),
            versionTag = "v0-curated",
        )
}
