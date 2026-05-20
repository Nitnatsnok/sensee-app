package app.sensee.tts.core

/**
 * Plays speech for a [SpeechRequest] on the current device.
 *
 * Implementations differ by how the audio is produced — the system TTS engine
 * speaks directly, while a network-backed synthesizer fetches audio bytes and
 * routes them through an [app.sensee.tts.playback.AudioPlayer]. Callers do not
 * need to distinguish the two.
 */
public interface Speaker {
    public fun speak(request: SpeechRequest): SpeechHandle

    public fun stop()
}
