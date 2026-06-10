package app.sensee.feature.home.presentation.impl.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback

@Composable
internal fun rememberHomeTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultHomeTextProvider.withFallback(parent) }
}
