package app.sensee.database

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<DatabaseTransactionRunner>(),
)
@Inject
public class DefaultDatabaseTransactionRunner(
    private val databaseProvider: SenseeDatabaseProvider,
) : DatabaseTransactionRunner {
    // One app-wide write serializer. Every write transaction — a standalone sense
    // upsert, a claim, a deck ingest — acquires this lock *before* opening the
    // SQLDelight transaction, so all writers share the single order lock-then-tx
    // and can never deadlock against each other.
    private val writeMutex = Mutex()

    override suspend fun <T> transaction(block: suspend () -> T): T {
        if (currentCoroutineContext()[WriteTransactionMarker] != null) {
            // Already inside a transaction on this coroutine (e.g. claim's sense
            // upsert): re-enter the held lock and enlist in the enclosing SQLDelight
            // transaction (a savepoint) instead of acquiring the lock again, which
            // would deadlock a non-reentrant mutex.
            return databaseProvider.database().transactionWithResult { block() }
        }
        return writeMutex.withLock {
            withContext(WriteTransactionMarker()) {
                databaseProvider.database().transactionWithResult { block() }
            }
        }
    }

    // Marks "this coroutine already holds the write lock and an open transaction",
    // so a nested transaction() enlists rather than re-locking.
    private class WriteTransactionMarker : AbstractCoroutineContextElement(WriteTransactionMarker) {
        companion object Key : CoroutineContext.Key<WriteTransactionMarker>
    }
}
