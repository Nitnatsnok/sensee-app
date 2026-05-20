package app.sensee.ui.designSystem.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.composeunstyled.theme.ThemeProperty
import com.composeunstyled.theme.ThemeToken

public object SenseeThemeProperties {
    public val colors: ThemeProperty<Color> = ThemeProperty("app.colors")
    public val spacing: ThemeProperty<Dp> = ThemeProperty("app.spacing")
    public val shapes: ThemeProperty<CornerBasedShape> = ThemeProperty("app.shapes")
    public val typography: ThemeProperty<TextStyle> = ThemeProperty("app.textStyles")
    public val layout: ThemeProperty<Dp> = ThemeProperty("app.layout")
}

public object SenseeColorTokens {
    public val background: ThemeToken<Color> = ThemeToken("background")
    public val surface: ThemeToken<Color> = ThemeToken("surface")
    public val surfaceBright: ThemeToken<Color> = ThemeToken("surfaceBright")
    public val surfaceDim: ThemeToken<Color> = ThemeToken("surfaceDim")
    public val surfaceContainerLowest: ThemeToken<Color> = ThemeToken("surfaceContainerLowest")
    public val surfaceContainerLow: ThemeToken<Color> = ThemeToken("surfaceContainerLow")
    public val surfaceContainer: ThemeToken<Color> = ThemeToken("surfaceContainer")
    public val surfaceContainerHigh: ThemeToken<Color> = ThemeToken("surfaceContainerHigh")
    public val surfaceContainerHighest: ThemeToken<Color> = ThemeToken("surfaceContainerHighest")
    public val surfaceTint: ThemeToken<Color> = ThemeToken("surfaceTint")

    public val textPrimary: ThemeToken<Color> = ThemeToken("textPrimary")
    public val textSecondary: ThemeToken<Color> = ThemeToken("textSecondary")
    public val textMuted: ThemeToken<Color> = ThemeToken("textMuted")
    public val border: ThemeToken<Color> = ThemeToken("border")
    public val divider: ThemeToken<Color> = ThemeToken("divider")

    public val inverseSurface: ThemeToken<Color> = ThemeToken("inverseSurface")
    public val inverseTextPrimary: ThemeToken<Color> = ThemeToken("inverseTextPrimary")
    public val inverseAccent: ThemeToken<Color> = ThemeToken("inverseAccent")

    public val accent: ThemeToken<Color> = ThemeToken("accent")
    public val textOnAccent: ThemeToken<Color> = ThemeToken("textOnAccent")
    public val accentContainer: ThemeToken<Color> = ThemeToken("accentContainer")
    public val textOnAccentContainer: ThemeToken<Color> = ThemeToken("textOnAccentContainer")
    public val accentFixed: ThemeToken<Color> = ThemeToken("accentFixed")
    public val accentFixedDim: ThemeToken<Color> = ThemeToken("accentFixedDim")
    public val textOnAccentFixed: ThemeToken<Color> = ThemeToken("textOnAccentFixed")
    public val textOnAccentFixedVariant: ThemeToken<Color> = ThemeToken("textOnAccentFixedVariant")

    public val accentSubtle: ThemeToken<Color> = ThemeToken("accentSubtle")
    public val textOnAccentSubtle: ThemeToken<Color> = ThemeToken("textOnAccentSubtle")
    public val accentSubtleContainer: ThemeToken<Color> = ThemeToken("accentSubtleContainer")
    public val textOnAccentSubtleContainer: ThemeToken<Color> = ThemeToken("textOnAccentSubtleContainer")
    public val accentSubtleFixed: ThemeToken<Color> = ThemeToken("accentSubtleFixed")
    public val accentSubtleFixedDim: ThemeToken<Color> = ThemeToken("accentSubtleFixedDim")
    public val textOnAccentSubtleFixed: ThemeToken<Color> = ThemeToken("textOnAccentSubtleFixed")
    public val textOnAccentSubtleFixedVariant: ThemeToken<Color> = ThemeToken("textOnAccentSubtleFixedVariant")

    public val warning: ThemeToken<Color> = ThemeToken("warning")
    public val textOnWarning: ThemeToken<Color> = ThemeToken("textOnWarning")
    public val warningContainer: ThemeToken<Color> = ThemeToken("warningContainer")
    public val textOnWarningContainer: ThemeToken<Color> = ThemeToken("textOnWarningContainer")
    public val warningFixed: ThemeToken<Color> = ThemeToken("warningFixed")
    public val warningFixedDim: ThemeToken<Color> = ThemeToken("warningFixedDim")
    public val textOnWarningFixed: ThemeToken<Color> = ThemeToken("textOnWarningFixed")
    public val textOnWarningFixedVariant: ThemeToken<Color> = ThemeToken("textOnWarningFixedVariant")

    public val danger: ThemeToken<Color> = ThemeToken("danger")
    public val textOnDanger: ThemeToken<Color> = ThemeToken("textOnDanger")
    public val dangerContainer: ThemeToken<Color> = ThemeToken("dangerContainer")
    public val textOnDangerContainer: ThemeToken<Color> = ThemeToken("textOnDangerContainer")

