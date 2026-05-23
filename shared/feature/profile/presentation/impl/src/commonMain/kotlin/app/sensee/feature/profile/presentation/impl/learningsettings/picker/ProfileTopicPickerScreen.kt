package app.sensee.feature.profile.presentation.impl.learningsettings.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.CommonTextKeys
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerAction
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerComponent
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerUiState
import app.sensee.settings.domain.LearningTopic
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.button.SenseeIconButton
import app.sensee.ui.designSystem.component.checkbox.SenseeCheckbox
import app.sensee.ui.designSystem.component.layout.SenseeErrorState
import app.sensee.ui.designSystem.component.layout.SenseeLoadingState
import app.sensee.ui.designSystem.component.layout.SenseeScreenContent
import app.sensee.ui.designSystem.component.layout.SenseeSheetHeader
import app.sensee.ui.designSystem.component.senseeMinTouchTargetSize
import app.sensee.ui.designSystem.icons.Close24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

/**
 * Topic picker, rendered both as the Profile section's third pane and inside a
 * `SenseeModalBottomSheet`. The same header (title + close) appears in both
 * paths — in the sheet path it sits below the drag indicator.
 *
 * [paneBackground] is true when the picker is laid out as its own pane and
 * needs to paint a solid surface; false inside the sheet where the sheet
 * already provides one.
 */
@Composable
public fun ProfileTopicPickerScreen(
    component: ProfileTopicPickerComponent,
    modifier: Modifier = Modifier,
    paneBackground: Boolean = true,
    textProvider: TextProvider = rememberProfileTopicPickerTextProvider(),
) {
    val uiState by component.uiState.collectAsState()
    val rootModifier =
        if (paneBackground) {
            modifier.fillMaxSize().background(SenseeTheme.colors.background)
        } else {
            modifier.fillMaxWidth()
        }
    Box(modifier = rootModifier) {
        ProfileTopicPickerContent(
            uiState = uiState,
            onAction = component::onAction,
            textProvider = textProvider,
        )
    }
}

@Composable
private fun ProfileTopicPickerContent(
    uiState: ProfileTopicPickerUiState,
    onAction: (ProfileTopicPickerAction) -> Unit,
    textProvider: TextProvider,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PickerHeader(
            onClose = { onAction(ProfileTopicPickerAction.Close) },
            textProvider = textProvider,
        )
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
}

@Composable
private fun PickerHeader(
    onClose: () -> Unit,
    textProvider: TextProvider,
) {
    val spacing = SenseeTheme.spacing
    val typography = SenseeTheme.typography
    SenseeSheetHeader(
        modifier = Modifier.padding(horizontal = spacing.large),
        title = {
            Text(
                text = textProvider.text(ProfileTopicPickerTextKeys.Title),
                style = typography.titleMedium,
            )
        },
        actions = {
            SenseeIconButton(
                onClick = onClose,
                accessibilityLabel = textProvider.text(ProfileTopicPickerTextKeys.Close),
                icon = { SenseeIcon(imageVector = Close24px, contentDescription = null) },
            )
        },
    )
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .senseeMinTouchTargetSize()
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
