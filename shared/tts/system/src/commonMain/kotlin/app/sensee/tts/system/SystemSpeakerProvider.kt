package app.sensee.tts.system

import app.sensee.core.platform.PlatformContext
import app.sensee.tts.core.Speaker
import kotlinx.coroutines.CoroutineScope

/**
 * Builds a [Speaker] backed by the platform's built-in text-to-speech engine.
 *
 * The returned instance is meant to be held by the application graph as a
 * singleton — engine initialization is expensive on some platforms (notably
 * Android's [android.speech.tts.TextToSpeech]).
 *
 * The [scope] is used to bridge async init/completion callbacks; pass a scope
 * tied to the application lifetime.
 */
public expect fun systemSpeaker(
    context: PlatformContext,
    scope: CoroutineScope,
): Speaker
