package app.sensee.core.decompose.context

import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.result.ComponentResultDispatcher
import com.arkivanov.decompose.ComponentContextFactory
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.instancekeeper.InstanceKeeperOwner
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import com.arkivanov.essenty.statekeeper.StateKeeperOwner

internal class DecoratedAppComponentContext(
    private val delegate: AppComponentContext,
    override val navigation: NavigationDispatcher,
    override val results: ComponentResultDispatcher,
) : AppComponentContext,
    LifecycleOwner by delegate,
    StateKeeperOwner by delegate,
    InstanceKeeperOwner by delegate,
    BackHandlerOwner by delegate {
    override val screenConfigSerializer = delegate.screenConfigSerializer
    override val contentPresentation: Value<AppContentPresentation> = delegate.contentPresentation

    override val componentContextFactory: ComponentContextFactory<AppComponentContext> =
        ComponentContextFactory { lifecycle, stateKeeper, instanceKeeper, backHandler ->
            val childDelegate =
                delegate.componentContextFactory(
                    lifecycle = lifecycle,
                    stateKeeper = stateKeeper,
                    instanceKeeper = instanceKeeper,
                    backHandler = backHandler,
                )

            DecoratedAppComponentContext(
                delegate = childDelegate,
                navigation = navigation,
                results = results,
            )
        }
}
