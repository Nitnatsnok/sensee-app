package app.sensee.tts.playback

private fun newAudioElement(): dynamic = js("new Audio()")

internal actual fun newWebAudioElement(): WebAudioElement = JsWebAudioElement(newAudioElement())

private class JsWebAudioElement(
    private val delegate: dynamic,
) : WebAudioElement {
    override var src: String
        get() = delegate.src as String
        set(value) {
            delegate.src = value
        }

    override var playbackRate: Double
        get() = delegate.playbackRate as Double
        set(value) {
            delegate.playbackRate = value
        }

    override fun play() {
        delegate.play()
    }

    override fun pause() {
        delegate.pause()
    }

    override fun onEnded(listener: () -> Unit) {
        delegate.addEventListener("ended", { listener() })
    }

    override fun onError(listener: () -> Unit) {
        delegate.addEventListener("error", { listener() })
    }
}
