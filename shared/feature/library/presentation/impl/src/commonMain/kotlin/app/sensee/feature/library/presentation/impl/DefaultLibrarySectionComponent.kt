package app.sensee.feature.library.presentation.impl

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.context.appChildStack
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.navigation.pop
import app.sensee.feature.library.presentation.api.LibraryHomeComponent
import app.sensee.feature.library.presentation.api.LibrarySectionComponent
import app.sensee.feature.library.presentation.navigationApi.LibraryConfig
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
public class DefaultLibrarySectionComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val target: LibraryConfig?,
    private val libraryHomeComponentFactory: LibraryHomeComponent.Factory,
) : LibrarySectionComponent,
    AppComponentContext by componentContext {
    private val stackNavigation = StackNavigation<ScreenConfig>()
    private val showBottomBarState = MutableStateFlow(true)

    override val stack: Value<ChildStack<ScreenConfig, AppComponent>> =
        appChildStack(
            source = stackNavigation,
            serializer = screenConfigSerializer,
            initialConfiguration = target ?: LibraryConfig.Home,
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
            is LibraryConfig -> {
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
            LibraryConfig.Home -> libraryHomeComponentFactory.create(componentContext)
            else -> error("Unknown library screen config: $config")
        }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<LibrarySectionComponent.Factory>(),
    )
    public fun interface Factory : LibrarySectionComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            target: LibraryConfig?,
        ): DefaultLibrarySectionComponent
    }
}
