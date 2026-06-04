package app.sensee.database

/**
 * Runs a block inside one aggregate-database transaction. A neutral seam (no
 * feature or SRS dependency) so cross-owner operations — `claim` composes a
 * lexicon write, an SRS snapshot clone and a deck membership — commit or roll
 * back together. Nested data-layer calls go through the same singleton
 * [SenseeDatabase] and enlist in this enclosing transaction rather than opening
 * their own.
 */
public interface DatabaseTransactionRunner {
    public suspend fun <T> transaction(block: suspend () -> T): T
}
