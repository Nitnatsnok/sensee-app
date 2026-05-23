package app.sensee.feature.profile.presentation.impl.learningsettings.picker

import app.sensee.core.coroutines.AppCoroutineScopes
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.settings.domain.LearningSettings
import app.sensee.settings.domain.LearningTopic
import app.sensee.settings.domain.TopicCatalogRepository
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileTopicPickerLogicTest {
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

    private class FakeTopicCatalog(
        private val topics: List<LearningTopic> = SampleTopics,
    ) : TopicCatalogRepository {
        override suspend fun topics(): List<LearningTopic> = topics
    }

    private class SuspendedSettings(
        var snapshot: UserSettingsSnapshot = UserSettingsSnapshot(),
    ) : UserSettingsRepository {
        val pendingUpdates = mutableListOf<CompletableDeferred<UserSettingsSnapshot>>()
        val requestedSnapshots = mutableListOf<UserSettingsSnapshot>()

        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = flowOf(snapshot)

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = snapshot

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot {
            requestedSnapshots += transform(snapshot)
            val update = CompletableDeferred<UserSettingsSnapshot>()
            pendingUpdates += update
            return update.await().also { snapshot = it }
        }
    }

    private fun logic(
        settings: UserSettingsRepository = FakeSettings(),
        topicCatalog: TopicCatalogRepository = FakeTopicCatalog(),
    ) = ProfileTopicPickerLogic(
        settingsRepository = settings,
        topicCatalog = topicCatalog,
        appDispatchers = immediateAppDispatchers(),
        appCoroutineScopes = immediateAppCoroutineScopes(),
        appDiagnostics = noOpAppDiagnostics(),
    )

    private fun immediateAppCoroutineScopes(): AppCoroutineScopes {
        val dispatchers = immediateAppDispatchers()
        return AppCoroutineScopes(
            applicationScope = CoroutineScope(dispatchers.default + SupervisorJob()),
        )
    }

    @Test
    fun `load populates topics and current selection`() {
        val stored =
            UserSettingsSnapshot().copy(learning = LearningSettings(preferredTopicIds = setOf("travel")))

        val state = logic(settings = FakeSettings(stored)).uiState.value

        assertEquals(DataLoadingState.Success, state.loadingState)
        assertEquals(SampleTopics, state.topics)
        assertEquals(setOf("travel"), state.selectedTopicIds)
    }

    @Test
    fun `toggling an unselected topic adds it and persists`() {
        val settings = FakeSettings()
        val logic = logic(settings)

        logic.toggleTopic("travel")

        assertEquals(setOf("travel"), logic.uiState.value.selectedTopicIds)
        assertEquals(setOf("travel"), settings.snapshot.learning.preferredTopicIds)
    }

    @Test
    fun `toggling a selected topic removes it and persists`() {
        val stored =
            UserSettingsSnapshot().copy(learning = LearningSettings(preferredTopicIds = setOf("travel", "food")))
        val settings = FakeSettings(stored)
        val logic = logic(settings)

        logic.toggleTopic("travel")

        assertEquals(setOf("food"), logic.uiState.value.selectedTopicIds)
        assertEquals(setOf("food"), settings.snapshot.learning.preferredTopicIds)
    }

    @Test
    fun `topic catalog failure surfaces as an error state without crashing`() {
        val crashingCatalog =
            object : TopicCatalogRepository {
                override suspend fun topics(): List<LearningTopic> = error("catalog boom")
            }

        val state = logic(topicCatalog = crashingCatalog).uiState.value

        assertTrue(state.loadingState is DataLoadingState.Error)
        assertEquals(emptyList(), state.topics)
    }

    @Test
    fun `latest failed toggle rolls back to stored topic ids after earlier failed save`() =
        runTest {
            val settings = SuspendedSettings()
            val logic = logic(settings)

            logic.toggleTopic("travel")
            logic.toggleTopic("food")

            assertEquals(setOf("travel", "food"), logic.uiState.value.selectedTopicIds)
            assertEquals(2, settings.pendingUpdates.size)
            assertEquals(setOf("travel"), settings.requestedSnapshots[0].learning.preferredTopicIds)
            assertEquals(setOf("travel", "food"), settings.requestedSnapshots[1].learning.preferredTopicIds)

            settings.pendingUpdates[0].completeExceptionally(IllegalStateException("first failed"))
            yield()

            assertEquals(setOf("travel", "food"), logic.uiState.value.selectedTopicIds)

            settings.pendingUpdates[1].completeExceptionally(IllegalStateException("second failed"))
            yield()

            assertEquals(emptySet(), logic.uiState.value.selectedTopicIds)
        }

    @Test
    fun `pending toggle persists after picker logic is destroyed`() =
        runTest {
            val settings = SuspendedSettings()
            val logic = logic(settings)

            logic.toggleTopic("travel")
            assertEquals(setOf("travel"), logic.uiState.value.selectedTopicIds)
            assertEquals(1, settings.pendingUpdates.size)

            logic.onDestroy()
            settings.pendingUpdates
                .single()
                .complete(
                    UserSettingsSnapshot().copy(
                        learning = LearningSettings(preferredTopicIds = setOf("travel")),
                    ),
                )
            yield()

            assertEquals(setOf("travel"), settings.snapshot.learning.preferredTopicIds)
        }

    private companion object {
        val SampleTopics: List<LearningTopic> =
            listOf(
                LearningTopic("travel", "Путешествия", "travel and tourism"),
                LearningTopic("food", "Еда", "food and cooking"),
                LearningTopic("sports", "Спорт", "sports and fitness"),
            )
    }
}
