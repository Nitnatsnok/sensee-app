package app.sensee.srs.core.id

import kotlin.jvm.JvmInline

@JvmInline
public value class SrsCardId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "SrsCardId must not be blank"
        }
    }

    override fun toString(): String = value
}
