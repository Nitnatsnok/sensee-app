@file:Suppress("MatchingDeclarationName")

package app.sensee.ui.designSystem.component.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.icons.SoundSampler24px
import org.jetbrains.compose.resources.stringResource
import sensee.shared.ui.design_system.generated.resources.Res
import sensee.shared.ui.design_system.generated.resources.speak_button

public object SenseeSpeakIconButtonDefaults {
    /** Smaller than the standard 40dp icon button so it can sit alongside a word or sentence. */
    public val Size: Dp = 32.dp
    public val IconSize: Dp = 20.dp
}

/**
 * Small inline icon button that triggers text-to-speech for nearby text.
 *
 * Sized smaller than the standard 40dp icon button so it can sit alongside a word or sentence
 * without dominating the line.
 */
@Composable
public fun SenseeSpeakIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = SenseeSpeakIconButtonDefaults.Size,
    iconSize: Dp = SenseeSpeakIconButtonDefaults.IconSize,
    contentColor: Color = Color.Unspecified,
) {
    val label = stringResource(Res.string.speak_button)
    SenseeIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = SenseeIconButtonDefaults.colors(content = contentColor),
        size = size,
        iconSize = iconSize,
        accessibilityLabel = label,
        icon = {
            SenseeIcon(
                imageVector = SoundSampler24px,
                contentDescription = null,
            )
        },
    )
}
