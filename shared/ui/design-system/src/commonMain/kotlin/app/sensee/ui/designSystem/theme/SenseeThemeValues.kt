package app.sensee.ui.designSystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.composeunstyled.theme.Theme

public data class SenseeColors(
    val background: Color,
    val surface: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceTint: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val divider: Color,
    val inverseSurface: Color,
    val inverseTextPrimary: Color,
    val inverseAccent: Color,
    val accent: Color,
    val textOnAccent: Color,
    val accentContainer: Color,
    val textOnAccentContainer: Color,
    val accentFixed: Color,
    val accentFixedDim: Color,
    val textOnAccentFixed: Color,
    val textOnAccentFixedVariant: Color,
    val accentSubtle: Color,
    val textOnAccentSubtle: Color,
    val accentSubtleContainer: Color,
    val textOnAccentSubtleContainer: Color,
    val accentSubtleFixed: Color,
    val accentSubtleFixedDim: Color,
    val textOnAccentSubtleFixed: Color,
    val textOnAccentSubtleFixedVariant: Color,
    val warning: Color,
    val textOnWarning: Color,
    val warningContainer: Color,
    val textOnWarningContainer: Color,
    val warningFixed: Color,
    val warningFixedDim: Color,
    val textOnWarningFixed: Color,
    val textOnWarningFixedVariant: Color,
    val danger: Color,
    val textOnDanger: Color,
    val dangerContainer: Color,
    val textOnDangerContainer: Color,
    val success: Color,
    val textOnSuccess: Color,
    val successContainer: Color,
    val textOnSuccessContainer: Color,
    val info: Color,
    val textOnInfo: Color,
    val infoContainer: Color,
    val textOnInfoContainer: Color,
    val scrim: Color,
)

public data class SenseeSpacing(
    val none: Dp,
    val extraSmall: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val extraLarge: Dp,
    val doubleExtraLarge: Dp,
    val tripleExtraLarge: Dp,
)

public data class SenseeLayout(
    val screenHorizontalPaddingCompact: Dp,
    val screenHorizontalPaddingMedium: Dp,
    val screenHorizontalPaddingExpanded: Dp,
    val screenVerticalPaddingCompact: Dp,
    val screenVerticalPaddingMedium: Dp,
    val screenVerticalPaddingExpanded: Dp,
    val contentMaxWidthCompact: Dp,
    val contentMaxWidthMedium: Dp,
    val contentMaxWidthExpanded: Dp,
    val paneGap: Dp,
    val navigationRailWidth: Dp,
    val bottomNavigationHeight: Dp,
)

public data class SenseeShapes(
    val extraSmall: CornerBasedShape,
    val small: CornerBasedShape,
    val medium: CornerBasedShape,
    val large: CornerBasedShape,
    val extraLarge: CornerBasedShape,
    val circle: CornerBasedShape = CircleShape,
)

public data class SenseeTextStyles(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)

public object SenseeTheme {
    public val colors: SenseeColors
        @Composable get() {
            val palette = Theme[SenseeThemeProperties.colors]
            return SenseeColors(
                background = palette[SenseeColorTokens.background],
                surface = palette[SenseeColorTokens.surface],
                surfaceBright = palette[SenseeColorTokens.surfaceBright],
                surfaceDim = palette[SenseeColorTokens.surfaceDim],
                surfaceContainerLowest = palette[SenseeColorTokens.surfaceContainerLowest],
                surfaceContainerLow = palette[SenseeColorTokens.surfaceContainerLow],
                surfaceContainer = palette[SenseeColorTokens.surfaceContainer],
                surfaceContainerHigh = palette[SenseeColorTokens.surfaceContainerHigh],
                surfaceContainerHighest = palette[SenseeColorTokens.surfaceContainerHighest],
                surfaceTint = palette[SenseeColorTokens.surfaceTint],
                textPrimary = palette[SenseeColorTokens.textPrimary],
                textSecondary = palette[SenseeColorTokens.textSecondary],
                textMuted = palette[SenseeColorTokens.textMuted],
                border = palette[SenseeColorTokens.border],
                divider = palette[SenseeColorTokens.divider],
                inverseSurface = palette[SenseeColorTokens.inverseSurface],
                inverseTextPrimary = palette[SenseeColorTokens.inverseTextPrimary],
                inverseAccent = palette[SenseeColorTokens.inverseAccent],
                accent = palette[SenseeColorTokens.accent],
                textOnAccent = palette[SenseeColorTokens.textOnAccent],
                accentContainer = palette[SenseeColorTokens.accentContainer],
                textOnAccentContainer = palette[SenseeColorTokens.textOnAccentContainer],
                accentFixed = palette[SenseeColorTokens.accentFixed],
                accentFixedDim = palette[SenseeColorTokens.accentFixedDim],
                textOnAccentFixed = palette[SenseeColorTokens.textOnAccentFixed],
                textOnAccentFixedVariant = palette[SenseeColorTokens.textOnAccentFixedVariant],
                accentSubtle = palette[SenseeColorTokens.accentSubtle],
                textOnAccentSubtle = palette[SenseeColorTokens.textOnAccentSubtle],
                accentSubtleContainer = palette[SenseeColorTokens.accentSubtleContainer],
                textOnAccentSubtleContainer = palette[SenseeColorTokens.textOnAccentSubtleContainer],
                accentSubtleFixed = palette[SenseeColorTokens.accentSubtleFixed],
                accentSubtleFixedDim = palette[SenseeColorTokens.accentSubtleFixedDim],
                textOnAccentSubtleFixed = palette[SenseeColorTokens.textOnAccentSubtleFixed],
                textOnAccentSubtleFixedVariant =
                    palette[SenseeColorTokens.textOnAccentSubtleFixedVariant],
                warning = palette[SenseeColorTokens.warning],
                textOnWarning = palette[SenseeColorTokens.textOnWarning],
                warningContainer = palette[SenseeColorTokens.warningContainer],
                textOnWarningContainer = palette[SenseeColorTokens.textOnWarningContainer],
                warningFixed = palette[SenseeColorTokens.warningFixed],
                warningFixedDim = palette[SenseeColorTokens.warningFixedDim],
                textOnWarningFixed = palette[SenseeColorTokens.textOnWarningFixed],
                textOnWarningFixedVariant = palette[SenseeColorTokens.textOnWarningFixedVariant],
                danger = palette[SenseeColorTokens.danger],
                dangerContainer = palette[SenseeColorTokens.dangerContainer],
                textOnDanger = palette[SenseeColorTokens.textOnDanger],
                textOnDangerContainer = palette[SenseeColorTokens.textOnDangerContainer],
                success = palette[SenseeColorTokens.success],
                successContainer = palette[SenseeColorTokens.successContainer],
                textOnSuccess = palette[SenseeColorTokens.textOnSuccess],
                textOnSuccessContainer = palette[SenseeColorTokens.textOnSuccessContainer],
                info = palette[SenseeColorTokens.info],
                infoContainer = palette[SenseeColorTokens.infoContainer],
                textOnInfo = palette[SenseeColorTokens.textOnInfo],
                textOnInfoContainer = palette[SenseeColorTokens.textOnInfoContainer],
                scrim = palette[SenseeColorTokens.scrim],
            )
        }

