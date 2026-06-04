package app.sensee.database

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<DatabaseTransactionRunner>(),
)
@Inject
public class DefaultDatabaseTransactionRunner(
    private val databaseProvider: SenseeDatabaseProvider,
) : DatabaseTransactionRunner {
    override suspend fun <T> transaction(block: suspend () -> T): T =
        databaseProvider.database().transactionWithResult { block() }
}
