package app.sensee.feature.profile.presentation.impl

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.context.appChildStack
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.navigation.pop
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.api.ProfileSectionComponent
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
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
import kotlinx.coroutines.flow.asStateFlow

@AssistedInject
public class DefaultProfileSectionComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val target: ProfileConfig?,
    private val profileHomeComponentFactory: ProfileHomeComponent.Factory,
) : ProfileSectionComponent,
    AppComponentContext by componentContext {
    private val stackNavigation = StackNavigation<ScreenConfig>()
    private val showBottomBarState = MutableStateFlow(true)

    override val stack: Value<ChildStack<ScreenConfig, AppComponent>> =
        appChildStack(
            source = stackNavigation,
            serializer = screenConfigSerializer,
            initialConfiguration = target ?: ProfileConfig.Home,
            handleBackButton = true,
            navigation = this,
            childFactory = ::createChild,
        )

    override val showBottomBar: StateFlow<Boolean> = showBottomBarState.asStateFlow()

    override fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus =
        when (target) {
            is ProfileConfig -> {
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
            ProfileConfig.Home -> profileHomeComponentFactory.create(componentContext)
            else -> error("Unknown profile screen config: $config")
        }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<ProfileSectionComponent.Factory>(),
    )
    public fun interface Factory : ProfileSectionComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            target: ProfileConfig?,
        ): DefaultProfileSectionComponent
    }
}
