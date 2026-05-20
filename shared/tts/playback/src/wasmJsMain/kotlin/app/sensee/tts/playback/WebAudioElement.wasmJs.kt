@file:OptIn(ExperimentalWasmJsInterop::class)

package app.sensee.tts.playback

import kotlin.js.ExperimentalWasmJsInterop

private external interface JsAudioElement : JsAny {
    var src: String
    var playbackRate: Double

    fun play()

    fun pause()

    fun addEventListener(
        type: String,
        listener: (JsAny?) -> Unit,
    )
}

private fun newAudioElement(): JsAudioElement = js("new Audio()")

internal actual fun newWebAudioElement(): WebAudioElement = WasmJsWebAudioElement(newAudioElement())

private class WasmJsWebAudioElement(
    private val delegate: JsAudioElement,
) : WebAudioElement {
    override var src: String
        get() = delegate.src
        set(value) {
            delegate.src = value
        }

    override var playbackRate: Double
        get() = delegate.playbackRate
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
        delegate.addEventListener("ended") { listener() }
    }

    override fun onError(listener: () -> Unit) {
        delegate.addEventListener("error") { listener() }
    }
}
