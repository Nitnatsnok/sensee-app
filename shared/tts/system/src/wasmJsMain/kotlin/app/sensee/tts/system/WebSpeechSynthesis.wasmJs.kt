@file:OptIn(ExperimentalWasmJsInterop::class)

package app.sensee.tts.system

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.unsafeCast

private external interface SpeechSynthesis : JsAny {
    fun speak(utterance: SpeechSynthesisUtterance)

    fun cancel()
}

private external interface SpeechSynthesisUtterance : JsAny {
    var lang: String
    var rate: Double

    fun addEventListener(
        type: String,
        listener: (JsAny?) -> Unit,
    )
}

private fun speechSynthesisOrNullValue(): JsAny? =
    // Keep nullable: window or speechSynthesis can be unavailable in non-browser/test contexts.
    js("(typeof window !== 'undefined' && window.speechSynthesis) ? window.speechSynthesis : null")

private fun speechSynthesisOrNull(): SpeechSynthesis? = speechSynthesisOrNullValue()?.unsafeCast<SpeechSynthesis>()

private fun newUtterance(
    @Suppress("UNUSED_PARAMETER") text: String,
): SpeechSynthesisUtterance = js("new SpeechSynthesisUtterance(text)")

private fun speechErrorCode(
    @Suppress("UNUSED_PARAMETER") event: JsAny?,
): String? =
    // Keep nullable: the Web Speech error event may be absent or lack an error code.
    js("(event && event.error) ? String(event.error) : null")

internal actual fun webSpeechSynthesisOrNull(): WebSpeechSynthesis? =
    speechSynthesisOrNull()?.let(::WasmWebSpeechSynthesis)

private class WasmWebSpeechSynthesis(
    private val delegate: SpeechSynthesis,
) : WebSpeechSynthesis {
    override fun createUtterance(text: String): WebSpeechUtterance = WasmWebSpeechUtterance(newUtterance(text))

    override fun speak(utterance: WebSpeechUtterance) {
        delegate.speak((utterance as WasmWebSpeechUtterance).delegate)
    }

    override fun cancel() {
        delegate.cancel()
    }
}

private class WasmWebSpeechUtterance(
    val delegate: SpeechSynthesisUtterance,
) : WebSpeechUtterance {
    override var lang: String
        get() = delegate.lang
        set(value) {
            delegate.lang = value
        }

    override var rate: Double
        get() = delegate.rate
        set(value) {
            delegate.rate = value
        }

    override fun onStart(listener: () -> Unit) {
        delegate.addEventListener("start") { listener() }
    }

    override fun onEnd(listener: () -> Unit) {
        delegate.addEventListener("end") { listener() }
    }

    override fun onError(listener: (String?) -> Unit) {
        delegate.addEventListener("error") { event -> listener(speechErrorCode(event)) }
    }
}
