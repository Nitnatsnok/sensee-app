package app.sensee.feature.profile.presentation.impl.section

import app.sensee.core.decompose.context.RootComponentContext
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsUiState
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.api.ProfileSettingsPlaceholderComponent
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
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
        val component = buildComponent(target = ProfileConfig.AiSettings)
        val detail = component.panels.value.details
        assertEquals(ProfileConfig.AiSettings, detail?.configuration)
    }

    @Test
    fun `opening a category shows its component in the detail panel`() {
        val component = buildComponent(target = null)

        val status = component.open(ProfileConfig.AiSettings) {}

        val detail = component.panels.value.details
        assertEquals(NavigationRequestStatus.Handled, status)
        assertEquals(ProfileConfig.AiSettings, detail?.configuration)
        assertIs<ProfileAiSettingsComponent>(detail?.instance)
    }

    @Test
    fun `an unimplemented category opens a placeholder carrying its config`() {
        val component = buildComponent(target = null)

        component.open(ProfileConfig.AppSettings) {}

        val detail = component.panels.value.details
        val instance = detail?.instance
        assertIs<ProfileSettingsPlaceholderComponent>(instance)
        assertEquals(ProfileConfig.AppSettings, instance.config)
    }

    @Test
    fun `opening Home closes the open category`() {
        val component = buildComponent(target = ProfileConfig.AiSettings)

        component.open(ProfileConfig.Home) {}

        assertNull(component.panels.value.details)
    }

    @Test
    fun `back closes an open category and reports handled`() {
        val component = buildComponent(target = ProfileConfig.AiSettings)

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
            profileAiSettingsComponentFactory =
                { FakeProfileAiSettingsComponent() },
        )
    }

    private class FakeProfileHomeComponent : ProfileHomeComponent {
        override val items: ImmutableList<ProfileConfig> = persistentListOf()

        override fun onItemSelected(config: ProfileConfig) = Unit
    }

    private class FakeProfileAiSettingsComponent : ProfileAiSettingsComponent {
        override val uiState: StateFlow<ProfileAiSettingsUiState> =
            MutableStateFlow(ProfileAiSettingsUiState())

        override fun onAction(action: ProfileAiSettingsAction) = Unit
    }
}
