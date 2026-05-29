package app.sensee.tts.integration

import app.sensee.core.coroutines.AppCoroutineScopes
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.network.NetworkLogger
import app.sensee.core.network.createRealHttpClientEngine
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.platform.PlatformContext
import app.sensee.database.SenseeDatabaseProvider
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.tts.cache.CachingSpeechSynthesizer
import app.sensee.tts.cache.SqlDelightAudioClipStoreFactory
import app.sensee.tts.core.AudioClipStore
import app.sensee.tts.core.Speaker
import app.sensee.tts.core.TtsCatalog
import app.sensee.tts.elevenlabs.api.ElevenLabsClientFactory
import app.sensee.tts.elevenlabs.config.ElevenLabsConfig
import app.sensee.tts.elevenlabs.speaker.ElevenLabsSpeaker
import app.sensee.tts.elevenlabs.synthesis.ElevenLabsSynthesizerFactory
import app.sensee.tts.openai.api.OpenAiSpeechClientFactory
import app.sensee.tts.openai.config.OpenAiSpeechConfig
import app.sensee.tts.openai.speaker.OpenAiSpeaker
import app.sensee.tts.openai.synthesis.OpenAiSynthesizerFactory
import app.sensee.tts.playback.AudioPlayerFactory
import app.sensee.tts.system.systemSpeaker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/**
 * Owns the TTS seam's assembly (mirrors `AiIntegrationProviders`): the real
 * HTTP engines, per-provider adapter chains and the disk cache are built here,
 * in the DI/composition layer, and bound as app singletons. The bound
 * [RoutingSpeaker]/[RoutingTtsCatalog] are pure logic that receive these as
 * collaborators — no class assembles its own network stack.
 */
@ContributesTo(AppScope::class)
public interface TtsIntegrationProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideAudioClipStore(
        databaseProvider: SenseeDatabaseProvider,
        dispatchers: AppDispatchers,
    ): AudioClipStore =
        SqlDelightAudioClipStoreFactory.create(
            databaseProvider = databaseProvider,
            dispatchers = dispatchers,
        )

    @SingleIn(AppScope::class)
    @Provides
    public fun provideElevenLabsConfig(): ElevenLabsConfig = ElevenLabsConfig()

    /**
     * The single ElevenLabs [HttpClient] for the app lifetime, shared by the
     * synthesizer and the catalog/key-verification path — same host and config.
     */
    @ElevenLabsHttpClient
    @SingleIn(AppScope::class)
    @Provides
    public fun provideElevenLabsHttpClient(
        config: ElevenLabsConfig,
        json: Json,
        logger: AppLogger,
    ): HttpClient =
        ElevenLabsClientFactory.create(
            engine = createRealHttpClientEngine(),
            config = config,
            json = json,
            logger = NetworkLogger { message -> logger.tag("TtsHttp").debug { message } },
        )

    @Suppress("ProfiledLongParameterList")
    @SingleIn(AppScope::class)
    @Provides
    public fun provideElevenLabsSpeaker(
        scopes: AppCoroutineScopes,
        playerFactory: AudioPlayerFactory,
        settings: UserSettingsRepository,
        store: AudioClipStore,
        @ElevenLabsHttpClient client: HttpClient,
        config: ElevenLabsConfig,
        logger: AppLogger,
    ): ElevenLabsSpeaker =
        ElevenLabsSpeaker(
            synthesizer =
                CachingSpeechSynthesizer(
                    delegate =
                        ElevenLabsSynthesizerFactory.create(
                            httpClient = client,
                            config = config,
                            credentials = {
                                settings
                                    .readSettings()
                                    .ai.ttsApiKey
                                    .orEmpty()
                            },
                        ),
                    store = store,
                    engineId = "elevenlabs",
                    logger = logger,
                ),
            playerFactory = playerFactory,
            scope = scopes.applicationScope,
            logger = logger,
        )

    @SingleIn(AppScope::class)
    @Provides
    public fun provideOpenAiSpeaker(
        scopes: AppCoroutineScopes,
        playerFactory: AudioPlayerFactory,
        settings: UserSettingsRepository,
        store: AudioClipStore,
        json: Json,
        logger: AppLogger,
    ): OpenAiSpeaker {
        val config = OpenAiSpeechConfig()
        val networkLogger = NetworkLogger { message -> logger.tag("TtsHttp").debug { message } }
        return OpenAiSpeaker(
            synthesizer =
                CachingSpeechSynthesizer(
                    delegate =
                        OpenAiSynthesizerFactory.create(
                            httpClient =
                                OpenAiSpeechClientFactory.create(
                                    engine = createRealHttpClientEngine(),
                                    config = config,
                                    json = json,
                                    logger = networkLogger,
                                ),
                            config = config,
                            credentials = {
                                settings
                                    .readSettings()
                                    .ai.ttsApiKey
                                    .orEmpty()
                            },
                        ),
                    store = store,
                    engineId = "openai:${config.model}",
                    logger = logger,
                ),
            playerFactory = playerFactory,
            scope = scopes.applicationScope,
            logger = logger,
        )
    }

    @SingleIn(AppScope::class)
    @Provides
    public fun provideSpeaker(
        elevenLabs: ElevenLabsSpeaker,
        openAi: OpenAiSpeaker,
        context: PlatformContext,
        scopes: AppCoroutineScopes,
        settings: UserSettingsRepository,
        logger: AppLogger,
    ): Speaker =
        RoutingSpeaker(
            elevenLabs = elevenLabs,
            openAi = openAi,
            // Trivial platform call — built inline here because the DI graph
            // already binds Speaker to the routed instance above.
            system = systemSpeaker(context = context, scope = scopes.applicationScope),
            settings = settings,
            scopes = scopes,
            logger = logger,
        )

    @SingleIn(AppScope::class)
    @Provides
    public fun provideTtsCatalog(
        @ElevenLabsHttpClient client: HttpClient,
        config: ElevenLabsConfig,
    ): TtsCatalog =
        RoutingTtsCatalog(
            httpClient = client,
            config = config,
        )
}

/**
 * Qualifies the real-network ElevenLabs [HttpClient], distinct from the
 * mock-backend [HttpClient] bound as the default in the app graph.
 */
@Qualifier
public annotation class ElevenLabsHttpClient
