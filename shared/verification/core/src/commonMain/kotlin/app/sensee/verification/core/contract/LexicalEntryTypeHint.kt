package app.sensee.verification.core.contract

import kotlinx.serialization.Serializable

/**
 * Neutral entry-type carried as a wire id. The feature boundary resolves
 * this to its own taxonomy; an id the feature does not know yet stays as a
 * raw string and the consumer renders it as best it can rather than dropping
 * the suggestion (ADR-006 forward-compat).
 *
 * Well-known ids: `word`, `phrasal_verb`, `idiom`, `collocation`,
 * `grammar_pattern`, `phrase`.
 */
@Serializable
public data class LexicalEntryTypeHint(
    val id: String,
)
