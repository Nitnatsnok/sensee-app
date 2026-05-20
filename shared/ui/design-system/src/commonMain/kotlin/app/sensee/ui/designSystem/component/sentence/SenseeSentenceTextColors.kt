package app.sensee.ui.designSystem.component.sentence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import app.sensee.ui.designSystem.theme.SenseeTheme

@Immutable
public data class SenseeSentenceTextColors(
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val wordAccent: Color,
    val wordMuted: Color,
    val wordDanger: Color,
    val wordSuccess: Color,
    val grammarBackground: Color,
    val placeholderEmptyBackground: Color,
    val placeholderEmptyBorder: Color,
    val placeholderEmptyContent: Color,
    val placeholderSelectedBackground: Color,
    val placeholderSelectedBorder: Color,
    val placeholderSelectedContent: Color,
    val placeholderFilledBackground: Color,
    val placeholderFilledBorder: Color,
    val placeholderFilledContent: Color,
    val placeholderCorrectBackground: Color,
    val placeholderCorrectBorder: Color,
    val placeholderCorrectContent: Color,
    val placeholderIncorrectBackground: Color,
    val placeholderIncorrectBorder: Color,
    val placeholderIncorrectContent: Color,
)

@Immutable
public data class SenseeSentenceTextStyles(
    val accent: SpanStyle,
    val muted: SpanStyle,
    val danger: SpanStyle,
    val success: SpanStyle,
    val grammar: SpanStyle,
    val targetWord: SpanStyle,
    val selectedWord: SpanStyle,
    val correctWord: SpanStyle,
    val incorrectWord: SpanStyle,
)

/**
 * Sizing for inline placeholder pills. Values are expressed relative to the surrounding text
 * font size (either as `em` `TextUnit`s for the inline-content placeholder API, or as `*Em`
 * floats the pill internals convert into `Dp` via density and font size). The pill scales
 * together with the sentence text — a larger context body grows the pill height and padding
 * in lockstep. Empty pills render without inner content; word count is communicated by pill
 * width via [emptySingleWordWidthEm] and [emptyWordSpacingEm].
 */
@Immutable
public data class SenseeSentencePlaceholderMetrics(
    /**
     * Total pill height. Slightly larger than 1em so the pill stays vertically inside the
     * surrounding line — at `bodyLarge` (16sp / 24sp line height) this resolves to ~20sp,
     * comfortably under the line cap and bottom.
     */
    val height: TextUnit = 1.25.em,
    val horizontalPaddingEm: Float = 0.4f,
    /** Fallback width per word slot when no [expectedText][SenseeSentencePart.Placeholder.expectedText] is supplied. */
    val emptySingleWordWidthEm: Float = 4.4f,
    val emptyWordSpacingEm: Float = 0.8f,
    /** Floor for filled / hint-sized pills — keeps very short words (e.g. "I") from collapsing. */
    val filledMinWidthEm: Float = 1.5f,
    /** Approximate width of one character in `em` for hint-based / filled sizing. */
    val filledCharWidthEm: Float = 0.5f,
)

public object SenseeSentenceTextDefaults {
    @Composable
    public fun colors(): SenseeSentenceTextColors {
        val colors = SenseeTheme.colors

        return remember(colors) {
            SenseeSentenceTextColors(
                textPrimary = colors.textPrimary,
                textSecondary = colors.textSecondary,
                textMuted = colors.textMuted,
                wordAccent = colors.accent,
                wordMuted = colors.textSecondary,
                wordDanger = colors.danger,
                wordSuccess = colors.success,
                grammarBackground = colors.accentSubtleContainer.copy(alpha = 0.62f),
                placeholderEmptyBackground = colors.surfaceContainerHigh.copy(alpha = 0.88f),
                placeholderEmptyBorder = colors.border.copy(alpha = 0.85f),
                placeholderEmptyContent = colors.textMuted,
                placeholderSelectedBackground = colors.accentContainer,
                placeholderSelectedBorder = colors.accent,
                placeholderSelectedContent = colors.textOnAccentContainer,
                placeholderFilledBackground = colors.accentSubtleContainer.copy(alpha = 0.9f),
                placeholderFilledBorder = colors.accentSubtle.copy(alpha = 0.72f),
                placeholderFilledContent = colors.textOnAccentSubtleContainer,
                placeholderCorrectBackground = colors.successContainer,
                placeholderCorrectBorder = colors.success,
                placeholderCorrectContent = colors.textOnSuccessContainer,
                placeholderIncorrectBackground = colors.dangerContainer,
                placeholderIncorrectBorder = colors.danger,
                placeholderIncorrectContent = colors.textOnDangerContainer,
            )
        }
    }

    @Composable
    public fun textStyles(colors: SenseeSentenceTextColors = colors()): SenseeSentenceTextStyles =
        remember(colors) {
            SenseeSentenceTextStyles(
                accent =
                    SpanStyle(
                        color = colors.wordAccent,
                        fontWeight = FontWeight.SemiBold,
                    ),
                muted =
                    SpanStyle(
                        color = colors.wordMuted,
                    ),
                danger =
                    SpanStyle(
                        color = colors.wordDanger,
                        fontWeight = FontWeight.SemiBold,
                    ),
                success =
                    SpanStyle(
                        color = colors.wordSuccess,
                        fontWeight = FontWeight.SemiBold,
                    ),
                grammar =
                    SpanStyle(
                        background = colors.grammarBackground,
                        fontWeight = FontWeight.Medium,
                    ),
                targetWord =
                    SpanStyle(
                        color = colors.wordAccent,
                        fontWeight = FontWeight.SemiBold,
                    ),
                selectedWord =
                    SpanStyle(
                        background = colors.placeholderSelectedBackground.copy(alpha = 0.75f),
                        color = colors.placeholderSelectedContent,
                        fontWeight = FontWeight.SemiBold,
                    ),
                correctWord =
                    SpanStyle(
                        color = colors.wordSuccess,
                        fontWeight = FontWeight.SemiBold,
                    ),
                incorrectWord =
                    SpanStyle(
                        color = colors.wordDanger,
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }

    @Composable
    public fun placeholderMetrics(): SenseeSentencePlaceholderMetrics =
        remember {
            SenseeSentencePlaceholderMetrics()
        }
}
