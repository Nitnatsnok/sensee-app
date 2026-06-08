package app.sensee.ui.designSystem.component.badge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.ui.designSystem.theme.SenseeTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * A wrapping row of [SenseeBadge]s built from [labels]: chips flow onto new lines as the
 * width runs out, with the theme's `extraSmall` rhythm between them. Pass [colors] to pick a
 * single badge variant for the whole group (defaults to the neutral outlined chip).
 *
 * For per-chip variants or non-text chips, compose [SenseeBadge] inside your own `FlowRow`.
 */
@Composable
public fun SenseeBadgeFlow(
    labels: ImmutableList<String>,
    modifier: Modifier = Modifier,
    colors: SenseeBadgeColors = SenseeBadgeDefaults.neutralColors(),
) {
    val spacing = SenseeTheme.spacing
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        labels.forEach { label -> SenseeBadge(text = label, colors = colors) }
    }
}
