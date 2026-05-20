package app.sensee.appShell.root

import app.sensee.core.decompose.context.RootComponentContext
import com.arkivanov.decompose.ComponentContext
import dev.zacsweers.metro.Inject

@Inject
public class AppRootFactory(
    private val rootComponentFactory: RootComponent.Factory,
    private val rootNavigationSerializerProvider: RootNavigationSerializerProvider,
) {
    public fun create(
        componentContext: ComponentContext,
        deepLink: String? = null,
    ): RootComponent {
        val appComponentContext =
            RootComponentContext(
                delegate = componentContext,
                screenConfigSerializer = rootNavigationSerializerProvider.configSerializer,
            )

        return rootComponentFactory.create(
            componentContext = appComponentContext,
            deepLink = deepLink,
        )
    }
}
