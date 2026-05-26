package app.sensee.ui.designSystem.component.button

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
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
import app.sensee.ui.designSystem.component.LocalSenseeMinTouchTargetSize
import app.sensee.ui.designSystem.component.senseeMinTouchTargetSize
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.UnstyledButton

/**
 * Icon button with a decoupled visible size and touch target. The visible decoration
 * (background, border, clipped shape) sizes to [size]; the click area is at least
 * `LocalSenseeMinTouchTargetSize.current`, so a small inline button (`size = 32.dp`)
 * still has a 48dp tap zone for accessibility.
 */
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
    val minTouchTargetSize = LocalSenseeMinTouchTargetSize.current
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
                .senseeMinTouchTargetSize(minTouchTargetSize),
        interactionSource = interactionSource,
        // Indication attaches to the inner Box so the ripple is clipped to the visible shape.
        indication = null,
    ) {
        Box(
            modifier =
                Modifier
                    .size(size)
                    .clip(shape)
                    .background(colors.containerColor(enabled))
                    .indication(interactionSource, LocalIndication.current)
                    .border(
                        width = borderWidth,
                        color = colors.borderColor(enabled),
                        shape = shape,
                    ),
            contentAlignment = Alignment.Center,
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
}
