package app.sensee.feature.profile.presentation.impl.learningsettings.picker

import app.sensee.core.coroutines.AppCoroutineScopes
import app.sensee.core.decompose.context.RootComponentContext
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerAction
import app.sensee.settings.domain.LearningTopic
import app.sensee.settings.domain.TopicCatalogRepository
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.PolymorphicSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultProfileTopicPickerComponentTest {
    @Test
    fun `Close action invokes the onClose callback supplied by the host`() {
        var closeCount = 0
        val component = buildComponent(onClose = { closeCount += 1 })

        component.onAction(ProfileTopicPickerAction.Close)

        assertEquals(1, closeCount)
    }

    private fun buildComponent(onClose: () -> Unit = {}): DefaultProfileTopicPickerComponent {
        val lifecycle = LifecycleRegistry()
        val componentContext =
            RootComponentContext(
                delegate = DefaultComponentContext(lifecycle),
                screenConfigSerializer = PolymorphicSerializer(ScreenConfig::class),
            )
        lifecycle.resume()
        val logicFactory =
            ProfileTopicPickerLogic.Factory {
                ProfileTopicPickerLogic(
                    settingsRepository = FakeSettings(),
                    topicCatalog = FakeTopicCatalog(),
                    appDispatchers = immediateAppDispatchers(),
                    appCoroutineScopes = immediateAppCoroutineScopes(),
                    appDiagnostics = noOpAppDiagnostics(),
                )
            }
        return DefaultProfileTopicPickerComponent(
            componentContext = componentContext,
            onClose = onClose,
            logicFactory = logicFactory,
        )
    }

    private fun immediateAppCoroutineScopes(): AppCoroutineScopes {
        val dispatchers = immediateAppDispatchers()
        return AppCoroutineScopes(
            applicationScope = CoroutineScope(dispatchers.default + SupervisorJob()),
        )
    }

    private class FakeSettings(
        var snapshot: UserSettingsSnapshot = UserSettingsSnapshot(),
    ) : UserSettingsRepository {
        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = flowOf(snapshot)

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = snapshot

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot = transform(snapshot).also { snapshot = it }
    }

    private class FakeTopicCatalog : TopicCatalogRepository {
        override suspend fun topics(): List<LearningTopic> = emptyList()
    }
}
