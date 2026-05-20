package app.sensee.tts.playback

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.platform.PlatformContext

/**
 * Provides a platform-bound [AudioPlayerFactory]. Implementations live in the
 * platform source sets so that consumers in commonMain can obtain a factory
 * through a single entry point. [logger] and [dispatchers] are used by players
 * with blocking work / internal diagnostics worth tracing (currently the JVM
 * player); other platforms accept them for parity.
 */
public expect fun audioPlayerFactory(
    context: PlatformContext,
    logger: AppLogger,
    dispatchers: AppDispatchers,
): AudioPlayerFactory
