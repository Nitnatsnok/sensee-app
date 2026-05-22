package app.sensee.feature.profile.presentation.impl.aisettings

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsUiState
import kotlinx.coroutines.flow.drop

/**
 * TextFieldState mirrors for the five text fields the design-system requires us
 * to drive with [TextFieldState] ([app.sensee.ui.designSystem.component.textField.SenseeTextField]
 * has no value/onValueChange overload). All draft values live in
 * [ProfileAiSettingsUiState.draftSnapshot] — this class is the UI-side handle only,
 * not the source of truth.
 */
@Stable
internal class ProfileAiSettingsFormState(
    val aiApiKey: TextFieldState,
    val aiModel: TextFieldState,
    val ttsApiKey: TextFieldState,
    val ttsModel: TextFieldState,
    val ttsVoiceId: TextFieldState,
)

@Composable
internal fun rememberProfileAiSettingsFormState(): ProfileAiSettingsFormState {
    val aiApiKey = rememberTextFieldState()
    val aiModel = rememberTextFieldState()
    val ttsApiKey = rememberTextFieldState()
    val ttsModel = rememberTextFieldState()
    val ttsVoiceId = rememberTextFieldState()
    return remember { ProfileAiSettingsFormState(aiApiKey, aiModel, ttsApiKey, ttsModel, ttsVoiceId) }
}

@Composable
internal fun SyncProfileForm(
    uiState: ProfileAiSettingsUiState,
    formState: ProfileAiSettingsFormState,
    onAction: (ProfileAiSettingsAction) -> Unit,
) {
    formState.aiApiKey.BindToDraft(uiState.draftSnapshot.aiApiKey) {
        onAction(ProfileAiSettingsAction.SetAiApiKey(it))
    }
    formState.aiModel.BindToDraft(uiState.draftSnapshot.aiModel) {
        onAction(ProfileAiSettingsAction.SetAiModel(it))
    }
    formState.ttsApiKey.BindToDraft(uiState.draftSnapshot.ttsApiKey) {
        onAction(ProfileAiSettingsAction.SetTtsApiKey(it))
    }
    formState.ttsModel.BindToDraft(uiState.draftSnapshot.ttsModel) {
        onAction(ProfileAiSettingsAction.SetTtsModel(it))
    }
    formState.ttsVoiceId.BindToDraft(uiState.draftSnapshot.ttsVoiceId) {
        onAction(ProfileAiSettingsAction.SetTtsVoiceId(it))
    }
}

/**
 * Two-way binding between a [TextFieldState] (UI-owned, required by the
 * design-system) and a draft string in [ProfileAiSettingsUiState]. Downstream: any
 * `draftValue` change (load, save, Logic reset) is applied to [TextFieldState];
 * `setTextIfDifferent` keeps echoes from re-triggering the upstream. Upstream:
 * every keystroke becomes an action; `drop(1)` skips the first snapshot which
 * is the value the downstream just placed, not a user edit.
 *
 * `rememberUpdatedState` is required because the upstream effect is keyed on
 * the stable receiver and never restarts; without it the captured callback
 * would hold the first composition's reference.
 */
@Composable
private fun TextFieldState.BindToDraft(
    draftValue: String,
    onChange: (String) -> Unit,
) {
    val currentOnChange by rememberUpdatedState(onChange)

    LaunchedEffect(this, draftValue) {
        setTextIfDifferent(draftValue)
    }
    LaunchedEffect(this) {
        snapshotFlow { text.toString() }
            .drop(1)
            .collect { currentOnChange(it) }
    }
}

private fun TextFieldState.setTextIfDifferent(value: String) {
    if (text.toString() != value) {
        setTextAndPlaceCursorAtEnd(value)
    }
}
