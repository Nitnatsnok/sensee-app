package app.sensee.ui.designSystem.theme

import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ThemeToken

/**
 * Dark-theme color values. Pair with [SenseeColorPaletteLight] — keep the two files
 * structurally identical so token rows line up when diffing palettes. M3 tone conventions
 * are followed (primary/error/secondary/tertiary in dark mode use inverted "container"
 * tones, etc.). `*-fixed` tokens carry the same values as in the light palette by design —
 * they're meant for elements that shouldn't shift on a theme switch.
 */
internal val SenseeColorPaletteDark: Map<ThemeToken<Color>, Color> =
    mapOf(
        SenseeColorTokens.background to Color(0xFF15131F),
        SenseeColorTokens.surface to Color(0xFF15131F),
        SenseeColorTokens.surfaceBright to Color(0xFF3B3845),
        SenseeColorTokens.surfaceDim to Color(0xFF15131F),
        SenseeColorTokens.surfaceContainerLowest to Color(0xFF0A0816),
        SenseeColorTokens.surfaceContainerLow to Color(0xFF1D1929),
        SenseeColorTokens.surfaceContainer to Color(0xFF221F2D),
        SenseeColorTokens.surfaceContainerHigh to Color(0xFF2C2937),
        SenseeColorTokens.surfaceContainerHighest to Color(0xFF373442),
        SenseeColorTokens.surfaceTint to Color(0xFFC4C0FF),
        SenseeColorTokens.textPrimary to Color(0xFFE6E0F4),
        SenseeColorTokens.textSecondary to Color(0xFFC8C4D6),
        SenseeColorTokens.textMuted to Color(0xFF918FA0),
        SenseeColorTokens.border to Color(0xFF474554),
        SenseeColorTokens.divider to Color(0xFF322E3F),
        SenseeColorTokens.inverseSurface to Color(0xFFE6E0F4),
        SenseeColorTokens.inverseTextPrimary to Color(0xFF322E3F),
        SenseeColorTokens.inverseAccent to Color(0xFF534BCD),
        SenseeColorTokens.accent to Color(0xFFC4C0FF),
        SenseeColorTokens.textOnAccent to Color(0xFF1F009D),
        SenseeColorTokens.accentContainer to Color(0xFF3B2FB4),
        SenseeColorTokens.textOnAccentContainer to Color(0xFFE3DFFF),
        SenseeColorTokens.accentFixed to Color(0xFFE3DFFF),
        SenseeColorTokens.accentFixedDim to Color(0xFFC4C0FF),
        SenseeColorTokens.textOnAccentFixed to Color(0xFF110068),
        SenseeColorTokens.textOnAccentFixedVariant to Color(0xFF3B2FB4),
        SenseeColorTokens.accentSubtle to Color(0xFFCDBDFF),
        SenseeColorTokens.textOnAccentSubtle to Color(0xFF340090),
        SenseeColorTokens.accentSubtleContainer to Color(0xFF4D2CA8),
        SenseeColorTokens.textOnAccentSubtleContainer to Color(0xFFE7DEFF),
        SenseeColorTokens.accentSubtleFixed to Color(0xFFE7DEFF),
        SenseeColorTokens.accentSubtleFixedDim to Color(0xFFCDBDFF),
        SenseeColorTokens.textOnAccentSubtleFixed to Color(0xFF20005F),
        SenseeColorTokens.textOnAccentSubtleFixedVariant to Color(0xFF4D2CA8),
        SenseeColorTokens.warning to Color(0xFFFFB870),
        SenseeColorTokens.textOnWarning to Color(0xFF4A2700),
        SenseeColorTokens.warningContainer to Color(0xFF693C00),
        SenseeColorTokens.textOnWarningContainer to Color(0xFFFFDCBE),
        SenseeColorTokens.warningFixed to Color(0xFFFFDCBE),
        SenseeColorTokens.warningFixedDim to Color(0xFFFFB870),
        SenseeColorTokens.textOnWarningFixed to Color(0xFF2C1600),
        SenseeColorTokens.textOnWarningFixedVariant to Color(0xFF693C00),
        SenseeColorTokens.danger to Color(0xFFFFB4AB),
        SenseeColorTokens.textOnDanger to Color(0xFF690005),
        SenseeColorTokens.dangerContainer to Color(0xFF93000A),
        SenseeColorTokens.textOnDangerContainer to Color(0xFFFFDAD6),
        SenseeColorTokens.success to Color(0xFF99D49E),
        SenseeColorTokens.textOnSuccess to Color(0xFF0A2914),
        SenseeColorTokens.successContainer to Color(0xFF15402A),
        SenseeColorTokens.textOnSuccessContainer to Color(0xFFB7E4C7),
        SenseeColorTokens.info to Color(0xFF9CC3EF),
        SenseeColorTokens.textOnInfo to Color(0xFF062349),
        SenseeColorTokens.infoContainer to Color(0xFF0E3771),
        SenseeColorTokens.textOnInfoContainer to Color(0xFFC8DCF6),
        SenseeColorTokens.scrim to Color(0xFF000000).copy(alpha = 0.70f),
    )
