package app.sensee.ui.designSystem.component.checkbox

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.icons.CheckBox
import app.sensee.ui.designSystem.icons.CheckBoxOutlineBlank

@Composable
public fun SenseeCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    colors: SenseeCheckboxColors = SenseeCheckboxDefaults.colors(),
) {
    val toggleModifier =
        if (onCheckedChange != null) {
            Modifier.toggleable(value = checked, onValueChange = onCheckedChange)
        } else {
            Modifier
        }

    SenseeIcon(
        imageVector = if (checked) CheckBox else CheckBoxOutlineBlank,
        contentDescription = null,
        tint = if (checked) colors.checkedTint else colors.uncheckedTint,
        modifier =
            modifier
                .size(SenseeCheckboxDefaults.Size)
                .then(toggleModifier),
    )
}
