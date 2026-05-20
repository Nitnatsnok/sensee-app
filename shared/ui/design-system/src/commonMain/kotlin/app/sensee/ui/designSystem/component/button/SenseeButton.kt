package app.sensee.ui.designSystem.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.UnstyledButton
import com.composeunstyled.minimumInteractiveComponentSize

@Composable
public fun SenseeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SenseeButtonColors = SenseeButtonColors.default(),
    shape: Shape = SenseeButtonDefaults.shape(),
    contentPadding: PaddingValues = SenseeButtonDefaults.contentPadding(),
    borderWidth: Dp = SenseeButtonDefaults.BorderWidth,
    minWidth: Dp = SenseeButtonDefaults.MinWidth,
    minHeight: Dp = SenseeButtonDefaults.MinHeight,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit,
) {
    UnstyledButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = contentPadding,
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .defaultMinSize(
                    minWidth = minWidth,
                    minHeight = minHeight,
                ).clip(shape)
                .background(colors.containerColor(enabled))
                .border(
                    width = borderWidth,
                    color = colors.borderColor(enabled),
                    shape = shape,
                ),
        interactionSource = interactionSource,
    ) {
        ProvideContentColor(colors.contentColor(enabled)) {
            Row(
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = verticalAlignment,
                content = content,
            )
        }
    }
}
