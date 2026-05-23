package app.sensee.feature.profile.presentation.api

import app.sensee.core.presentation.DataLoadingState
import app.sensee.settings.domain.LearningTopic
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * Learning-category settings state. The screen shows a single picker-trigger
 * row whose summary lists [selectedTopicIds] resolved against the cached
 * [topics] catalog. The picker itself lives in a separate component.
 */
public data class ProfileLearningSettingsUiState(
    val loadingState: DataLoadingState = DataLoadingState.Idle,
    val topics: PersistentList<LearningTopic> = persistentListOf(),
    val selectedTopicIds: PersistentSet<String> = persistentSetOf(),
)
