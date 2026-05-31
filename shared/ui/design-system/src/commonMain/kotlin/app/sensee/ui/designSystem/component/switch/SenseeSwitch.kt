package app.sensee.ui.designSystem.component.switch

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.sensee.ui.designSystem.component.LocalSenseeMinTouchTargetSize
import app.sensee.ui.designSystem.component.senseeMinTouchTargetSize
import app.sensee.ui.designSystem.theme.SenseeStateAlphas
import com.composeunstyled.SwitchThumb
import com.composeunstyled.UnstyledSwitch

/**
 * Stateless on/off switch built on Compose Unstyled's renderless [UnstyledSwitch], which owns the
 * toggle gesture, `Role.Switch` semantics, and the thumb travel. The component renders nothing on
 * its own, so the pill track and thumb circle are drawn here over theme tokens. State is hoisted:
 * the caller owns [checked] and reacts to [onCheckedChange]; pass `onCheckedChange = null` for a
 * read-only switch. Track and thumb colours cross-fade with the checked state.
 *
 * The thumb sits inside a square container the track's height. Its travel is
 * `trackWidth - trackHeight`, and the visible circle is centred in that square, which yields an
 * equal resting gap at both ends without a separate inset.
 *
 * The interactive node expands to [LocalSenseeMinTouchTargetSize] (the drawn 52×32 pill stays
 * centred inside it via [wrapContentSize]), so the toggle hit area meets the design-system minimum
 * even though the visual track is shorter — mirroring the other interactive primitives.
 */
@Composable
public fun SenseeSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    colors: SenseeSwitchColors = SenseeSwitchDefaults.colors(),
) {
    val contentAlpha = if (enabled) 1f else SenseeStateAlphas.DISABLED_CONTENT
    val trackColor by animateColorAsState(
        (if (checked) colors.checkedTrack else colors.uncheckedTrack).copy(alpha = contentAlpha),
    )
    val thumbColor by animateColorAsState(
        (if (checked) colors.checkedThumb else colors.uncheckedThumb).copy(alpha = contentAlpha),
    )

    UnstyledSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier =
            modifier
                .senseeMinTouchTargetSize(LocalSenseeMinTouchTargetSize.current)
                .wrapContentSize()
                .width(SenseeSwitchDefaults.TrackWidth)
                .height(SenseeSwitchDefaults.TrackHeight)
                .clip(CircleShape)
                .background(trackColor),
        enabled = enabled,
    ) {
        SwitchThumb(animationSpec = spring()) {
            Box(
                modifier = Modifier.size(SenseeSwitchDefaults.TrackHeight),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(SenseeSwitchDefaults.ThumbSize)
                            .clip(CircleShape)
                            .background(thumbColor),
                )
            }
        }
    }
}
