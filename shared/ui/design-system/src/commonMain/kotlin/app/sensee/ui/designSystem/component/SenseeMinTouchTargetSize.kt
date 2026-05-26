package app.sensee.ui.designSystem.component

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public fun Modifier.senseeMinTouchTargetSize(size: Dp = SenseeMinTouchTargetSize): Modifier =
    sizeIn(minWidth = size, minHeight = size)

public val SenseeMinTouchTargetSize: Dp = 48.dp

public val LocalSenseeMinTouchTargetSize: ProvidableCompositionLocal<Dp> =
    staticCompositionLocalOf { SenseeMinTouchTargetSize }
