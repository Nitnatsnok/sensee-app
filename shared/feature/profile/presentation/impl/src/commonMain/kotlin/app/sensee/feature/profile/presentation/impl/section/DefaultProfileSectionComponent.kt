package app.sensee.feature.profile.presentation.impl.section

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.context.AppContentPresentation
import app.sensee.core.decompose.context.appChildPanels
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileChildPanels
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileSectionComponent
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerComponent
import app.sensee.feature.profile.presentation.impl.placeholder.DefaultProfileSettingsPlaceholderComponent
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.feature.profile.presentation.navigationApi.ProfileExtraConfig
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.router.panels.Panels
import com.arkivanov.decompose.router.panels.PanelsNavigation
import com.arkivanov.decompose.router.panels.navigate
import com.arkivanov.decompose.router.panels.setMode
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.subscribe
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
    @Assisted target: ProfileConfig?,
    private val profileHomeComponentFactory: ProfileHomeComponent.Factory,
    private val profileAiSettingsComponentFactory: ProfileAiSettingsComponent.Factory,
    private val profileLearningSettingsComponentFactory: ProfileLearningSettingsComponent.Factory,
    private val profileTopicPickerComponentFactory: ProfileTopicPickerComponent.Factory,
) : ProfileSectionComponent,
    AppComponentContext by componentContext {
    private val panelsNavigation =
        PanelsNavigation<ProfileConfig.Home, ProfileConfig.Settings, ProfileExtraConfig>()
    private val showBottomBarState = MutableStateFlow(true)
    private var lastSettingsDetail = target as? ProfileConfig.Settings ?: DefaultSettingsConfig

    override val panels: Value<ProfileChildPanels> =
        appChildPanels(
            source = panelsNavigation,
            serializers =
                Triple(
                    ProfileConfig.Home.serializer(),
                    ProfileConfig.Settings.serializer(),
                    ProfileExtraConfig.serializer(),
                ),
            initialPanels = {
                Panels(
                    main = ProfileConfig.Home,
                    details = null,
                    extra = null,
                    mode = ChildPanelsMode.SINGLE,
                )
            },
            handleBackButton = false,
            navigation = this,
            mainFactory = { _, childContext ->
                profileHomeComponentFactory.create(childContext)
            },
            detailsFactory = ::createDetails,
            extraFactory = ::createExtra,
        )

    override val showBottomBar: StateFlow<Boolean> = showBottomBarState.asStateFlow()

    // System back cascades through the panels: close extra (picker) first, then
    // compact detail, before letting the press fall through to the shell.
    // appChildPanels' own handleBackButton stays off so this stays the single source.
    private val panelsBackCallback =
        BackCallback(isEnabled = false) {
            when {
                panels.value.extra != null -> closeTopicPicker()
                panels.value.details != null &&
                    contentPresentation.value == AppContentPresentation.SinglePane -> closeSettings()
            }
        }

    init {
        backHandler.register(panelsBackCallback)
        panels.subscribe(lifecycle) { state ->
            state.details?.configuration?.let { config ->
                lastSettingsDetail = config
            }
            updatePanelsBackCallback()
        }
        contentPresentation.subscribe(lifecycle) { presentation ->
            applyContentPresentation(presentation)
        }
    }

    override fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus =
        when (target) {
            ProfileConfig.Home -> {
                if (contentPresentation.value == AppContentPresentation.SinglePane) {
                    closeSettings()
                } else {
                    ensureSettingsDetailOpened()
                }
                onComplete(true)
                NavigationRequestStatus.Handled
            }
            is ProfileConfig.Settings -> {
                openSettings(target)
                onComplete(true)
                NavigationRequestStatus.Handled
            }
            ProfileExtraConfig.TopicPicker -> {
                openTopicPicker()
                onComplete(true)
                NavigationRequestStatus.Handled
            }
            else -> NavigationRequestStatus.Unhandled
        }

    override fun back(onResult: (NavigationRequestStatus) -> Unit) {
        when {
            panels.value.extra != null -> {
                closeTopicPicker()
                onResult(NavigationRequestStatus.Handled)
            }
            panels.value.details != null &&
                contentPresentation.value == AppContentPresentation.SinglePane -> {
                closeSettings()
                onResult(NavigationRequestStatus.Handled)
            }
            else -> onResult(NavigationRequestStatus.Unhandled)
        }
    }

    private fun openSettings(config: ProfileConfig.Settings) {
        lastSettingsDetail = config
        panelsNavigation.navigate(details = config, extra = null)
    }

    private fun closeSettings() {
        panelsNavigation.navigate(details = null, extra = null)
    }

    private fun openTopicPicker() {
        panelsNavigation.navigate(extra = ProfileExtraConfig.TopicPicker)
    }

    private fun closeTopicPicker() {
        panelsNavigation.navigate(extra = null)
    }

    private fun applyContentPresentation(presentation: AppContentPresentation) {
        val mode = presentation.toChildPanelsMode()
        if (panels.value.mode != mode) {
            panelsNavigation.setMode(mode)
        }
        if (presentation != AppContentPresentation.SinglePane) {
            ensureSettingsDetailOpened()
        }
        updatePanelsBackCallback()
    }

    private fun ensureSettingsDetailOpened() {
        if (panels.value.details == null) {
            panelsNavigation.navigate(
                details = lastSettingsDetail,
                extra = panels.value.extra?.configuration,
            )
        }
    }

    private fun updatePanelsBackCallback() {
        panelsBackCallback.isEnabled =
            panels.value.extra != null ||
            (
                panels.value.details != null &&
                    contentPresentation.value == AppContentPresentation.SinglePane
            )
    }

    private fun createDetails(
        config: ProfileConfig.Settings,
        componentContext: AppComponentContext,
    ): AppComponent =
        when (config) {
            ProfileConfig.Settings.Ai -> profileAiSettingsComponentFactory.create(componentContext)
            ProfileConfig.Settings.Learning ->
                profileLearningSettingsComponentFactory.create(componentContext)
            ProfileConfig.Settings.App,
            ProfileConfig.Settings.Practice,
            ProfileConfig.Settings.Experimental,
            -> DefaultProfileSettingsPlaceholderComponent(componentContext, config)
        }

    private fun createExtra(
        config: ProfileExtraConfig,
        componentContext: AppComponentContext,
    ): AppComponent =
        when (config) {
            ProfileExtraConfig.TopicPicker ->
                profileTopicPickerComponentFactory.create(
                    componentContext = componentContext,
                    onClose = ::closeTopicPicker,
                )
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

@OptIn(ExperimentalDecomposeApi::class)
private fun AppContentPresentation.toChildPanelsMode(): ChildPanelsMode =
    when (this) {
        AppContentPresentation.SinglePane -> ChildPanelsMode.SINGLE
        AppContentPresentation.ListDetail -> ChildPanelsMode.DUAL
        AppContentPresentation.SupportingPane -> ChildPanelsMode.TRIPLE
    }

private val DefaultSettingsConfig = ProfileConfig.Settings.App
