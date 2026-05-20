package app.sensee.feature.library.domain

import kotlin.jvm.JvmInline

@JvmInline
public value class LemmaId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "LemmaId must not be blank"
        }
    }

    override fun toString(): String = value
}
