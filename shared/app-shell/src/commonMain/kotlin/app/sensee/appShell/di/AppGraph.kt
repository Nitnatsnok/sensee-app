package app.sensee.appShell.di

import app.sensee.appShell.root.AppRootFactory
import app.sensee.core.platform.PlatformEnvironment
import app.sensee.database.DatabaseConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph(AppScope::class)
public interface AppGraph {
    public val appRootFactory: AppRootFactory

    @DependencyGraph.Factory
    public fun interface Factory {
        public fun create(
            @Provides platformEnvironment: PlatformEnvironment,
            @Provides databaseConfig: DatabaseConfig,
        ): AppGraph
    }
}

public fun createAppGraph(
    platformEnvironment: PlatformEnvironment,
    databaseConfig: DatabaseConfig,
): AppGraph =
    createGraphFactory<AppGraph.Factory>()
        .create(platformEnvironment, databaseConfig)
