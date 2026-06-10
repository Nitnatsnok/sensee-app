package app.sensee.feature.profile.presentation.impl.learningsettings

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.decompose.logic.BaseLogic
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.core.presentation.DataLoadingState
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsUiState
import app.sensee.settings.domain.TopicCatalogRepository
import app.sensee.settings.domain.UserSettingsRepository
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
public class ProfileLearningSettingsLogic(
    private val settingsRepository: UserSettingsRepository,
    private val topicCatalog: TopicCatalogRepository,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics) {
    @AssistedFactory
    public fun interface Factory {
        public fun create(): ProfileLearningSettingsLogic
    }

    public val uiState: StateFlow<ProfileLearningSettingsUiState>
        field = MutableStateFlow(ProfileLearningSettingsUiState())

    init {
        loadCatalog()
        // Picker writes the selection too; observing the flow keeps the
        // trigger-row summary in sync regardless of who toggled.
        logicScope.launch {
            settingsRepository.observeSettings().collectLatest { snapshot ->
                uiState.update {
                    it.copy(selectedTopicIds = snapshot.learning.preferredTopicIds.toPersistentSet())
                }
            }
        }
    }

    public fun load() {
        loadCatalog()
    }

    private fun loadCatalog() {
        logicScope.launch {
            uiState.update { it.copy(loadingState = DataLoadingState.Loading) }
            runCatchingCancellable { topicCatalog.topics() }
                .onSuccess { topics ->
                    uiState.update {
                        it.copy(
                            loadingState = DataLoadingState.Success,
                            topics = topics.toPersistentList(),
                        )
                    }
                }.onFailure { failure ->
                    logger.error(failure) { "Failed to load topic catalog" }
                    uiState.update {
                        it.copy(
                            loadingState = DataLoadingState.Error(failure),
                            topics = persistentListOf(),
                        )
                    }
                }
        }
    }
}
