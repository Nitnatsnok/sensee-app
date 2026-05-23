package app.sensee.ui.designSystem.component.pickerField

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.icons.ChevronRight
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

/**
 * Settings-style picker field — a clickable surface that surfaces the field's
 * [title] (what this field represents) and the current [value] in two text
 * lines, with an optional [leadingIcon], a trailing chevron, and an optional
 * [hint] rendered below the surface as a small caption.
 *
 * Use this when tapping the field opens a separate selection UI (a modal
 * sheet, a side pane, a dedicated screen). For an inline value chooser with
 * a popup, see `SenseeSelectField`.
 */
@Composable
public fun SenseePickerField(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = ChevronRight,
    colors: SenseePickerFieldColors = SenseePickerFieldDefaults.colors(),
    contentPadding: PaddingValues = SenseePickerFieldDefaults.contentPadding(),
) {
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing
    val shape = SenseePickerFieldDefaults.shape()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.container)
                    .border(SenseePickerFieldDefaults.BorderWidth, colors.border, shape)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            if (leadingIcon != null) {
                SenseeIcon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.leadingIcon,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            ) {
                Text(
                    text = title,
                    color = colors.title,
                    style = typography.bodyLarge,
                )
                Text(
                    text = value,
                    color = colors.value,
                    style = typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailingIcon != null) {
                SenseeIcon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = colors.trailingIcon,
                )
            }
        }
        if (hint != null) {
            Text(
                text = hint,
                color = colors.hint,
                style = typography.bodySmall,
                modifier = Modifier.padding(horizontal = spacing.small),
            )
        }
    }
}
