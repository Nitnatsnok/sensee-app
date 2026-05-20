package app.sensee.ui.designSystem.component.selectField

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.textField.SenseeTextFieldDefaults
import app.sensee.ui.designSystem.icons.ArrowDownwardAlt24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text
import kotlinx.collections.immutable.ImmutableList

/**
 * A select that opens its options on tap (not a permanent button list). Styled
 * like [app.sensee.ui.designSystem.component.textField.SenseeTextField] so a
 * provider/model/voice chooser reads as a normal field with a chevron.
 *
 * `expanded` is owned internally on purpose: the open/closed state has no meaning
 * outside the field and no caller has needed to drive it, so hoisting it would only
 * add a parameter every call site has to thread for no benefit.
 */
@Composable
public fun <T> SenseeSelectField(
    label: String,
    selected: T?,
    options: ImmutableList<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    colors: SenseeSelectFieldColors = SenseeSelectFieldDefaults.colors(),
) {
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing
    val shape = SenseeTextFieldDefaults.shape()

    var expanded by remember { mutableStateOf(false) }
    var fieldWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        Text(text = label, color = colors.label, style = typography.labelMedium)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { fieldWidthPx = it.width }
                    .defaultMinSize(minHeight = SenseeTextFieldDefaults.MinHeight)
                    .clip(shape)
                    .background(colors.container)
                    .border(SenseeTextFieldDefaults.BorderWidth, colors.border, shape)
                    .semantics(mergeDescendants = true) { role = Role.DropdownList }
                    .clickable(enabled = options.isNotEmpty()) { expanded = true }
                    .padding(horizontal = spacing.large, vertical = spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val shown = selected?.let(optionLabel) ?: placeholder
            Text(
                text = shown,
                color = if (selected != null) colors.content else colors.placeholder,
                style = typography.bodyLarge,
            )
            SenseeIcon(
                imageVector = ArrowDownwardAlt24px,
                contentDescription = null,
                tint = colors.chevron,
            )
        }

        if (expanded) {
            Popup(
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                LazyColumn(
                    modifier =
                        Modifier
                            .width(with(density) { fieldWidthPx.toDp() })
                            .heightIn(max = SenseeSelectFieldDefaults.MaxPopupHeight)
                            .clip(shape)
                            .background(colors.popupContainer)
                            .border(SenseeTextFieldDefaults.BorderWidth, colors.border, shape)
                            .selectableGroup(),
                ) {
                    items(options) { option ->
                        Text(
                            text = optionLabel(option),
                            color = colors.optionContent,
                            style = typography.bodyLarge,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = option == selected,
                                        role = Role.Button,
                                    ) {
                                        onSelect(option)
                                        expanded = false
                                    }.padding(horizontal = spacing.large, vertical = spacing.medium),
                        )
                    }
                }
            }
        }
    }
}
