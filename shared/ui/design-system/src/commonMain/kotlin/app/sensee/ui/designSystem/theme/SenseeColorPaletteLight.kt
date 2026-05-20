package app.sensee.ui.designSystem.theme

import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ThemeToken

/**
 * Light-theme color values. Sensee absorbs the full Material 3 token set but keeps its own
 * token naming — see [SenseeColorTokens]. Designer-supplied M3 swatches; pair with the dark
 * map in [SenseeColorPaletteDark] (file-split so both palettes can be diffed side-by-side
 * when colour decisions are being reviewed). `*-fixed` tokens are intentionally identical in
 * both palettes; they're for elements that shouldn't shift on a theme switch.
 */
internal val SenseeColorPaletteLight: Map<ThemeToken<Color>, Color> =
    mapOf(
        SenseeColorTokens.background to Color(0xFFFDF7FF),
        SenseeColorTokens.surface to Color(0xFFFDF7FF),
        SenseeColorTokens.surfaceBright to Color(0xFFFDF7FF),
        SenseeColorTokens.surfaceDim to Color(0xFFDFD6ED),
        SenseeColorTokens.surfaceContainerLowest to Color(0xFFFFFFFF),
        SenseeColorTokens.surfaceContainerLow to Color(0xFFF8F1FF),
        SenseeColorTokens.surfaceContainer to Color(0xFFF3EAFF),
        SenseeColorTokens.surfaceContainerHigh to Color(0xFFEDE4FB),
        SenseeColorTokens.surfaceContainerHighest to Color(0xFFE7DFF6),
        SenseeColorTokens.surfaceTint to Color(0xFF534BCD),
        SenseeColorTokens.textPrimary to Color(0xFF1D1929),
        SenseeColorTokens.textSecondary to Color(0xFF474554),
        SenseeColorTokens.textMuted to Color(0xFF777585),
        SenseeColorTokens.border to Color(0xFFC8C4D6),
        SenseeColorTokens.divider to Color(0xFFDFD6ED),
        SenseeColorTokens.inverseSurface to Color(0xFF322E3F),
        SenseeColorTokens.inverseTextPrimary to Color(0xFFF5EEFF),
        SenseeColorTokens.inverseAccent to Color(0xFFC4C0FF),
        SenseeColorTokens.accent to Color(0xFF0F0062),
        SenseeColorTokens.textOnAccent to Color(0xFFFFFFFF),
        SenseeColorTokens.accentContainer to Color(0xFF1F009D),
        SenseeColorTokens.textOnAccentContainer to Color(0xFF8D87FF),
        SenseeColorTokens.accentFixed to Color(0xFFE3DFFF),
        SenseeColorTokens.accentFixedDim to Color(0xFFC4C0FF),
        SenseeColorTokens.textOnAccentFixed to Color(0xFF110068),
        SenseeColorTokens.textOnAccentFixedVariant to Color(0xFF3B2FB4),
        SenseeColorTokens.accentSubtle to Color(0xFF1D005A),
        SenseeColorTokens.textOnAccentSubtle to Color(0xFFFFFFFF),
        SenseeColorTokens.accentSubtleContainer to Color(0xFF340090),
        SenseeColorTokens.textOnAccentSubtleContainer to Color(0xFF9E82FE),
        SenseeColorTokens.accentSubtleFixed to Color(0xFFE7DEFF),
        SenseeColorTokens.accentSubtleFixedDim to Color(0xFFCDBDFF),
        SenseeColorTokens.textOnAccentSubtleFixed to Color(0xFF20005F),
        SenseeColorTokens.textOnAccentSubtleFixedVariant to Color(0xFF4D2CA8),
        SenseeColorTokens.warning to Color(0xFF8B5000),
        SenseeColorTokens.textOnWarning to Color(0xFFFFFFFF),
        SenseeColorTokens.warningContainer to Color(0xFFFF9800),
        SenseeColorTokens.textOnWarningContainer to Color(0xFF653900),
        SenseeColorTokens.warningFixed to Color(0xFFFFDCBE),
        SenseeColorTokens.warningFixedDim to Color(0xFFFFB870),
        SenseeColorTokens.textOnWarningFixed to Color(0xFF2C1600),
        SenseeColorTokens.textOnWarningFixedVariant to Color(0xFF693C00),
        SenseeColorTokens.danger to Color(0xFFBA1A1A),
        SenseeColorTokens.textOnDanger to Color(0xFFFFFFFF),
        SenseeColorTokens.dangerContainer to Color(0xFFFFDAD6),
        SenseeColorTokens.textOnDangerContainer to Color(0xFF93000A),
        SenseeColorTokens.success to Color(0xFF2D6A4F),
        SenseeColorTokens.textOnSuccess to Color(0xFFFFFFFF),
        SenseeColorTokens.successContainer to Color(0xFFB7E4C7),
        SenseeColorTokens.textOnSuccessContainer to Color(0xFF122F1F),
        SenseeColorTokens.info to Color(0xFF1A65C4),
        SenseeColorTokens.textOnInfo to Color(0xFFFFFFFF),
        SenseeColorTokens.infoContainer to Color(0xFFC8DCF6),
        SenseeColorTokens.textOnInfoContainer to Color(0xFF0A2C5E),
        SenseeColorTokens.scrim to Color(0xFF1D1929).copy(alpha = 0.60f),
    )
