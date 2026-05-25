package app.sensee.ui.designSystem.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

/**
 * Single-row, low-emphasis status strip. Sits inline above/below content
 * (e.g. "Loading dictionary…" or "Couldn't load — [Retry]") and stays out of
 * the way of the main flow. For full-area centered states use
 * [SenseeStatusBlock] instead.
 *
 * Slot layout (left → right, vertically centered):
 *
 * ```
 *   text                                                actions
 * ```
 *
 * Both slots are optional, but a row with neither is a no-op; callers should
 * collapse the row themselves in such cases.
 */
@Composable
public fun SenseeInlineStatus(
    modifier: Modifier = Modifier,
    text: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = SenseeInlineStatusDefaults.contentPadding(),
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (text != null) {
            Text(
                text = text,
                color = colors.textMuted,
                style = typography.bodySmall,
            )
        }
        if (actions != null) {
            actions()
        }
    }
}

public object SenseeInlineStatusDefaults {
    @Composable
    public fun contentPadding(): PaddingValues {
        val spacing = SenseeTheme.spacing
        return PaddingValues(horizontal = spacing.medium, vertical = spacing.extraSmall)
    }
}
