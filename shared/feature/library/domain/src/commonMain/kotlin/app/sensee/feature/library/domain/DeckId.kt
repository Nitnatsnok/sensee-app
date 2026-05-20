package app.sensee.feature.library.domain

import kotlin.jvm.JvmInline

@JvmInline
public value class DeckId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "DeckId must not be blank"
        }
    }

    override fun toString(): String = value
}
