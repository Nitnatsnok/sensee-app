package app.sensee.srs.engine.storage

public interface SrsTransactionRunner {
    public suspend fun <T> transaction(block: suspend () -> T): T
}
