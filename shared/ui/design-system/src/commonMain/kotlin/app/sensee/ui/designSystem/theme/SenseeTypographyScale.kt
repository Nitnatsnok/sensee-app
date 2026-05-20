@file:Suppress("MatchingDeclarationName")

package app.sensee.ui.designSystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.composeunstyled.theme.ThemeToken
import org.jetbrains.compose.resources.Font

/**
 * Per-token typography spec (size, line height, weight, letter spacing). Pure data — the
 * actual Mulish font family and the eventual `TextStyle` map are assembled at composition
 * time by [senseeTextStyles] which needs `@Composable` context to load font resources.
 *
 */
internal data class SenseeTypographySpec(
    val fontSize: Int,
    val lineHeight: Int,
    val fontWeight: FontWeight = FontWeight.Normal,
    val letterSpacing: Float = 0f,
)

/**
 * Numeric typography scale — M3-aligned values (display/headline/title/body/label families).
 * Theme-independent. Combined with [senseeFontFamily] at composition time inside
 * [senseeTextStyles] to produce the final [TextStyle] map.
 */
internal val SenseeTypographyScale: Map<ThemeToken<TextStyle>, SenseeTypographySpec> =
    mapOf(
        SenseeTypographyTokens.bodyLarge to
            SenseeTypographySpec(fontSize = 16, lineHeight = 24, letterSpacing = 0.5f),
        SenseeTypographyTokens.bodyMedium to
            SenseeTypographySpec(fontSize = 14, lineHeight = 20, letterSpacing = 0.25f),
        SenseeTypographyTokens.bodySmall to
            SenseeTypographySpec(fontSize = 12, lineHeight = 16, letterSpacing = 0.4f),
        SenseeTypographyTokens.displayLarge to
            SenseeTypographySpec(fontSize = 57, lineHeight = 64, fontWeight = FontWeight.Bold),
        SenseeTypographyTokens.displayMedium to
            SenseeTypographySpec(fontSize = 45, lineHeight = 52, fontWeight = FontWeight.Bold),
        SenseeTypographyTokens.displaySmall to
            SenseeTypographySpec(fontSize = 36, lineHeight = 44, fontWeight = FontWeight.Bold),
        SenseeTypographyTokens.headlineLarge to
            SenseeTypographySpec(fontSize = 32, lineHeight = 40, fontWeight = FontWeight.SemiBold),
        SenseeTypographyTokens.headlineMedium to
            SenseeTypographySpec(fontSize = 28, lineHeight = 36, fontWeight = FontWeight.SemiBold),
        SenseeTypographyTokens.headlineSmall to
            SenseeTypographySpec(fontSize = 24, lineHeight = 32, fontWeight = FontWeight.SemiBold),
        SenseeTypographyTokens.labelLarge to
            SenseeTypographySpec(
                fontSize = 14,
                lineHeight = 20,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1f,
            ),
        SenseeTypographyTokens.labelMedium to
            SenseeTypographySpec(
                fontSize = 12,
                lineHeight = 16,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5f,
            ),
        SenseeTypographyTokens.labelSmall to
            SenseeTypographySpec(
                fontSize = 11,
                lineHeight = 16,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5f,
            ),
        SenseeTypographyTokens.titleLarge to
            SenseeTypographySpec(fontSize = 22, lineHeight = 28, fontWeight = FontWeight.Bold),
        SenseeTypographyTokens.titleMedium to
            SenseeTypographySpec(
                fontSize = 16,
                lineHeight = 24,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.15f,
            ),
        SenseeTypographyTokens.titleSmall to
            SenseeTypographySpec(
                fontSize = 14,
                lineHeight = 20,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1f,
            ),
    )

/** Mulish font family — 5 weights + italics. Loaded from the design-system's resources. */
@Composable
internal fun senseeFontFamily(): FontFamily =
    FontFamily(
        Font(resource = SenseeFontResources.mulishVariable, weight = FontWeight.Light),
        Font(resource = SenseeFontResources.mulishVariable, weight = FontWeight.Normal),
        Font(resource = SenseeFontResources.mulishVariable, weight = FontWeight.Medium),
        Font(resource = SenseeFontResources.mulishVariable, weight = FontWeight.SemiBold),
        Font(resource = SenseeFontResources.mulishVariable, weight = FontWeight.Bold),
        Font(
            resource = SenseeFontResources.mulishItalicVariable,
            weight = FontWeight.Light,
            style = FontStyle.Italic,
        ),
        Font(
            resource = SenseeFontResources.mulishItalicVariable,
            weight = FontWeight.Normal,
            style = FontStyle.Italic,
        ),
        Font(
            resource = SenseeFontResources.mulishItalicVariable,
            weight = FontWeight.Medium,
            style = FontStyle.Italic,
        ),
        Font(
            resource = SenseeFontResources.mulishItalicVariable,
            weight = FontWeight.SemiBold,
            style = FontStyle.Italic,
        ),
        Font(
            resource = SenseeFontResources.mulishItalicVariable,
            weight = FontWeight.Bold,
            style = FontStyle.Italic,
        ),
    )

/** Materialises [SenseeTypographyScale] into a [TextStyle] map bound to Mulish. */
@Composable
internal fun senseeTextStyles(): Map<ThemeToken<TextStyle>, TextStyle> {
    val defaultTextStyle = TextStyle(fontFamily = senseeFontFamily())
    return SenseeTypographyScale.mapValues { (_, spec) ->
        defaultTextStyle.copy(
            fontSize = spec.fontSize.sp,
            lineHeight = spec.lineHeight.sp,
            fontWeight = spec.fontWeight,
            letterSpacing = spec.letterSpacing.sp,
        )
    }
}
