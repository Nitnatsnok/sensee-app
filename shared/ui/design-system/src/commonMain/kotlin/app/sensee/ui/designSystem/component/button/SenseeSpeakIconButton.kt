@file:Suppress("MatchingDeclarationName")

package app.sensee.ui.designSystem.component.button

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.icons.SoundSampler24px
import com.composeunstyled.LocalContentColor
import org.jetbrains.compose.resources.stringResource
import sensee.shared.ui.design_system.generated.resources.Res
import sensee.shared.ui.design_system.generated.resources.speak_button
import sensee.shared.ui.design_system.generated.resources.stop_speaking_button

public object SenseeSpeakIconButtonDefaults {
    /** Smaller than the standard 40dp icon button so it can sit alongside a word or sentence. */
    public val Size: Dp = 32.dp
    public val IconSize: Dp = 20.dp
    internal val LoadingStrokeWidth: Dp = 2.dp
}

public enum class SenseeSpeakIconButtonState {
    Idle,
    Loading,
    Speaking,
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
    state: SenseeSpeakIconButtonState = SenseeSpeakIconButtonState.Idle,
) {
    val label =
        stringResource(
            when (state) {
                SenseeSpeakIconButtonState.Loading,
                SenseeSpeakIconButtonState.Speaking,
                -> Res.string.stop_speaking_button

                SenseeSpeakIconButtonState.Idle -> Res.string.speak_button
            },
        )
    SenseeIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = SenseeIconButtonDefaults.colors(content = contentColor),
        size = size,
        iconSize = iconSize,
        accessibilityLabel = label,
        icon = {
            SenseeSpeakIconButtonContent(state = state)
        },
    )
}

@Composable
private fun SenseeSpeakIconButtonContent(state: SenseeSpeakIconButtonState) {
    when (state) {
        SenseeSpeakIconButtonState.Loading -> SenseeSpeakIconButtonLoading()
        SenseeSpeakIconButtonState.Speaking -> SenseeSpeakIconButtonPulsingIcon()
        SenseeSpeakIconButtonState.Idle ->
            SenseeIcon(
                imageVector = SoundSampler24px,
                contentDescription = null,
            )
    }
}

@Composable
private fun SenseeSpeakIconButtonLoading() {
    val color = LocalContentColor.current
    val transition = rememberInfiniteTransition(label = "speakButtonLoading")
    val rotation =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = LinearEasing),
                ),
            label = "speakButtonLoadingRotation",
        )
    val strokeWidth = SenseeSpeakIconButtonDefaults.LoadingStrokeWidth

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        val strokeInset = strokeWidth.toPx() / 2f
        val arcSize =
            Size(
                width = size.width - strokeInset * 2f,
                height = size.height - strokeInset * 2f,
            )
        drawArc(
            color = color.copy(alpha = 0.28f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(strokeInset, strokeInset),
            size = arcSize,
            style = stroke,
        )
        drawArc(
            color = color,
            startAngle = rotation.value,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(strokeInset, strokeInset),
            size = arcSize,
            style = stroke,
        )
    }
}

@Composable
private fun SenseeSpeakIconButtonPulsingIcon() {
    val transition = rememberInfiniteTransition(label = "speakButtonPulse")
    val scale =
        transition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.12f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 620, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "speakButtonPulseScale",
        )
    val alpha =
        transition.animateFloat(
            initialValue = 0.68f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 620, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "speakButtonPulseAlpha",
        )

    SenseeIcon(
        imageVector = SoundSampler24px,
        contentDescription = null,
        modifier =
            Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
    )
}
