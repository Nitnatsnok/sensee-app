package app.sensee.feature.profile.presentation.impl.section

import app.sensee.core.decompose.context.RootComponentContext
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsUiState
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsUiState
import app.sensee.feature.profile.presentation.api.ProfileSettingsPlaceholderComponent
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerAction
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerComponent
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerUiState
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.feature.profile.presentation.navigationApi.ProfileExtraConfig
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.PolymorphicSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalDecomposeApi::class)
class DefaultProfileSectionComponentTest {
    @Test
    fun `section opens with no detail panel when target is null`() {
        val component = buildComponent(target = null)
        assertNull(component.panels.value.details)
    }

    @Test
    fun `section opens directly into the target category`() {
        val component = buildComponent(target = ProfileConfig.Settings.Ai)
        val detail = component.panels.value.details
        assertEquals(ProfileConfig.Settings.Ai, detail?.configuration)
    }

    @Test
    fun `opening a category shows its component in the detail panel`() {
        val component = buildComponent(target = null)

        val status = component.open(ProfileConfig.Settings.Ai) {}

        val detail = component.panels.value.details
        assertEquals(NavigationRequestStatus.Handled, status)
        assertEquals(ProfileConfig.Settings.Ai, detail?.configuration)
        assertIs<ProfileAiSettingsComponent>(detail?.instance)
    }

    @Test
    fun `an unimplemented category opens a placeholder carrying its config`() {
        val component = buildComponent(target = null)

        component.open(ProfileConfig.Settings.App) {}

        val detail = component.panels.value.details
        val instance = detail?.instance
        assertIs<ProfileSettingsPlaceholderComponent>(instance)
        assertEquals(ProfileConfig.Settings.App, instance.config)
    }

    @Test
    fun `opening LearningSettings shows the learning settings component in the detail panel`() {
        val component = buildComponent(target = null)

        component.open(ProfileConfig.Settings.Learning) {}

        val detail = component.panels.value.details
        assertEquals(ProfileConfig.Settings.Learning, detail?.configuration)
        assertIs<ProfileLearningSettingsComponent>(detail?.instance)
    }

    @Test
    fun `opening Home closes the open category`() {
        val component = buildComponent(target = ProfileConfig.Settings.Ai)

        component.open(ProfileConfig.Home) {}

        assertNull(component.panels.value.details)
    }

    @Test
    fun `back closes an open category and reports handled`() {
        val component = buildComponent(target = ProfileConfig.Settings.Ai)

        var result: NavigationRequestStatus? = null
        component.back { result = it }

        assertEquals(NavigationRequestStatus.Handled, result)
        assertNull(component.panels.value.details)
    }

    @Test
    fun `back with no open category reports unhandled`() {
        val component = buildComponent(target = null)

        var result: NavigationRequestStatus? = null
        component.back { result = it }

        assertEquals(NavigationRequestStatus.Unhandled, result)
    }

    @Test
    fun `opening the topic picker shows it in the extra panel`() {
        val component = buildComponent(target = ProfileConfig.Settings.Learning)

        val status = component.open(ProfileExtraConfig.TopicPicker) {}

        val extra = component.panels.value.extra
        assertEquals(NavigationRequestStatus.Handled, status)
        assertEquals(ProfileExtraConfig.TopicPicker, extra?.configuration)
        assertIs<ProfileTopicPickerComponent>(extra?.instance)
    }

    @Test
    fun `the picker's Close action closes the extra panel`() {
        val component = buildComponent(target = ProfileConfig.Settings.Learning)
        component.open(ProfileExtraConfig.TopicPicker) {}

        val picker =
            assertIs<ProfileTopicPickerComponent>(
                component.panels.value.extra
                    ?.instance,
            )
        picker.onAction(ProfileTopicPickerAction.Close)

        assertNull(component.panels.value.extra)
    }

    @Test
    fun `back closes the picker before the detail panel`() {
        val component = buildComponent(target = ProfileConfig.Settings.Learning)
        component.open(ProfileExtraConfig.TopicPicker) {}

        var first: NavigationRequestStatus? = null
        component.back { first = it }
        assertEquals(NavigationRequestStatus.Handled, first)
        assertNull(component.panels.value.extra)
        assertEquals(
            ProfileConfig.Settings.Learning,
            component.panels.value.details
                ?.configuration,
        )

        var second: NavigationRequestStatus? = null
        component.back { second = it }
        assertEquals(NavigationRequestStatus.Handled, second)
        assertNull(component.panels.value.details)
    }

    private fun buildComponent(target: ProfileConfig?): DefaultProfileSectionComponent {
        val lifecycle = LifecycleRegistry()
        val componentContext =
            RootComponentContext(
                delegate = DefaultComponentContext(lifecycle),
                // ProfileConfig panels carry their own serializers; the context
                // one is unused here, so any KSerializer<ScreenConfig> will do.
                screenConfigSerializer = PolymorphicSerializer(ScreenConfig::class),
            )
        lifecycle.resume()
        return DefaultProfileSectionComponent(
            componentContext = componentContext,
            target = target,
            profileHomeComponentFactory = { FakeProfileHomeComponent() },
            profileAiSettingsComponentFactory = { FakeProfileAiSettingsComponent() },
            profileLearningSettingsComponentFactory = { FakeProfileLearningSettingsComponent() },
            profileTopicPickerComponentFactory = { _, onClose -> FakeProfileTopicPickerComponent(onClose) },
        )
    }

    private class FakeProfileHomeComponent : ProfileHomeComponent {
        override val items: ImmutableList<ProfileConfig.Settings> = persistentListOf()

        override fun onItemSelected(config: ProfileConfig.Settings) = Unit
    }

    private class FakeProfileAiSettingsComponent : ProfileAiSettingsComponent {
        override val uiState: StateFlow<ProfileAiSettingsUiState> =
            MutableStateFlow(ProfileAiSettingsUiState())

        override fun onAction(action: ProfileAiSettingsAction) = Unit
    }

    private class FakeProfileLearningSettingsComponent : ProfileLearningSettingsComponent {
        override val uiState: StateFlow<ProfileLearningSettingsUiState> =
            MutableStateFlow(ProfileLearningSettingsUiState())

        override fun onAction(action: ProfileLearningSettingsAction) = Unit
    }

    private class FakeProfileTopicPickerComponent(
        private val onClose: () -> Unit,
    ) : ProfileTopicPickerComponent {
        override val uiState: StateFlow<ProfileTopicPickerUiState> =
            MutableStateFlow(ProfileTopicPickerUiState())

        override fun onAction(action: ProfileTopicPickerAction) {
            when (action) {
                ProfileTopicPickerAction.Close -> onClose()
                is ProfileTopicPickerAction.Toggle, ProfileTopicPickerAction.Retry -> Unit
            }
        }
    }
}
