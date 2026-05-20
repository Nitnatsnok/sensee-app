package app.sensee.grammar.domain

/**
 * The surface form of a sense as a structured token list, never a single
 * string (ADR-001). Identity of a sense includes its surface form:
 * `come across`, `come across [as]`, `come across <something>` are distinct
 * senses, so the optional particle and the argument slot are modelled, not
 * formatted into text. Rendering (italic slot, etc.) is a UI concern;
 * [display] is the neutral plain-text projection.
 */
public data class SurfaceForm(
    val tokens: List<SurfaceToken>,
) {
    init {
        require(tokens.isNotEmpty()) { "SurfaceForm needs at least one token" }
    }

    public companion object {
        /**
         * Parses a textual surface form into structured tokens: a `[word]`
         * becomes [SurfaceToken.Optional], a `<word>` becomes [SurfaceToken.Slot],
         * everything else coalesces into [SurfaceToken.Literal] runs. The AI
         * boundary uses this so the seam stays string-based while the domain
         * stays structured (ADR-001).
         */
        public fun parse(text: String): SurfaceForm {
            val tokens = mutableListOf<SurfaceToken>()
            val literal = StringBuilder()

            fun flushLiteral() {
                if (literal.isNotBlank()) tokens.add(SurfaceToken.Literal(literal.toString().trim()))
                literal.clear()
            }
            text.trim().split(' ').filter { it.isNotBlank() }.forEach { word ->
                when {
                    word.startsWith("[") && word.endsWith("]") -> {
                        flushLiteral()
                        tokens.add(SurfaceToken.Optional(word.removeSurrounding("[", "]")))
                    }
                    word.startsWith("<") && word.endsWith(">") -> {
                        flushLiteral()
                        tokens.add(SurfaceToken.Slot(word.removeSurrounding("<", ">")))
                    }
                    else -> literal.append(if (literal.isEmpty()) word else " $word")
                }
            }
            flushLiteral()
            require(tokens.isNotEmpty()) { "Cannot parse an empty surface form" }
            return SurfaceForm(tokens)
        }
    }

    public fun display(): String =
        tokens.joinToString(" ") { token ->
            when (token) {
                is SurfaceToken.Literal -> token.text
                is SurfaceToken.Optional -> "[${token.text}]"
                is SurfaceToken.Slot -> "<${token.name}>"
            }
        }
}

public sealed interface SurfaceToken {
    /** A fixed part of the form, e.g. `come across`. */
    public data class Literal(
        val text: String,
    ) : SurfaceToken

    /** An optional particle, e.g. the `as` in `come across [as]`. */
    public data class Optional(
        val text: String,
    ) : SurfaceToken

    /** An argument placeholder, e.g. the `<something>` in `come across <something>`. */
    public data class Slot(
        val name: String,
    ) : SurfaceToken
}
