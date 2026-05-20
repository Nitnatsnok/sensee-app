package app.sensee.srs.core.id

import kotlin.jvm.JvmInline

@JvmInline
public value class SrsScope(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "SrsScope must not be blank"
        }
    }

    override fun toString(): String = value

    public companion object {
        public val Default: SrsScope = SrsScope("default")
    }
}
