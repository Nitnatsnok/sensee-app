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

/**
 * Three-pane variant: same factory shape as [appChildPanels] above, plus an
 * [extraFactory] that builds the third pane's child for callers using the
 * supporting-pane layout (rendered inline by the three-pane [AppChildPanels] on
 * width ≥ Large, and as a caller-controlled overlay on narrower layouts).
 *
 * Six factory/source arguments exceed the regular-function budget; the three
 * factories mirror Decompose's own `childPanels` signature, so collapsing them
 * into a config object would just obscure the parallel.
 */
@Suppress("ProfiledLongParameterList")
@OptIn(ExperimentalDecomposeApi::class, ExperimentalSerializationApi::class)
public fun <MC : Any, MT : Any, DC : Any, DT : Any, EC : Any, ET : Any> AppComponentContext.appChildPanels(
    source: NavigationSource<PanelsNavigation.Event<MC, DC, EC>>,
    serializers: Triple<KSerializer<MC>, KSerializer<DC>, KSerializer<EC>>?,
    initialPanels: () -> Panels<MC, DC, EC>,
    key: String = "DefaultChildPanels",
    handleBackButton: Boolean = false,
    navigation: NavigationDispatcher? = null,
    results: ComponentResultDispatcher? = null,
    mainFactory: (configuration: MC, AppComponentContext) -> MT,
    detailsFactory: (configuration: DC, AppComponentContext) -> DT,
    extraFactory: (configuration: EC, AppComponentContext) -> ET,
): Value<ChildPanels<MC, MT, DC, DT, EC, ET>> =
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
        extraFactory = { configuration, componentContext ->
            extraFactory(
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
