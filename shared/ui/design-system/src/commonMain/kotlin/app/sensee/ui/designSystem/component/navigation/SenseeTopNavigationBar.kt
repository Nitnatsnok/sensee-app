package app.sensee.ui.designSystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics

/**
 * Horizontal navigation bar pinned to the top edge. Counterpart of [SenseeBottomNavigationBar]
 * and [SenseeNavigationRail] for layouts that want a web-style top bar — destination items
 * pack from the start; the optional [action] slot is pushed to the trailing edge. The bar
 * occupies real layout space, so content below it reflows; it is not an overlay.
 *
 * The background and divider span the full width of the host, but the items and action sit
 * inside a bounded inner frame (capped at [SenseeAdaptiveLayoutMetrics.contentMaxWidth] and
 * horizontally centred), so the items align with the left edge of the content frame below
 * the bar, and the action aligns with the content's right edge — neither flies out to the
 * window edges on wide displays.
 */
@Composable
public fun SenseeTopNavigationBar(
    modifier: Modifier = Modifier,
    colors: SenseeNavigationContainerColors = SenseeNavigationDefaults.containerColors(),
    layoutMetrics: SenseeAdaptiveLayoutMetrics =
        LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics(),
    action: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val layout = SenseeTheme.layout
    val spacing = SenseeTheme.spacing

    Box(
        modifier =
            modifier
                .background(colors.container)
                .statusBarsPadding()
                .height(layout.topNavigationHeight)
                .selectableGroup()
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color = colors.divider,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = SenseeNavigationDefaults.DividerThickness.toPx(),
                    )
                }.padding(horizontal = layoutMetrics.screenHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .widthIn(max = layoutMetrics.contentMaxWidth)
                    .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(
                LocalSenseeNavigationItemLayout provides SenseeNavigationItemLayout.TopBar,
            ) {
                content()
            }
            if (action != null) {
                Spacer(Modifier.weight(1f))
                action()
            }
        }
    }
}
