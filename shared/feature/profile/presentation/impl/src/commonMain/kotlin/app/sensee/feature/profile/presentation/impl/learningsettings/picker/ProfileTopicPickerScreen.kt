package app.sensee.feature.profile.presentation.impl.learningsettings.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.CommonTextKeys
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerAction
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerComponent
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerUiState
import app.sensee.settings.domain.LearningTopic
import app.sensee.ui.designSystem.component.LocalSenseeMinTouchTargetSize
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.checkbox.SenseeCheckbox
import app.sensee.ui.designSystem.component.layout.SenseeErrorState
import app.sensee.ui.designSystem.component.layout.SenseeLoadingState
import app.sensee.ui.designSystem.component.layout.SenseePaneHeader
import app.sensee.ui.designSystem.component.layout.SenseeScreenContent
import app.sensee.ui.designSystem.component.layout.SenseeSheetHeader
import app.sensee.ui.designSystem.component.layout.SenseeSurface
import app.sensee.ui.designSystem.component.senseeMinTouchTargetSize
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

@Composable
public fun ProfileTopicPickerScreen(
    component: ProfileTopicPickerComponent,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    textProvider: TextProvider = rememberProfileTopicPickerTextProvider(),
) {
    val uiState by component.uiState.collectAsState()
    val title = textProvider.text(ProfileTopicPickerTextKeys.Title)
    val closeLabel = textProvider.text(ProfileTopicPickerTextKeys.Close)
    val onClose = { component.onAction(ProfileTopicPickerAction.Close) }

    if (compact) {
        Column(modifier = modifier.fillMaxWidth()) {
            SenseeSheetHeader(
                onClose = onClose,
                closeAccessibilityLabel = closeLabel,
            ) {
                Text(text = title, style = SenseeTheme.typography.titleMedium)
            }
            ProfileTopicPickerBody(
                uiState = uiState,
                onAction = component::onAction,
                textProvider = textProvider,
            )
        }
    } else {
        val spacing = SenseeTheme.spacing
        SenseeSurface(
            modifier =
                modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = spacing.medium, bottom = spacing.medium, end = spacing.medium),
            shape = SenseeTheme.shapes.large,
            contentPadding = PaddingValues(0.dp),
            borderWidth = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SenseePaneHeader(
                    onClose = onClose,
                    closeAccessibilityLabel = closeLabel,
                    contentPadding =
                        PaddingValues(start = spacing.large, top = spacing.small, end = spacing.small),
                ) {
                    Text(text = title, style = SenseeTheme.typography.headlineSmall)
                }
                ProfileTopicPickerBody(
                    uiState = uiState,
                    onAction = component::onAction,
                    textProvider = textProvider,
                )
            }
        }
    }
}

@Composable
private fun ProfileTopicPickerBody(
    uiState: ProfileTopicPickerUiState,
    onAction: (ProfileTopicPickerAction) -> Unit,
    textProvider: TextProvider,
) {
    when (val state = uiState.loadingState) {
        DataLoadingState.Idle,
        DataLoadingState.Loading,
        ->
            SenseeScreenContent {
                SenseeLoadingState(title = textProvider.text(ProfileTopicPickerTextKeys.Loading))
            }

        is DataLoadingState.Error ->
            SenseeScreenContent {
                SenseeErrorState(
                    title =
                        textProvider.errorText(
                            state.throwable,
                            ProfileTopicPickerTextKeys.LoadError,
                        ),
                    actions = {
                        SenseeButton(onClick = { onAction(ProfileTopicPickerAction.Retry) }) {
                            Text(text = textProvider.text(CommonTextKeys.Retry))
                        }
                    },
                )
            }

        DataLoadingState.Success ->
            TopicList(
                topics = uiState.topics,
                selectedTopicIds = uiState.selectedTopicIds,
                onToggle = { id -> onAction(ProfileTopicPickerAction.Toggle(id)) },
            )
    }
}

@Composable
private fun TopicList(
    topics: ImmutableList<LearningTopic>,
    selectedTopicIds: ImmutableSet<String>,
    onToggle: (String) -> Unit,
) {
    val spacing = SenseeTheme.spacing
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                top = spacing.small,
                bottom = spacing.large,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        items(topics, key = { it.id }) { topic ->
            TopicRow(
                topic = topic,
                checked = topic.id in selectedTopicIds,
                onToggle = { onToggle(topic.id) },
            )
        }
    }
}

@Composable
private fun TopicRow(
    topic: LearningTopic,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val spacing = SenseeTheme.spacing
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val minTouchTargetSize = LocalSenseeMinTouchTargetSize.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .senseeMinTouchTargetSize(minTouchTargetSize)
                .toggleable(
                    value = checked,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                ).padding(horizontal = spacing.large, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        SenseeCheckbox(checked = checked, onCheckedChange = null)
        Text(
            text = topic.displayName,
            color = colors.textPrimary,
            style = typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}
