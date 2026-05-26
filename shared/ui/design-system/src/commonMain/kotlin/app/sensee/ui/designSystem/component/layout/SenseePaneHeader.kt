package app.sensee.ui.designSystem.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.topBar.SenseeTopBarIconButton
import app.sensee.ui.designSystem.icons.Close24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.LocalContentColor

/**
 * Title row for a wide-layout detail pane sitting inside a card surface. Layout only —
 * the enclosing card provides background, padding and any status-bar inset.
 */
@Composable
public fun SenseePaneHeader(
    onClose: () -> Unit,
    closeAccessibilityLabel: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = SenseePaneHeaderDefaults.contentPadding(),
    actions: @Composable RowScope.() -> Unit = {},
    title: @Composable () -> Unit,
) {
    val colors = SenseeTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.extraSmall),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
            Box(modifier = Modifier.weight(1f)) {
                title()
            }
        }
        CompositionLocalProvider(LocalContentColor provides colors.textSecondary) {
            actions()
            SenseeTopBarIconButton(
                onClick = onClose,
                accessibilityLabel = closeAccessibilityLabel,
                icon = { SenseeIcon(imageVector = Close24px, contentDescription = null) },
            )
        }
    }
}

public object SenseePaneHeaderDefaults {
    @Composable
    public fun contentPadding(): PaddingValues = PaddingValues(horizontal = SenseeTheme.spacing.small)
}
