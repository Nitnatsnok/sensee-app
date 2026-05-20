package app.sensee.ui.designSystem.component.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.button.SenseeIconButton
import app.sensee.ui.designSystem.icons.ArrowLeftAlt24px
import app.sensee.ui.designSystem.icons.ArrowRightAlt24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import org.jetbrains.compose.resources.stringResource
import sensee.shared.ui.design_system.generated.resources.Res
import sensee.shared.ui.design_system.generated.resources.nav_rail_collapse
import sensee.shared.ui.design_system.generated.resources.nav_rail_expand

/**
 * Vertical navigation rail. When [onExpandedChange] is supplied, a leading toggle button lets
 * the user switch between a collapsed icon-only rail and an expanded rail whose items show the
 * full label beside the icon. Collapsed width is fixed by the design tokens; expanded width
 * follows the widest item (icon slot + longest label + end padding), capped by
 * [SenseeNavigationDefaults.RailExpandedMaxWidth] beyond which labels ellipsise rather than
 * grow the rail. The change is smoothed by `animateContentSize`. The rail occupies real
 * layout space — surrounding content reflows around it; it is not an overlay.
 *
 * The [action] slot renders below [content] in a frame the size of the collapsed inner width,
 * so a FAB-like button stays at the same absolute X regardless of expansion state (pin it to
 * the bottom by ending [content] with a `Spacer(Modifier.weight(1f))`).
 */
@Composable
public fun SenseeNavigationRail(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    colors: SenseeNavigationContainerColors = SenseeNavigationDefaults.containerColors(),
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = SenseeTheme.spacing
    val layout = SenseeTheme.layout

    val itemLayout =
        if (expanded) {
            SenseeNavigationItemLayout.RailExpanded
        } else {
            SenseeNavigationItemLayout.RailCollapsed
        }
    val collapsedInnerWidth = layout.navigationRailWidth - spacing.small * 2

    Column(
        modifier =
            modifier
                .background(colors.container)
                .navigationBarsPadding()
                // Order matters: drawWithContent → animateContentSize → width(...).
                // The divider and the rail width must animate together, and the width
                // modifier must sit inside animateContentSize so its imposed size flows
                // through the animation.
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color = colors.divider,
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = SenseeNavigationDefaults.DividerThickness.toPx(),
                    )
                }.animateContentSize()
                .then(
                    if (expanded) {
                        Modifier
                            .width(IntrinsicSize.Max)
                            .widthIn(max = SenseeNavigationDefaults.RailExpandedMaxWidth)
                    } else {
                        Modifier.width(layout.navigationRailWidth)
                    },
                ).selectableGroup()
                .padding(
                    start = spacing.small,
                    top = spacing.medium,
                    end = spacing.small,
                    bottom = spacing.medium,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        if (onExpandedChange != null) {
            SenseeIconButton(
                onClick = { onExpandedChange(!expanded) },
                modifier = Modifier.align(if (expanded) Alignment.End else Alignment.CenterHorizontally),
                icon = {
                    SenseeIcon(
                        imageVector = if (expanded) ArrowLeftAlt24px else ArrowRightAlt24px,
                        contentDescription =
                            stringResource(
                                if (expanded) Res.string.nav_rail_collapse else Res.string.nav_rail_expand,
                            ),
                    )
                },
            )
        }

        // CompositionLocalProvider adds no layout node, so weighted spacers / fillMaxHeight
        // passed by the caller still resolve against this Column.
        CompositionLocalProvider(LocalSenseeNavigationItemLayout provides itemLayout) {
            content()
        }

        if (action != null) {
            // Fixed-width frame pinned to the column start: centring inside it keeps the
            // action button on the same absolute X as the collapsed-state nav-item icons,
            // both when the rail is collapsed (frame == column) and when it expands (frame
            // anchors to start, content stays put while the column grows around it).
            Box(
                modifier =
                    Modifier
                        .align(Alignment.Start)
                        .width(collapsedInnerWidth),
                contentAlignment = Alignment.Center,
            ) {
                action()
            }
        }
    }
}
