package app.sensee.verification.integration

import app.sensee.core.network.NetworkConfig
import app.sensee.core.network.NetworkHttpClientFactory
import app.sensee.core.network.createRealHttpClientEngine
import app.sensee.verification.core.CefrLevelProvider
import app.sensee.verification.core.ExampleQualityChecker
import app.sensee.verification.core.FrequencyProvider
import app.sensee.verification.core.LexicalEntryLookup
import app.sensee.verification.core.LexicalFamilyProvider
import app.sensee.verification.core.LexicalSource
import app.sensee.verification.core.SenseInventoryProvider
import app.sensee.verification.datamuse.DatamuseLexicalAdapter
import app.sensee.verification.datamuse.DatamuseSource
import app.sensee.verification.freeDictionary.FreeDictionaryLexicalAdapter
import app.sensee.verification.freeDictionary.FreeDictionarySource
import app.sensee.verification.languagetool.LanguageToolExampleChecker
import app.sensee.verification.languagetool.LanguageToolSource
import app.sensee.verification.senseeCurated.SenseeCefrLevelProvider
import app.sensee.verification.senseeCurated.SenseeCuratedSource
import app.sensee.verification.senseeCurated.SenseeFrequencyProvider
import app.sensee.verification.senseeCurated.SenseeLexicalFamilyProvider
import app.sensee.verification.senseeCurated.SenseeSenseInventoryProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/**
 * App-graph providers for the verification seam. Adapters compose via
 * Set-injection so the routing aggregator picks them up without code change.
 * Existence / part-of-speech come from the third-party network adapters
 * (Free Dictionary, Datamuse); frequency / CEFR / sense inventory / family are
 * served by the Sensee-curated backend over HTTP (mock backend in this build,
 * a real backend later). Example quality comes from LanguageTool. Each network
 * adapter owns its own `HttpClient` to isolate failure modes.
 */
@ContributesTo(AppScope::class)
public interface VerificationIntegrationProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideFreeDictionaryAdapter(json: Json): FreeDictionaryLexicalAdapter =
        FreeDictionaryLexicalAdapter(
            httpClient = verificationHttpClient(FreeDictionaryLexicalAdapter.DEFAULT_BASE_URL, json),
        )

    @SingleIn(AppScope::class)
    @Provides
    public fun provideLanguageToolChecker(json: Json): LanguageToolExampleChecker =
        LanguageToolExampleChecker(
            httpClient = verificationHttpClient(LanguageToolExampleChecker.DEFAULT_BASE_URL, json),
        )

    @SingleIn(AppScope::class)
    @Provides
    public fun provideExampleQualityCheckers(languageTool: LanguageToolExampleChecker): Set<ExampleQualityChecker> =
        setOf(languageTool)

    @SingleIn(AppScope::class)
    @Provides
    public fun provideDatamuseAdapter(json: Json): DatamuseLexicalAdapter =
        DatamuseLexicalAdapter(
            httpClient = verificationHttpClient(DatamuseLexicalAdapter.DEFAULT_BASE_URL, json),
        )

    @SingleIn(AppScope::class)
    @Provides
    public fun provideEntryLookups(
        freeDictionary: FreeDictionaryLexicalAdapter,
        datamuse: DatamuseLexicalAdapter,
    ): Set<LexicalEntryLookup> = setOf(freeDictionary, datamuse)

    @SingleIn(AppScope::class)
    @Provides
    public fun provideFrequencyProviders(sensee: SenseeFrequencyProvider): Set<FrequencyProvider> = setOf(sensee)

    @SingleIn(AppScope::class)
    @Provides
    public fun provideCefrProviders(sensee: SenseeCefrLevelProvider): Set<CefrLevelProvider> = setOf(sensee)

    @SingleIn(AppScope::class)
    @Provides
    public fun provideFamilyProviders(sensee: SenseeLexicalFamilyProvider): Set<LexicalFamilyProvider> = setOf(sensee)

    @SingleIn(AppScope::class)
    @Provides
    public fun provideSenseInventoryProviders(sensee: SenseeSenseInventoryProvider): Set<SenseInventoryProvider> =
        setOf(sensee)

    /** Every source whose `LexicalSourceRef` can appear in a report must be listed here. */
    @SingleIn(AppScope::class)
    @Provides
    public fun provideKnownLexicalSources(): Set<LexicalSource> =
        setOf(
            FreeDictionarySource.descriptor,
            DatamuseSource.descriptor,
            LanguageToolSource.descriptor,
            SenseeCuratedSource.descriptor,
        )
}

private const val REQUEST_TIMEOUT_MILLIS: Long = 4_000L

private fun verificationHttpClient(
    baseUrl: String,
    json: Json,
): HttpClient =
    NetworkHttpClientFactory.create(
        engine = createRealHttpClientEngine(),
        config =
            NetworkConfig(
                baseUrl = baseUrl,
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS,
            ),
        json = json,
    )