    public val success: ThemeToken<Color> = ThemeToken("success")
    public val textOnSuccess: ThemeToken<Color> = ThemeToken("textOnSuccess")
    public val successContainer: ThemeToken<Color> = ThemeToken("successContainer")
    public val textOnSuccessContainer: ThemeToken<Color> = ThemeToken("textOnSuccessContainer")

    public val info: ThemeToken<Color> = ThemeToken("info")
    public val textOnInfo: ThemeToken<Color> = ThemeToken("textOnInfo")
    public val infoContainer: ThemeToken<Color> = ThemeToken("infoContainer")
    public val textOnInfoContainer: ThemeToken<Color> = ThemeToken("textOnInfoContainer")

    public val scrim: ThemeToken<Color> = ThemeToken("scrim")
}

public object SenseeSpacingTokens {
    public val none: ThemeToken<Dp> = ThemeToken("none")
    public val extraSmall: ThemeToken<Dp> = ThemeToken("extraSmall")
    public val small: ThemeToken<Dp> = ThemeToken("small")
    public val medium: ThemeToken<Dp> = ThemeToken("medium")
    public val large: ThemeToken<Dp> = ThemeToken("large")
    public val extraLarge: ThemeToken<Dp> = ThemeToken("extraLarge")
    public val doubleExtraLarge: ThemeToken<Dp> = ThemeToken("doubleExtraLarge")
    public val tripleExtraLarge: ThemeToken<Dp> = ThemeToken("tripleExtraLarge")
}

public object SenseeLayoutTokens {
    public val screenHorizontalPaddingCompact: ThemeToken<Dp> = ThemeToken("screenHorizontalPaddingCompact")
    public val screenHorizontalPaddingMedium: ThemeToken<Dp> = ThemeToken("screenHorizontalPaddingMedium")
    public val screenHorizontalPaddingExpanded: ThemeToken<Dp> = ThemeToken("screenHorizontalPaddingExpanded")
    public val screenVerticalPaddingCompact: ThemeToken<Dp> = ThemeToken("screenVerticalPaddingCompact")
    public val screenVerticalPaddingMedium: ThemeToken<Dp> = ThemeToken("screenVerticalPaddingMedium")
    public val screenVerticalPaddingExpanded: ThemeToken<Dp> = ThemeToken("screenVerticalPaddingExpanded")
    public val contentMaxWidthCompact: ThemeToken<Dp> = ThemeToken("contentMaxWidthCompact")
    public val contentMaxWidthMedium: ThemeToken<Dp> = ThemeToken("contentMaxWidthMedium")
    public val contentMaxWidthExpanded: ThemeToken<Dp> = ThemeToken("contentMaxWidthExpanded")
    public val paneGap: ThemeToken<Dp> = ThemeToken("paneGap")
    public val navigationRailWidth: ThemeToken<Dp> = ThemeToken("navigationRailWidth")
    public val bottomNavigationHeight: ThemeToken<Dp> = ThemeToken("bottomNavigationHeight")
}

public object SenseeShapeTokens {
    public val extraSmall: ThemeToken<CornerBasedShape> = ThemeToken("extraSmall")
    public val small: ThemeToken<CornerBasedShape> = ThemeToken("small")
    public val medium: ThemeToken<CornerBasedShape> = ThemeToken("medium")
    public val large: ThemeToken<CornerBasedShape> = ThemeToken("large")
    public val extraLarge: ThemeToken<CornerBasedShape> = ThemeToken("extraLarge")
}

public object SenseeTypographyTokens {
    public val bodyLarge: ThemeToken<TextStyle> = ThemeToken("bodyLarge")
    public val bodyMedium: ThemeToken<TextStyle> = ThemeToken("bodyMedium")
    public val bodySmall: ThemeToken<TextStyle> = ThemeToken("bodySmall")
    public val displayLarge: ThemeToken<TextStyle> = ThemeToken("displayLarge")
    public val displayMedium: ThemeToken<TextStyle> = ThemeToken("displayMedium")
    public val displaySmall: ThemeToken<TextStyle> = ThemeToken("displaySmall")
    public val headlineLarge: ThemeToken<TextStyle> = ThemeToken("headlineLarge")
    public val headlineMedium: ThemeToken<TextStyle> = ThemeToken("headlineMedium")
    public val headlineSmall: ThemeToken<TextStyle> = ThemeToken("headlineSmall")
    public val labelLarge: ThemeToken<TextStyle> = ThemeToken("labelLarge")
    public val labelMedium: ThemeToken<TextStyle> = ThemeToken("labelMedium")
    public val labelSmall: ThemeToken<TextStyle> = ThemeToken("labelSmall")
    public val titleLarge: ThemeToken<TextStyle> = ThemeToken("titleLarge")
    public val titleMedium: ThemeToken<TextStyle> = ThemeToken("titleMedium")
    public val titleSmall: ThemeToken<TextStyle> = ThemeToken("titleSmall")
}
