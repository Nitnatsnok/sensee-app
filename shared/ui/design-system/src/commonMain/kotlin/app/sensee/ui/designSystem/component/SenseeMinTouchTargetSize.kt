package app.sensee.ui.designSystem.component

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Floor on a component's tap-target size. Replaces composeunstyled's
 * `minimumInteractiveComponentSize` (deprecated in 2.3.0, removed in 3.0) —
 * the library now leaves this up to each design system.
 *
 * Default is [SenseeMinTouchTargetSize] = 48dp, matching the Material 3 and
 * Android accessibility recommendation for primary touch targets.
 */
public fun Modifier.senseeMinTouchTargetSize(size: Dp = SenseeMinTouchTargetSize): Modifier =
    sizeIn(minWidth = size, minHeight = size)

public val SenseeMinTouchTargetSize: Dp = 48.dp
