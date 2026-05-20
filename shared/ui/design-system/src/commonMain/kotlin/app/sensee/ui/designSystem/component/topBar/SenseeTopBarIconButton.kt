package app.sensee.ui.designSystem.component.topBar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.component.button.SenseeIconButton
import app.sensee.ui.designSystem.component.button.SenseeIconButtonDefaults
import com.composeunstyled.LocalContentColor

@Composable
public fun SenseeTopBarIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = LocalContentColor.current,
    accessibilityLabel: String? = null,
) {
    SenseeIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors =
            SenseeIconButtonDefaults.colors(
                content = contentColor,
            ),
        size = 40.dp,
        iconSize = SenseeTopBarDefaults.ActionIconSize,
        accessibilityLabel = accessibilityLabel,
        icon = icon,
    )
}
