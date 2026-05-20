package app.sensee.tts.system

private fun speechSynthesisOrNull(): dynamic =
    js("(typeof window !== 'undefined' && window.speechSynthesis) ? window.speechSynthesis : null")

private fun newUtterance(
    @Suppress("UNUSED_PARAMETER") text: String,
): dynamic = js("new SpeechSynthesisUtterance(text)")

internal actual fun webSpeechSynthesisOrNull(): WebSpeechSynthesis? {
    val synthesis = speechSynthesisOrNull() ?: return null
    return JsWebSpeechSynthesis(synthesis)
}

private class JsWebSpeechSynthesis(
    private val delegate: dynamic,
) : WebSpeechSynthesis {
    override fun createUtterance(text: String): WebSpeechUtterance = JsWebSpeechUtterance(newUtterance(text))

    override fun speak(utterance: WebSpeechUtterance) {
        delegate.speak((utterance as JsWebSpeechUtterance).delegate)
    }

    override fun cancel() {
        delegate.cancel()
    }
}

private class JsWebSpeechUtterance(
    internal val delegate: dynamic,
) : WebSpeechUtterance {
    override var lang: String
        get() = delegate.lang as String
        set(value) {
            delegate.lang = value
        }

    override var rate: Double
        get() = delegate.rate as Double
        set(value) {
            delegate.rate = value
        }

    override fun onStart(listener: () -> Unit) {
        delegate.addEventListener("start", { listener() })
    }

    override fun onEnd(listener: () -> Unit) {
        delegate.addEventListener("end", { listener() })
    }

    override fun onError(listener: (String?) -> Unit) {
        delegate.addEventListener(
            "error",
            { event: dynamic -> listener(event?.error as? String) },
        )
    }
}
