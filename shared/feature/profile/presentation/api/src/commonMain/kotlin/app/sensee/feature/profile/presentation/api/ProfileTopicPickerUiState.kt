package app.sensee.feature.profile.presentation.api

import app.sensee.core.presentation.DataLoadingState
import app.sensee.settings.domain.LearningTopic
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * Topic picker state: a [topics] catalog (loaded once, then cached) and the
 * user's current [selectedTopicIds]. Persisting a toggle is optimistic — the
 * UI updates immediately and a failed persist rolls back via the same flow.
 */
public data class ProfileTopicPickerUiState(
    val loadingState: DataLoadingState = DataLoadingState.Idle,
    val topics: PersistentList<LearningTopic> = persistentListOf(),
    val selectedTopicIds: PersistentSet<String> = persistentSetOf(),
)
