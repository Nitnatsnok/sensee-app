package app.sensee.tts.playback

import app.sensee.tts.core.AudioFormat

/**
 * Media type for a synthesized [AudioFormat], used by the web players to build
 * the `data:` URL fed into an `HTMLAudioElement`. Providers emit MP3 in
 * practice; the other entries keep the mapping total.
 */
internal fun audioMimeType(format: AudioFormat): String =
    when (format) {
        AudioFormat.Mp3 -> "audio/mpeg"
        AudioFormat.Wav -> "audio/wav"
        AudioFormat.Pcm16 -> "audio/wave"
    }
