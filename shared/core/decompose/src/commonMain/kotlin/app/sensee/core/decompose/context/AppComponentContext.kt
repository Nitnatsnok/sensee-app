package app.sensee.core.decompose.context

import app.sensee.core.decompose.navigation.ChainedNavigationDispatcher
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.result.ChainedComponentResultDispatcher
import app.sensee.core.decompose.result.ComponentResultDispatcher
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.GenericComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.Lifecycle
import kotlinx.serialization.KSerializer

public interface NavigationContext {
    public val navigation: NavigationDispatcher
}

public interface ComponentResultContext {
    public val results: ComponentResultDispatcher
}

public interface ScreenConfigSerializationContext {
    public val screenConfigSerializer: KSerializer<ScreenConfig>
}

public interface AppComponentContext :
    GenericComponentContext<AppComponentContext>,
    AdaptivePresentationContext,
    NavigationContext,
    ComponentResultContext,
    ScreenConfigSerializationContext

/**
 * Create a decorator context (not child) with overridden component capabilities.
 */
public fun AppComponentContext.withComponentContextOverride(
    navigation: NavigationDispatcher? = null,
    results: ComponentResultDispatcher? = null,
): AppComponentContext =
    DecoratedAppComponentContext(
        delegate = this,
        navigation =
            navigation?.let { local ->
                ChainedNavigationDispatcher(
                    local = local,
                    parent = this.navigation,
                )
            } ?: this.navigation,
        results =
            results?.let { local ->
                ChainedComponentResultDispatcher(
                    local = local,
                    parent = this.results,
                )
            } ?: this.results,
    )

/**
 * Create a child context with overridden component capabilities.
 */
@OptIn(ExperimentalDecomposeApi::class)
public fun AppComponentContext.createChildContext(
    key: String,
    lifecycle: Lifecycle? = null,
    backHandlerPriority: Int = BackCallback.PRIORITY_DEFAULT,
    navigation: NavigationDispatcher? = null,
    results: ComponentResultDispatcher? = null,
): AppComponentContext {
    val child =
        childContext(
            key = key,
            lifecycle = lifecycle,
            backHandlerPriority = backHandlerPriority,
        )
    return child.withComponentContextOverride(
        navigation = navigation,
        results = results,
    )
}
