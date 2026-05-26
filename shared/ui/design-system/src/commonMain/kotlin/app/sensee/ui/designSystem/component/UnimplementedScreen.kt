package app.sensee.ui.designSystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import sensee.shared.ui.design_system.generated.resources.Res
import sensee.shared.ui.design_system.generated.resources.unimplemented_default_description

@Composable
public fun UnimplementedScreen(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = SenseeTheme.colors
    val spacing = SenseeTheme.spacing
    val typography = SenseeTheme.typography
    val layoutMetrics = LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics()
    val resolvedDescription =
        description ?: stringResource(Res.string.unimplemented_default_description)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    horizontal = layoutMetrics.screenHorizontalPadding,
                    vertical = layoutMetrics.screenVerticalPadding,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            modifier =
                Modifier
                    .widthIn(max = layoutMetrics.contentMaxWidth),
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                style = typography.titleLarge,
            )

            Text(
                text = resolvedDescription,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                style = typography.bodyMedium,
            )

            content()
        }
    }
}
