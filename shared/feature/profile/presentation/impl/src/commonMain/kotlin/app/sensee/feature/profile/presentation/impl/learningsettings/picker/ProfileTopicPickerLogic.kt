package app.sensee.feature.profile.presentation.impl.learningsettings.picker

import app.sensee.core.coroutines.AppCoroutineScopes
import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerUiState
import app.sensee.settings.domain.TopicCatalogRepository
import app.sensee.settings.domain.UserSettingsRepository
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AssistedInject
public class ProfileTopicPickerLogic(
    private val settingsRepository: UserSettingsRepository,
    private val topicCatalog: TopicCatalogRepository,
    private val appDispatchers: AppDispatchers,
    private val appCoroutineScopes: AppCoroutineScopes,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(): ProfileTopicPickerLogic
    }

    private var lastPersistedTopicIds: PersistentSet<String> = persistentSetOf()
    private var saveGeneration: Int = 0

    public val uiState: StateFlow<ProfileTopicPickerUiState>
        field = MutableStateFlow(ProfileTopicPickerUiState())

    init {
        load()
    }

    public fun load() {
        logicScope.launch {
            uiState.update { it.copy(loadingState = DataLoadingState.Loading) }
            runCatchingCancellable {
                val topics = topicCatalog.topics()
                val settings = settingsRepository.readSettings()
                topics to settings
            }.onSuccess { (topics, settings) ->
                lastPersistedTopicIds = settings.learning.preferredTopicIds.toPersistentSet()
                uiState.update {
                    ProfileTopicPickerUiState(
                        loadingState = DataLoadingState.Success,
                        topics = topics.toPersistentList(),
                        selectedTopicIds = lastPersistedTopicIds,
                    )
                }
            }.onFailure { failure ->
                logger.error(failure) { "Failed to load topic catalog" }
                uiState.update { it.copy(loadingState = DataLoadingState.Error(failure)) }
            }
        }
    }

    // Optimistic toggle: the UI flips immediately, persistence runs in the
    // background, and on the latest failed persist the state rolls back to
    // what the repository actually holds.
    public fun toggleTopic(id: String) {
        val previous = uiState.value.selectedTopicIds
        val next = if (id in previous) previous.remove(id) else previous.add(id)
        if (next == previous) {
            return
        }
        val generation = ++saveGeneration
        uiState.update { it.copy(selectedTopicIds = next) }
        appCoroutineScopes.applicationScope.launch(appDispatchers.main.immediate) {
            runCatchingCancellable {
                settingsRepository.updateLearningSettings { learning ->
                    learning.copy(preferredTopicIds = next)
                }
            }.onSuccess { snapshot ->
                val persisted = snapshot.learning.preferredTopicIds.toPersistentSet()
                lastPersistedTopicIds = persisted
                if (shouldApplySaveResult(generation)) {
                    uiState.update { it.copy(selectedTopicIds = persisted) }
                }
            }.onFailure { throwable ->
                logger.error(throwable) { "Failed to persist preferred topic ids" }
                if (shouldApplySaveResult(generation)) {
                    uiState.update { it.copy(selectedTopicIds = readPersistedTopicIds()) }
                }
            }
        }
    }

    private fun shouldApplySaveResult(generation: Int): Boolean = generation == saveGeneration && logicScope.isActive

    private suspend fun readPersistedTopicIds(): PersistentSet<String> =
        runCatchingCancellable {
            settingsRepository
                .readSettings()
                .learning.preferredTopicIds
                .toPersistentSet()
        }.getOrElse { throwable ->
            logger.error(throwable) { "Failed to reload preferred topic ids after a persist failure" }
            lastPersistedTopicIds
        }.also { persisted ->
            lastPersistedTopicIds = persisted
        }
}
