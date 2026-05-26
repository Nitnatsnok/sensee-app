package app.sensee.core.decompose.context

import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.navigation.NoOpNavigationDispatcher
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.result.ComponentResultDispatcher
import app.sensee.core.decompose.result.NoOpComponentResultDispatcher
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ComponentContextFactory
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.instancekeeper.InstanceKeeperOwner
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import com.arkivanov.essenty.statekeeper.StateKeeperOwner
import kotlinx.serialization.KSerializer

public class RootComponentContext(
    private val delegate: ComponentContext,
    override val screenConfigSerializer: KSerializer<ScreenConfig>,
    override val navigation: NavigationDispatcher = NoOpNavigationDispatcher,
    override val results: ComponentResultDispatcher = NoOpComponentResultDispatcher,
    private val contentPresentationState: MutableValue<AppContentPresentation> =
        MutableValue(AppContentPresentation.SinglePane),
) : AppComponentContext,
    MutableAdaptivePresentationContext,
    LifecycleOwner by delegate,
    StateKeeperOwner by delegate,
    InstanceKeeperOwner by delegate,
    BackHandlerOwner by delegate {
    override val contentPresentation: Value<AppContentPresentation> = contentPresentationState

    override fun setContentPresentation(presentation: AppContentPresentation) {
        if (contentPresentationState.value != presentation) {
            contentPresentationState.value = presentation
        }
    }

    override val componentContextFactory: ComponentContextFactory<AppComponentContext> =
        ComponentContextFactory { lifecycle, stateKeeper, instanceKeeper, backHandler ->
            val childContext =
                delegate.componentContextFactory(
                    lifecycle = lifecycle,
                    stateKeeper = stateKeeper,
                    instanceKeeper = instanceKeeper,
                    backHandler = backHandler,
                )

            RootComponentContext(
                delegate = childContext,
                screenConfigSerializer = screenConfigSerializer,
                navigation = navigation,
                results = results,
                contentPresentationState = contentPresentationState,
            )
        }
}
