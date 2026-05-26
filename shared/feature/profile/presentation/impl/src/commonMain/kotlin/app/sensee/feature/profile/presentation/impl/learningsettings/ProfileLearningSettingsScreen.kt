package app.sensee.feature.profile.presentation.impl.learningsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.CommonTextKeys
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsUiState
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.layout.SenseeErrorState
import app.sensee.ui.designSystem.component.layout.SenseeLoadingState
import app.sensee.ui.designSystem.component.layout.SenseeScreenContent
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.component.pickerField.SenseePickerField
import app.sensee.ui.designSystem.icons.Tag
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import com.composeunstyled.Text

@Composable
public fun ProfileLearningSettingsScreen(
    component: ProfileLearningSettingsComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberProfileLearningSettingsTextProvider(),
) {
    val uiState by component.uiState.collectAsState()
    val layoutMetrics = LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics()

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        when (val loadingState = uiState.loadingState) {
            DataLoadingState.Idle,
            DataLoadingState.Loading,
            ->
                SenseeScreenContent {
                    SenseeLoadingState(title = textProvider.text(ProfileLearningSettingsTextKeys.TopicsLoading))
                }

            is DataLoadingState.Error ->
                SenseeScreenContent {
                    SenseeErrorState(
                        title =
                            textProvider.errorText(
                                loadingState.throwable,
                                ProfileLearningSettingsTextKeys.TopicsLoadError,
                            ),
                        actions = {
                            SenseeButton(
                                onClick = {
                                    component.onAction(ProfileLearningSettingsAction.Retry)
                                },
                            ) {
                                Text(text = textProvider.text(CommonTextKeys.Retry))
                            }
                        },
                    )
                }

            DataLoadingState.Success ->
                LearningSettingsContent(
                    uiState = uiState,
                    onOpenPicker = { component.onAction(ProfileLearningSettingsAction.OpenPicker) },
                    textProvider = textProvider,
                    layoutMetrics = layoutMetrics,
                )
        }
    }
}

@Composable
private fun LearningSettingsContent(
    uiState: ProfileLearningSettingsUiState,
    onOpenPicker: () -> Unit,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    val spacing = SenseeTheme.spacing
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = layoutMetrics.screenHorizontalPadding,
                    top = layoutMetrics.screenVerticalPadding,
                    end = layoutMetrics.screenHorizontalPadding,
                    bottom = layoutMetrics.screenVerticalPadding,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SenseePickerField(
                title = textProvider.text(ProfileLearningSettingsTextKeys.SectionTopics),
                value = pickerSummary(uiState, textProvider),
                onClick = onOpenPicker,
                hint = textProvider.text(ProfileLearningSettingsTextKeys.TopicsHint),
                leadingIcon = Tag,
            )
        }
    }
}

private fun pickerSummary(
    uiState: ProfileLearningSettingsUiState,
    textProvider: TextProvider,
): String {
    val selectedTopicIds = uiState.selectedTopicIds
    if (selectedTopicIds.isEmpty()) {
        return textProvider.text(ProfileLearningSettingsTextKeys.PickerEmpty)
    }
    // Filter on `topics` (the catalog) preserves catalog order in the summary
    // and silently drops stale ids that no longer resolve.
    val selectedTopics = uiState.topics.filter { it.id in selectedTopicIds }
    if (selectedTopics.isEmpty()) {
        // Catalog hasn't loaded yet (selection arrived first) — fall back to
        // a count so the user still sees a non-empty signal.
        return "${textProvider.text(ProfileLearningSettingsTextKeys.PickerSelectedPrefix)} " +
            "${selectedTopicIds.size}"
    }
    if (selectedTopics.size <= PICKER_SUMMARY_VISIBLE_LIMIT) {
        return selectedTopics.joinToString(", ") { it.displayName }
    }
    val visible = selectedTopics.take(PICKER_SUMMARY_VISIBLE_LIMIT).joinToString(", ") { it.displayName }
    val rest = selectedTopics.size - PICKER_SUMMARY_VISIBLE_LIMIT
    return "$visible, ${textProvider.text(ProfileLearningSettingsTextKeys.PickerMore)} $rest"
}

private const val PICKER_SUMMARY_VISIBLE_LIMIT = 2
