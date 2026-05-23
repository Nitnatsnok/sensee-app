package app.sensee.feature.profile.presentation.impl.learningsettings

import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.settings.domain.LearningSettings
import app.sensee.settings.domain.LearningTopic
import app.sensee.settings.domain.TopicCatalogRepository
import app.sensee.settings.domain.UserSettingsRepository
import app.sensee.settings.domain.UserSettingsScope
import app.sensee.settings.domain.UserSettingsSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileLearningSettingsLogicTest {
    private class FakeSettings(
        initial: UserSettingsSnapshot = UserSettingsSnapshot(),
    ) : UserSettingsRepository {
        private val flow = MutableStateFlow(initial)

        override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = flow.asStateFlow()

        override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = flow.value

        override suspend fun updateSettings(
            scope: UserSettingsScope,
            transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
        ): UserSettingsSnapshot {
            val next = transform(flow.value)
            flow.value = next
            return next
        }
    }

    private class FakeTopicCatalog(
        private val topics: List<LearningTopic> = SampleTopics,
    ) : TopicCatalogRepository {
        override suspend fun topics(): List<LearningTopic> = topics
    }

    private fun logic(
        settings: UserSettingsRepository = FakeSettings(),
        topicCatalog: TopicCatalogRepository = FakeTopicCatalog(),
    ) = ProfileLearningSettingsLogic(
        settingsRepository = settings,
        topicCatalog = topicCatalog,
        appDispatchers = immediateAppDispatchers(),
        appDiagnostics = noOpAppDiagnostics(),
    )

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
    fun `retry reloads topics after a catalog failure`() {
        var fail = true
        val catalog =
            object : TopicCatalogRepository {
                override suspend fun topics(): List<LearningTopic> {
                    if (fail) error("catalog boom")
                    return SampleTopics
                }
            }
        val logic = logic(topicCatalog = catalog)
        assertTrue(logic.uiState.value.loadingState is DataLoadingState.Error)

        fail = false
        logic.load()

        val state = logic.uiState.value
        assertEquals(DataLoadingState.Success, state.loadingState)
        assertEquals(SampleTopics, state.topics)
    }

    @Test
    fun `selection updates from the settings flow stay in sync`() =
        runTest {
            val settings = FakeSettings()
            val logic = logic(settings = settings)

            assertEquals(emptySet(), logic.uiState.value.selectedTopicIds)

            // External writer (e.g. the picker component) flips the selection.
            settings.updateSettings { it.copy(learning = it.learning.copy(preferredTopicIds = setOf("food"))) }
            yield()

            assertEquals(setOf("food"), logic.uiState.value.selectedTopicIds)
        }

    @Test
    fun `flowOf-based observer also reflects the initial selection`() {
        val stored =
            UserSettingsSnapshot().copy(
                learning = LearningSettings(preferredTopicIds = setOf("travel", "food")),
            )
        val flowOnlySettings =
            object : UserSettingsRepository {
                override fun observeSettings(scope: UserSettingsScope): Flow<UserSettingsSnapshot> = flowOf(stored)

                override suspend fun readSettings(scope: UserSettingsScope): UserSettingsSnapshot = stored

                override suspend fun updateSettings(
                    scope: UserSettingsScope,
                    transform: (UserSettingsSnapshot) -> UserSettingsSnapshot,
                ): UserSettingsSnapshot = transform(stored)
            }

        val state = logic(settings = flowOnlySettings).uiState.value
        assertEquals(setOf("travel", "food"), state.selectedTopicIds)
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
