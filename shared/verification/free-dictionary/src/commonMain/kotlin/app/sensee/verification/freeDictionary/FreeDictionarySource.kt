package app.sensee.verification.freeDictionary

import app.sensee.verification.core.contract.AttributionPolicy
import app.sensee.verification.core.contract.LexicalSource
import app.sensee.verification.core.contract.LicensePolicy

/**
 * Source attribution and license for entries served by dictionaryapi.dev.
 * The API wraps Wiktionary (CC-BY-SA) + WordNet (BSD-style). `usableAsLlmContext`
 * is `true`: passing a short headword/POS/IPA hint to a third-party LLM is fair
 * use of the public lookup. `storeContentAllowed` is `true`: a private on-device
 * cache of CC-BY-SA content is permitted (caching is not redistribution, and
 * attribution rides on display) and spares the free API repeat lookups.
 */
public object FreeDictionarySource {
    public const val ID: String = "free-dictionary"

    public val descriptor: LexicalSource =
        LexicalSource.Adapter(
            id = ID,
            displayName = "Free Dictionary API (dictionaryapi.dev)",
            attribution =
                AttributionPolicy(
                    required = true,
                    displayString = "Definitions via Free Dictionary API (Wiktionary, WordNet)",
                    mustLinkBack = true,
                ),
            license =
                LicensePolicy(
                    storeContentAllowed = true,
                    maxCacheTtlMillis = null,
                    usableAsLlmContext = true,
                    noteIfAny = "Underlying content is CC-BY-SA (Wiktionary); attribute on display.",
                ),
            versionTag = "v2",
        )
}
