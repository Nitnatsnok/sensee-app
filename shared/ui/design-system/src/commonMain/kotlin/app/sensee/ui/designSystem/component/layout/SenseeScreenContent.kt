package app.sensee.ui.designSystem.component.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics

@Composable
public fun SenseeScreenContent(
    modifier: Modifier = Modifier,
    layoutMetrics: SenseeAdaptiveLayoutMetrics =
        LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics(),
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    horizontal = layoutMetrics.screenHorizontalPadding,
                    vertical = layoutMetrics.screenVerticalPadding,
                ),
        contentAlignment = contentAlignment,
    ) {
        SenseeScreenContentFrame(
            layoutMetrics = layoutMetrics,
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

@Composable
public fun SenseeScreenContentFrame(
    modifier: Modifier = Modifier,
    layoutMetrics: SenseeAdaptiveLayoutMetrics =
        LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics(),
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = contentAlignment,
    ) {
        Box(
            modifier =
                Modifier
                    .widthIn(max = layoutMetrics.contentMaxWidth)
                    .fillMaxWidth(),
            content = content,
        )
    }
}
