package app.sensee.feature.home.presentation.impl

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.context.appChildStack
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.navigation.pop
import app.sensee.feature.home.presentation.api.HomeComponent
import app.sensee.feature.home.presentation.api.HomeSectionComponent
import app.sensee.feature.home.presentation.navigationApi.HomeConfig
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.value.Value
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
public class DefaultHomeSectionComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val target: HomeConfig?,
    private val homeComponentFactory: HomeComponent.Factory,
) : HomeSectionComponent,
    AppComponentContext by componentContext {
    private val stackNavigation = StackNavigation<ScreenConfig>()

    override val stack: Value<ChildStack<ScreenConfig, AppComponent>> =
        appChildStack(
            source = stackNavigation,
            serializer = screenConfigSerializer,
            initialConfiguration = target ?: HomeConfig.Home,
            handleBackButton = true,
            navigation = this,
            childFactory = ::createChild,
        )

    override val showBottomBar: StateFlow<Boolean>
        field = MutableStateFlow(true)

    override fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus =
        when (target) {
            is HomeConfig -> {
                stackNavigation.bringToFront(target) {
                    onComplete(true)
                }
                NavigationRequestStatus.Handled
            }
            else -> NavigationRequestStatus.Unhandled
        }

    override fun back(onResult: (NavigationRequestStatus) -> Unit) {
        stackNavigation.pop(onResult = onResult)
    }

    private fun createChild(
        config: ScreenConfig,
        componentContext: AppComponentContext,
    ): AppComponent =
        when (config) {
            HomeConfig.Home -> homeComponentFactory.create(componentContext)
            else -> error("Unknown home screen config: $config")
        }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<HomeSectionComponent.Factory>(),
    )
    public fun interface Factory : HomeSectionComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            target: HomeConfig?,
        ): DefaultHomeSectionComponent
    }
}
