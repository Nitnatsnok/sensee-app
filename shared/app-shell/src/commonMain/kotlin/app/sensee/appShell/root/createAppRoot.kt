package app.sensee.appShell.root

import app.sensee.appShell.di.createAppGraph
import app.sensee.core.platform.PlatformEnvironment
import app.sensee.database.DatabaseConfig
import com.arkivanov.decompose.ComponentContext

public fun createAppRoot(
    componentContext: ComponentContext,
    platformEnvironment: PlatformEnvironment,
    deepLink: String? = null,
    databaseConfig: DatabaseConfig = DatabaseConfig(),
): RootComponent {
    val appGraph =
        createAppGraph(
            platformEnvironment = platformEnvironment,
            databaseConfig = databaseConfig,
        )
    return appGraph.appRootFactory.create(
        componentContext = componentContext,
        deepLink = deepLink,
    )
}
