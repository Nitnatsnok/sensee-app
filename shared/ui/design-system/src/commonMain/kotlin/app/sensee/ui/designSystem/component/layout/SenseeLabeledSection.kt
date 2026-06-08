package app.sensee.ui.designSystem.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

/**
 * A small caption [label] above its [content] — the labeled-field/section idiom used by
 * detail screens and grouped settings rows. The label uses the muted `labelMedium` caption
 * style; the content sits directly beneath it in a [ColumnScope].
 */
@Composable
public fun SenseeLabeledSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.extraSmall),
    ) {
        Text(text = label, color = colors.textSecondary, style = typography.labelMedium)
        content()
    }
}
