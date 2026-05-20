package app.sensee.core.decompose.context

import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.result.ComponentResultDispatcher
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.children.NavigationSource
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.router.panels.Panels
import com.arkivanov.decompose.router.panels.PanelsNavigation
import com.arkivanov.decompose.router.panels.childPanels
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer

@OptIn(ExperimentalDecomposeApi::class, ExperimentalSerializationApi::class)
public fun <MC : Any, MT : Any, DC : Any, DT : Any> AppComponentContext.appChildPanels(
    source: NavigationSource<PanelsNavigation.Event<MC, DC, Nothing>>,
    serializers: Pair<KSerializer<MC>, KSerializer<DC>>?,
    initialPanels: () -> Panels<MC, DC, Nothing>,
    key: String = "DefaultChildPanels",
    handleBackButton: Boolean = false,
    navigation: NavigationDispatcher? = null,
    results: ComponentResultDispatcher? = null,
    mainFactory: (configuration: MC, AppComponentContext) -> MT,
    detailsFactory: (configuration: DC, AppComponentContext) -> DT,
): Value<ChildPanels<MC, MT, DC, DT, Nothing, Nothing>> =
    childPanels(
        source = source,
        serializers = serializers,
        initialPanels = initialPanels,
        key = key,
        handleBackButton = handleBackButton,
        mainFactory = { configuration, componentContext ->
            mainFactory(
                configuration,
                componentContext.withComponentCapabilities(
                    navigation = navigation,
                    results = results,
                ),
            )
        },
        detailsFactory = { configuration, componentContext ->
            detailsFactory(
                configuration,
                componentContext.withComponentCapabilities(
                    navigation = navigation,
                    results = results,
                ),
            )
        },
    )

private fun AppComponentContext.withComponentCapabilities(
    navigation: NavigationDispatcher?,
    results: ComponentResultDispatcher?,
): AppComponentContext {
    if (navigation == null && results == null) {
        return this
    }

    return withComponentContextOverride(
        navigation = navigation,
        results = results,
    )
}
