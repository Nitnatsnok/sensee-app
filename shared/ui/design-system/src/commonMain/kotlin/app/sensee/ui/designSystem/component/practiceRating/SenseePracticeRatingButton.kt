package app.sensee.ui.designSystem.component.practiceRating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.minimumInteractiveComponentSize

/**
 * A rating button used in the practice deck swipe-rating row.
 *
 * The [icon] expresses the rating semantically (e.g. a sentiment face). The optional
 * [directionIcon] is a small arrow that hints at the swipe direction wired to this rating
 * (e.g. an arrow pointing left for "swipe left to mark Again"). The colored container conveys
 * rating severity through the danger / warning / success / info palette.
 *
 * [colors] is intentionally required with no default: each rating maps to a specific severity
 * palette (`SenseePracticeRatingButtonDefaults.againColors()` / `hardColors()` / `goodColors()` /
 * `easyColors()`) and there is no meaningful neutral fallback, so the caller must choose one.
 */
@Composable
public fun SenseePracticeRatingButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    colors: SenseePracticeRatingButtonColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    directionIcon: ImageVector? = null,
    shape: Shape = SenseePracticeRatingButtonDefaults.Shape,
    minWidth: Dp = SenseePracticeRatingButtonDefaults.MinWidth,
    minHeight: Dp = SenseePracticeRatingButtonDefaults.MinHeight,
    iconSize: Dp = SenseePracticeRatingButtonDefaults.IconSize,
    directionIconSize: Dp = SenseePracticeRatingButtonDefaults.DirectionIconSize,
) {
    val spacing = SenseeTheme.spacing
    val typography = SenseeTheme.typography

    UnstyledButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding =
            PaddingValues(
                horizontal = spacing.small,
                vertical = spacing.small,
            ),
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .defaultMinSize(minWidth = minWidth, minHeight = minHeight)
                .clip(shape)
                .background(colors.containerColor(enabled)),
    ) {
        ProvideContentColor(colors.contentColor(enabled)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            ) {
                if (directionIcon != null) {
                    SenseeIcon(
                        imageVector = directionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(directionIconSize),
                    )
                }
                SenseeIcon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                )
                Text(
                    text = label,
                    style = typography.labelMedium,
                )
            }
        }
    }
}
