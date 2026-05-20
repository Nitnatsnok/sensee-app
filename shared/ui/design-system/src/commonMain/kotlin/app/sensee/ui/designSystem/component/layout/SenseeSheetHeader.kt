package app.sensee.ui.designSystem.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.LocalContentColor

/**
 * Title bar for a `SenseeModalBottomSheet` (or any sheet-like surface).
 *
 * Distinct from `SenseeTopBar` / `SenseeImmersiveTopBar`: those are window-edge app bars and
 * handle status-bar insets themselves; a sheet sits inside an already-inset surface, so this
 * header just renders a `[title][actions]` row without any window-inset assumptions or fixed
 * height. Vertical sizing is intrinsic to the title content.
 */
@Composable
public fun SenseeSheetHeader(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = SenseeTheme.colors
    val spacing = SenseeTheme.spacing

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
            Row(modifier = Modifier.weight(1f)) {
                title()
            }
        }
        CompositionLocalProvider(LocalContentColor provides colors.textSecondary) {
            actions()
        }
    }
}
