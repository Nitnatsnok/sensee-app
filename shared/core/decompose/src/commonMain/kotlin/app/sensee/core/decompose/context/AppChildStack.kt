package app.sensee.core.decompose.context

import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.result.ComponentResultDispatcher
import com.arkivanov.decompose.router.children.NavigationSource
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.KSerializer

public fun <C : Any, T : Any> AppComponentContext.appChildStack(
    source: NavigationSource<StackNavigation.Event<C>>,
    serializer: KSerializer<C>?,
    initialConfiguration: C,
    key: String = "DefaultChildStack",
    handleBackButton: Boolean = false,
    navigation: NavigationDispatcher? = null,
    results: ComponentResultDispatcher? = null,
    childFactory: (configuration: C, AppComponentContext) -> T,
): Value<ChildStack<C, T>> =
    childStack(
        source = source,
        serializer = serializer,
        initialConfiguration = initialConfiguration,
        key = key,
        handleBackButton = handleBackButton,
        childFactory = { configuration, componentContext ->
            childFactory(
                configuration,
                componentContext.withComponentCapabilities(
                    navigation = navigation,
                    results = results,
                ),
            )
        },
    )

public fun <C : Any, T : Any> AppComponentContext.appChildStack(
    source: NavigationSource<StackNavigation.Event<C>>,
    serializer: KSerializer<C>?,
    initialStack: () -> List<C>,
    key: String = "DefaultChildStack",
    handleBackButton: Boolean = false,
    navigation: NavigationDispatcher? = null,
    results: ComponentResultDispatcher? = null,
    childFactory: (configuration: C, AppComponentContext) -> T,
): Value<ChildStack<C, T>> =
    childStack(
        source = source,
        serializer = serializer,
        initialStack = initialStack,
        key = key,
        handleBackButton = handleBackButton,
        childFactory = { configuration, componentContext ->
            childFactory(
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
