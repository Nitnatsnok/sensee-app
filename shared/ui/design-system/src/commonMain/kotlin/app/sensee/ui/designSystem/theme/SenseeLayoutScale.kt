package app.sensee.ui.designSystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.theme.ThemeToken

/**
 * Layout-metric values — screen padding and content max-width per adaptive size class, plus
 * fixed surfaces like the navigation rail width and bottom bar height. Theme-independent.
 */
internal val SenseeLayoutScale: Map<ThemeToken<Dp>, Dp> =
    mapOf(
        SenseeLayoutTokens.screenHorizontalPaddingCompact to 16.dp,
        SenseeLayoutTokens.screenHorizontalPaddingMedium to 24.dp,
        SenseeLayoutTokens.screenHorizontalPaddingExpanded to 32.dp,
        SenseeLayoutTokens.screenVerticalPaddingCompact to 16.dp,
        SenseeLayoutTokens.screenVerticalPaddingMedium to 24.dp,
        SenseeLayoutTokens.screenVerticalPaddingExpanded to 32.dp,
        SenseeLayoutTokens.contentMaxWidthCompact to 440.dp,
        SenseeLayoutTokens.contentMaxWidthMedium to 720.dp,
        SenseeLayoutTokens.contentMaxWidthExpanded to 1040.dp,
        SenseeLayoutTokens.paneGap to 24.dp,
        SenseeLayoutTokens.navigationRailWidth to 96.dp,
        SenseeLayoutTokens.bottomNavigationHeight to 72.dp,
    )
