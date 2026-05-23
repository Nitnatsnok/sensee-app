package app.sensee.ui.designSystem.component.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.sensee.ui.designSystem.component.button.SenseeIconButton
import app.sensee.ui.designSystem.component.button.SenseeIconButtonDefaults
import app.sensee.ui.designSystem.component.senseeMinTouchTargetSize
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

/**
 * Selectable destination item used inside both [SenseeBottomNavigationBar] and
 * [SenseeNavigationRail]. Its visual form is chosen by the surrounding container through
 * [LocalSenseeNavigationItemLayout] — icon-over-label in the bottom bar, icon-only in a
 * collapsed rail, and icon-plus-label in an expanded rail — so call sites never thread it.
 *
 * `shapes.large` (fixed 16dp corners) instead of `shapes.circle` keeps the click pill a
 * uniform shape regardless of label width.
 */
@Composable
public fun SenseeNavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: SenseeNavigationItemColors = SenseeNavigationDefaults.itemColors(),
) {
    val spacing = SenseeTheme.spacing
    val typography = SenseeTheme.typography
    val shapes = SenseeTheme.shapes
    val itemLayout = LocalSenseeNavigationItemLayout.current
    val iconTint = if (selected) colors.selectedIcon else colors.unselectedIcon
    val labelColor = if (selected) colors.selectedLabel else colors.unselectedLabel

    val base =
        modifier
            .senseeMinTouchTargetSize()
            .clip(shapes.large)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)

    when (itemLayout) {
        SenseeNavigationItemLayout.RailExpanded,
        SenseeNavigationItemLayout.RailCollapsed,
        -> {
            val isExpanded = itemLayout == SenseeNavigationItemLayout.RailExpanded
            // Fixed-width icon slot at the row start. In the collapsed state the slot fills
            // the entire item width with the icon centred. In the expanded state the slot
            // shrinks to (leading-gap + icon) and right-aligns the icon, so the icon's
            // absolute X stays put across the collapse/expand animation (matching the
            // rail's `action` slot) while the trailing dead space between icon and label
            // collapses to an explicit gap. Label uses natural width (no weight) so the
            // row's intrinsic max width reports the actual label size, letting the rail's
            // column size to content via `IntrinsicSize.Max`.
            val collapsedInnerWidth = SenseeTheme.layout.navigationRailWidth - spacing.small * 2
            val iconLeadingSpace = (collapsedInnerWidth - SenseeNavigationDefaults.ItemIconSize) / 2
            val iconSlotWidth =
                if (isExpanded) {
                    iconLeadingSpace + SenseeNavigationDefaults.ItemIconSize
                } else {
                    collapsedInnerWidth
                }
            // Mirrors the icon's leading offset so the trailing gap (label → rail edge)
            // matches the leading gap (rail edge → icon).
            val labelEndPadding = iconLeadingSpace
            Row(
                modifier =
                    base
                        .fillMaxWidth()
                        .padding(vertical = spacing.small)
                        .then(if (isExpanded) Modifier.padding(end = labelEndPadding) else Modifier)
                        // Icon-only: the label carries no visible glyph, so expose it to
                        // accessibility explicitly (the selectable adds Role.Tab + selected).
                        .semantics { if (!isExpanded) contentDescription = label },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.width(iconSlotWidth),
                    contentAlignment = if (isExpanded) Alignment.CenterEnd else Alignment.Center,
                ) {
                    SenseeNavigationIcon(icon = icon, tint = iconTint)
                }
                if (isExpanded) {
                    Spacer(Modifier.width(spacing.medium))
                    Text(
                        text = label,
                        color = labelColor,
                        style = typography.labelLarge,
                        singleLine = true,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        SenseeNavigationItemLayout.BottomBar ->
            Column(
                modifier = base.padding(all = spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            ) {
                SenseeNavigationIcon(icon = icon, tint = iconTint)
                Text(
                    text = label,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    style = typography.labelMedium,
                    singleLine = true,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

/**
 * Filled circular action button placed in a navigation container — typically a `+` button
 * inviting the user to create a new vocabulary entry. Same visual contract in both
 * [SenseeBottomNavigationBar] and [SenseeNavigationRail].
 */
@Composable
public fun SenseeNavigationActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    SenseeIconButton(
        onClick = onClick,
        colors = SenseeIconButtonDefaults.filledColors(),
        size = SenseeNavigationDefaults.ActionButtonSize,
        iconSize = SenseeNavigationDefaults.ActionButtonIconSize,
        icon = { SenseeNavigationIcon(icon = icon, contentDescription = contentDescription) },
        modifier = modifier,
    )
}
