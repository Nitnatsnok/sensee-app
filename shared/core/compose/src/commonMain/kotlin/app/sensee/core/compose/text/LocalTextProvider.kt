package app.sensee.core.compose.text

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import app.sensee.core.presentation.text.DefaultCommonTextProvider
import app.sensee.core.presentation.text.TextProvider

public val LocalTextProvider: ProvidableCompositionLocal<TextProvider> =
    staticCompositionLocalOf { DefaultCommonTextProvider }