    public val spacing: SenseeSpacing
        @Composable get() {
            val spacing = Theme[SenseeThemeProperties.spacing]
            return SenseeSpacing(
                none = spacing[SenseeSpacingTokens.none],
                extraSmall = spacing[SenseeSpacingTokens.extraSmall],
                small = spacing[SenseeSpacingTokens.small],
                medium = spacing[SenseeSpacingTokens.medium],
                large = spacing[SenseeSpacingTokens.large],
                extraLarge = spacing[SenseeSpacingTokens.extraLarge],
                doubleExtraLarge = spacing[SenseeSpacingTokens.doubleExtraLarge],
                tripleExtraLarge = spacing[SenseeSpacingTokens.tripleExtraLarge],
            )
        }

    public val layout: SenseeLayout
        @Composable get() {
            val layout = Theme[SenseeThemeProperties.layout]
            return SenseeLayout(
                screenHorizontalPaddingCompact = layout[SenseeLayoutTokens.screenHorizontalPaddingCompact],
                screenHorizontalPaddingMedium = layout[SenseeLayoutTokens.screenHorizontalPaddingMedium],
                screenHorizontalPaddingExpanded = layout[SenseeLayoutTokens.screenHorizontalPaddingExpanded],
                screenVerticalPaddingCompact = layout[SenseeLayoutTokens.screenVerticalPaddingCompact],
                screenVerticalPaddingMedium = layout[SenseeLayoutTokens.screenVerticalPaddingMedium],
                screenVerticalPaddingExpanded = layout[SenseeLayoutTokens.screenVerticalPaddingExpanded],
                contentMaxWidthCompact = layout[SenseeLayoutTokens.contentMaxWidthCompact],
                contentMaxWidthMedium = layout[SenseeLayoutTokens.contentMaxWidthMedium],
                contentMaxWidthExpanded = layout[SenseeLayoutTokens.contentMaxWidthExpanded],
                paneGap = layout[SenseeLayoutTokens.paneGap],
                navigationRailWidth = layout[SenseeLayoutTokens.navigationRailWidth],
                bottomNavigationHeight = layout[SenseeLayoutTokens.bottomNavigationHeight],
            )
        }

    public val shapes: SenseeShapes
        @Composable get() {
            val shapes = Theme[SenseeThemeProperties.shapes]
            return SenseeShapes(
                extraSmall = shapes[SenseeShapeTokens.extraSmall],
                small = shapes[SenseeShapeTokens.small],
                medium = shapes[SenseeShapeTokens.medium],
                large = shapes[SenseeShapeTokens.large],
                extraLarge = shapes[SenseeShapeTokens.extraLarge],
                circle = CircleShape,
            )
        }

    public val typography: SenseeTextStyles
        @Composable get() {
            val typography = Theme[SenseeThemeProperties.typography]
            return SenseeTextStyles(
                displayLarge = typography[SenseeTypographyTokens.displayLarge],
                displayMedium = typography[SenseeTypographyTokens.displayMedium],
                displaySmall = typography[SenseeTypographyTokens.displaySmall],
                headlineLarge = typography[SenseeTypographyTokens.headlineLarge],
                headlineMedium = typography[SenseeTypographyTokens.headlineMedium],
                headlineSmall = typography[SenseeTypographyTokens.headlineSmall],
                titleLarge = typography[SenseeTypographyTokens.titleLarge],
                titleMedium = typography[SenseeTypographyTokens.titleMedium],
                titleSmall = typography[SenseeTypographyTokens.titleSmall],
                bodyLarge = typography[SenseeTypographyTokens.bodyLarge],
                bodyMedium = typography[SenseeTypographyTokens.bodyMedium],
                bodySmall = typography[SenseeTypographyTokens.bodySmall],
                labelLarge = typography[SenseeTypographyTokens.labelLarge],
                labelMedium = typography[SenseeTypographyTokens.labelMedium],
                labelSmall = typography[SenseeTypographyTokens.labelSmall],
            )
        }
}
