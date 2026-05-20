package app.sensee.srs.core.id

import kotlin.jvm.JvmInline

@JvmInline
public value class SrsParametersId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "SrsParametersId must not be blank"
        }
    }

    override fun toString(): String = value
}
