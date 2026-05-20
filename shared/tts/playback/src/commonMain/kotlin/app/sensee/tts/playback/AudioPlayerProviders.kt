package app.sensee.tts.playback

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.platform.PlatformContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
public interface AudioPlayerProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideAudioPlayerFactory(
        context: PlatformContext,
        logger: AppLogger,
        dispatchers: AppDispatchers,
    ): AudioPlayerFactory = audioPlayerFactory(context = context, logger = logger, dispatchers = dispatchers)
}
