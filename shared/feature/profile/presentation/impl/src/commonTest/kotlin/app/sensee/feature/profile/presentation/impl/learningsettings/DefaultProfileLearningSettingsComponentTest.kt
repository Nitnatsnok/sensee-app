package app.sensee.feature.profile.presentation.impl.learningsettings

import app.sensee.core.decompose.context.RootComponentContext
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsAction
import app.sensee.feature.profile.presentation.navigationApi.ProfileExtraConfig
import app.sensee.settings.domain.LearningTopic
import app.sensee.settings.domain.TopicCatalogRepository
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.PolymorphicSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultProfileLearningSettingsComponentTest {
    @Test
    fun `OpenPicker requests navigation to the topic picker extra config`() {
        val captured = mutableListOf<ScreenConfig>()
        val component =
            buildComponent(
                navigation =
                    object : NavigationDispatcher {
                        override fun open(
                            target: ScreenConfig,
                            onComplete: (isSuccess: Boolean) -> Unit,
                        ): NavigationRequestStatus {
                            captured += target
                            onComplete(true)
                            return NavigationRequestStatus.Handled
                        }

                        override fun back(onResult: (NavigationRequestStatus) -> Unit) = Unit
                    },
            )

        component.onAction(ProfileLearningSettingsAction.OpenPicker)

        assertEquals(listOf<ScreenConfig>(ProfileExtraConfig.TopicPicker), captured)
    }

    private fun buildComponent(navigation: NavigationDispatcher): DefaultProfileLearningSettingsComponent {
        val lifecycle = LifecycleRegistry()
        val componentContext =
            RootComponentContext(
                delegate = DefaultComponentContext(lifecycle),
                screenConfigSerializer = PolymorphicSerializer(ScreenConfig::class),
                navigation = navigation,
            )
        lifecycle.resume()
        val logicFactory =
            ProfileLearningSettingsLogic.Factory {
                ProfileLearningSettingsLogic(
                    settingsRepository = FakeSettings(),
                    topicCatalog = FakeTopicCatalog(),
                    appDispatchers = immediateAppDispatchers(),
                    appDiagnostics = noOpAppDiagnostics(),
                )
            }
        return DefaultProfileLearningSettingsComponent(
            componentContext = componentContext,
            profileLearningSettingsLogicFactory = logicFactory,
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
