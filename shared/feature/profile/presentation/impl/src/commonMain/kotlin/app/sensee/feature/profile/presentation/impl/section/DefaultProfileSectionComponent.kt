package app.sensee.feature.profile.presentation.impl.section

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.context.appChildPanels
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileChildPanels
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.api.ProfileSectionComponent
import app.sensee.feature.profile.presentation.impl.placeholder.DefaultProfileSettingsPlaceholderComponent
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.router.panels.Panels
import com.arkivanov.decompose.router.panels.PanelsNavigation
import com.arkivanov.decompose.router.panels.navigate
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalDecomposeApi::class)
@AssistedInject
public class DefaultProfileSectionComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val target: ProfileConfig?,
    private val profileHomeComponentFactory: ProfileHomeComponent.Factory,
    private val profileAiSettingsComponentFactory: ProfileAiSettingsComponent.Factory,
) : ProfileSectionComponent,
    AppComponentContext by componentContext {
    private val panelsNavigation =
        PanelsNavigation<ProfileConfig.Home, ProfileConfig, Nothing>()
    private val showBottomBarState = MutableStateFlow(true)

    override val panels: Value<ProfileChildPanels> =
        appChildPanels(
            source = panelsNavigation,
            serializers = ProfileConfig.Home.serializer() to ProfileConfig.serializer(),
            initialPanels = {
                Panels(
                    main = ProfileConfig.Home,
                    details = target?.takeIf { it != ProfileConfig.Home },
                    mode = ChildPanelsMode.DUAL,
                )
            },
            handleBackButton = false,
            navigation = this,
            mainFactory = { _, childContext ->
                profileHomeComponentFactory.create(childContext)
            },
            detailsFactory = ::createDetails,
        )

    override val showBottomBar: StateFlow<Boolean> = showBottomBarState.asStateFlow()

    // System back closes an open category panel before the section itself is
    // left. appChildPanels' own handleBackButton stays off so this is the
    // single back handler — otherwise the press falls through to the shell and
    // exits Profile instead of returning to the menu.
    private val detailsBackCallback =
        BackCallback(isEnabled = panels.value.details != null) { closeSettings() }

    init {
        backHandler.register(detailsBackCallback)
    }

    override fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus =
        when (target) {
            ProfileConfig.Home -> {
                closeSettings()
                onComplete(true)
                NavigationRequestStatus.Handled
            }
            is ProfileConfig -> {
                openSettings(target)
                onComplete(true)
                NavigationRequestStatus.Handled
            }
            else -> NavigationRequestStatus.Unhandled
        }

    override fun back(onResult: (NavigationRequestStatus) -> Unit) {
        if (panels.value.details != null) {
            closeSettings()
            onResult(NavigationRequestStatus.Handled)
        } else {
            onResult(NavigationRequestStatus.Unhandled)
        }
    }

    private fun openSettings(config: ProfileConfig) {
        panelsNavigation.navigate(details = config, extra = null)
        detailsBackCallback.isEnabled = true
    }

    private fun closeSettings() {
        panelsNavigation.navigate(details = null, extra = null)
        detailsBackCallback.isEnabled = false
    }

    private fun createDetails(
        config: ProfileConfig,
        componentContext: AppComponentContext,
    ): AppComponent =
        when (config) {
            ProfileConfig.AiSettings -> profileAiSettingsComponentFactory.create(componentContext)
            ProfileConfig.AppSettings,
            ProfileConfig.LearningSettings,
            ProfileConfig.PracticeSettings,
            ProfileConfig.ExperimentalSettings,
            -> DefaultProfileSettingsPlaceholderComponent(componentContext, config)
            ProfileConfig.Home -> error("Profile Home is the menu panel, not a detail: $config")
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
