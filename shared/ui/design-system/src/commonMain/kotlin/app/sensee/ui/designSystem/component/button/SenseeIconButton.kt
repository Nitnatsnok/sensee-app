package app.sensee.ui.designSystem.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.UnstyledButton
import com.composeunstyled.minimumInteractiveComponentSize

@Composable
public fun SenseeIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SenseeIconButtonColors = SenseeIconButtonDefaults.colors(),
    size: Dp = SenseeIconButtonDefaults.Size,
    iconSize: Dp = SenseeIconButtonDefaults.IconSize,
    accessibilityLabel: String? = null,
    shape: Shape = CircleShape,
    borderWidth: Dp = SenseeIconButtonDefaults.BorderWidth,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val accessibilityModifier =
        if (accessibilityLabel == null) {
            Modifier
        } else {
            Modifier.semantics { contentDescription = accessibilityLabel }
        }
    UnstyledButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        modifier =
            modifier
                .then(accessibilityModifier)
                .minimumInteractiveComponentSize()
                .size(size)
                .clip(shape)
                .background(colors.containerColor(enabled))
                .border(
                    width = borderWidth,
                    color = colors.borderColor(enabled),
                    shape = shape,
                ),
        interactionSource = interactionSource,
    ) {
        ProvideContentColor(colors.contentColor(enabled)) {
            Box(
                modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
        }
    }
}
