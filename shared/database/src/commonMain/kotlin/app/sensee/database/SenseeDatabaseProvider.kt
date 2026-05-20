package app.sensee.database

import app.sensee.core.observability.logging.AppLogger
import app.sensee.core.platform.PlatformEnvironment
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Lazily opens the single aggregate [SenseeDatabase]. A seam (not the concrete
 * class) so data-layer code can be tested against an in-memory database.
 */
public interface SenseeDatabaseProvider {
    public suspend fun database(): SenseeDatabase
}

@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<SenseeDatabaseProvider>(),
)
@Inject
public class DefaultSenseeDatabaseProvider(
    private val platformEnvironment: PlatformEnvironment,
    private val config: DatabaseConfig,
    private val logger: AppLogger,
) : SenseeDatabaseProvider {
    private val mutex = Mutex()
    private var database: SenseeDatabase? = null

    override suspend fun database(): SenseeDatabase =
        database
            ?: mutex.withLock {
                database
                    ?: createDatabase(
                        driverFactory = DriverFactory(platformEnvironment),
                        config = config,
                        logger = logger.tag("SenseeDatabase"),
                    ).also { database = it }
            }
}
