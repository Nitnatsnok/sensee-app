package app.sensee.feature.profile.presentation.impl.aisettings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.profile.presentation.api.KeyCheckStatus
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.selectField.SenseeSelectField
import app.sensee.ui.designSystem.component.textField.SenseeTextField
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun VerifyRow(
    status: KeyCheckStatus,
    textProvider: TextProvider,
    onVerify: () -> Unit,
) {
    SenseeButton(
        onClick = onVerify,
        enabled = status != KeyCheckStatus.Checking,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            textProvider.text(
                if (status == KeyCheckStatus.Checking) {
                    ProfileAiSettingsTextKeys.Verifying
                } else {
                    ProfileAiSettingsTextKeys.Verify
                },
            ),
        )
    }
}

@Composable
internal fun KeyCheckMessage(
    status: KeyCheckStatus,
    textProvider: TextProvider,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    when (status) {
        is KeyCheckStatus.Valid ->
            Text(
                text = textProvider.text(ProfileAiSettingsTextKeys.KeyValid),
                color = colors.textSecondary,
                style = typography.bodyMedium,
            )

        is KeyCheckStatus.Invalid ->
            Text(
                text = textProvider.text(ProfileAiSettingsTextKeys.KeyInvalid, status.reason),
                color = colors.textSecondary,
                style = typography.bodyMedium,
            )

        KeyCheckStatus.Idle, KeyCheckStatus.Checking -> Unit
    }
}

@Composable
internal fun ModelField(
    status: KeyCheckStatus,
    label: String,
    options: ImmutableList<String>,
    state: TextFieldState,
) {
    when (status) {
        KeyCheckStatus.Idle, KeyCheckStatus.Checking -> Unit
        is KeyCheckStatus.Valid ->
            if (options.isNotEmpty()) {
                SenseeSelectField(
                    label = label,
                    selected = state.text.toString().ifBlank { null },
                    options = options,
                    optionLabel = { it },
                    onSelect = { state.setTextAndPlaceCursorAtEnd(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = options.firstOrNull().orEmpty(),
                )
            } else {
                SenseeTextField(state = state, accessibilityLabel = label, label = { Text(label) })
            }

        is KeyCheckStatus.Invalid ->
            SenseeTextField(state = state, accessibilityLabel = label, label = { Text(label) })
    }
}

internal fun KeyCheckStatus.hasResult(): Boolean =
    when (this) {
        is KeyCheckStatus.Valid, is KeyCheckStatus.Invalid -> true
        KeyCheckStatus.Idle, KeyCheckStatus.Checking -> false
    }
