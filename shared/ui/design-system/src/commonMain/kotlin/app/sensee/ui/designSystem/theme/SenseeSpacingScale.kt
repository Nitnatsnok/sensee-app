package app.sensee.ui.designSystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.theme.ThemeToken

/**
 * Spacing scale used throughout Sensee. Theme-independent — same values in light and dark.
 * Multipliers roughly follow a 4-pt grid (4 / 8 / 12 / 16 / 24 / 32 / 48).
 */
internal val SenseeSpacingScale: Map<ThemeToken<Dp>, Dp> =
    mapOf(
        SenseeSpacingTokens.none to 0.dp,
        SenseeSpacingTokens.extraSmall to 4.dp,
        SenseeSpacingTokens.small to 8.dp,
        SenseeSpacingTokens.medium to 12.dp,
        SenseeSpacingTokens.large to 16.dp,
        SenseeSpacingTokens.extraLarge to 24.dp,
        SenseeSpacingTokens.doubleExtraLarge to 32.dp,
        SenseeSpacingTokens.tripleExtraLarge to 48.dp,
    )
