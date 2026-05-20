package app.sensee.ui.designSystem.component.sentence

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import kotlin.math.max
import androidx.compose.ui.text.Placeholder as TextPlaceholder

@Composable
public fun SenseeSentenceText(
    sentence: SenseeSentence,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    colors: SenseeSentenceTextColors = SenseeSentenceTextDefaults.colors(),
    styles: SenseeSentenceTextStyles = SenseeSentenceTextDefaults.textStyles(colors),
    placeholderMetrics: SenseeSentencePlaceholderMetrics =
        SenseeSentenceTextDefaults.placeholderMetrics(),
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onClick: ((SenseeSentenceClick) -> Unit)? = null,
) {
    var textLayoutResult by remember {
        mutableStateOf<TextLayoutResult?>(null)
    }
    val currentOnClick by rememberUpdatedState(onClick)

    val annotatedString =
        remember(sentence, styles) {
            buildSenseeSentenceAnnotatedString(
                sentence = sentence,
                styles = styles,
            )
        }

    val inlineContent =
        rememberSenseeSentenceInlineContent(
            sentence = sentence,
            colors = colors,
            metrics = placeholderMetrics,
            textStyle = textStyle,
            onClick = onClick,
        )

    BasicText(
        text = annotatedString,
        modifier =
            modifier.then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.pointerInput(annotatedString) {
                        detectTapGestures { position ->
                            val tapHandler = currentOnClick ?: return@detectTapGestures
                            val layoutResult = textLayoutResult ?: return@detectTapGestures
                            val offset = layoutResult.getOffsetForPosition(position)

                            val wordAnnotation =
                                annotatedString
                                    .getStringAnnotations(
                                        tag = SentenceAnnotation.WORD,
                                        start = offset,
                                        end = offset,
                                    ).firstOrNull()

                            if (wordAnnotation != null) {
                                tapHandler(SenseeSentenceClick.Word(wordAnnotation.item))
                                return@detectTapGestures
                            }

                            val placeholderAnnotation =
                                annotatedString
                                    .getStringAnnotations(
                                        tag = SentenceAnnotation.PLACEHOLDER,
                                        start = offset,
                                        end = offset,
                                    ).firstOrNull()

                            if (placeholderAnnotation != null) {
                                val payload =
                                    SentencePlaceholderAnnotationPayload
                                        .decode(placeholderAnnotation.item)

                                tapHandler(
                                    SenseeSentenceClick.Placeholder(
                                        id = payload.id,
                                        groupId = payload.groupId,
                                        slotIndex = payload.slotIndex,
                                    ),
                                )
                            }
                        }
                    }
                },
            ),
        style =
            textStyle.merge(
                TextStyle(
                    color = colors.textPrimary,
                ),
            ),
        softWrap = softWrap,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inlineContent,
        onTextLayout = { result ->
            textLayoutResult = result
        },
    )
}

private fun buildSenseeSentenceAnnotatedString(
    sentence: SenseeSentence,
    styles: SenseeSentenceTextStyles,
): AnnotatedString =
    buildAnnotatedString {
        sentence.parts.forEach { part ->
            when (part) {
                is SenseeSentencePart.Text -> {
                    append(part.value)
                }

                is SenseeSentencePart.Word -> {
                    appendSentenceWord(part, styles)
                }

                is SenseeSentencePart.Placeholder -> {
                    appendSentencePlaceholder(part)
                }
            }
        }
    }

private fun AnnotatedString.Builder.appendSentenceWord(
    part: SenseeSentencePart.Word,
    styles: SenseeSentenceTextStyles,
) {
    val wordStart = length

    pushStringAnnotation(
        tag = SentenceAnnotation.WORD,
        annotation = part.id,
    )
    withStyle(part.state.toSpanStyle(styles)) {
        append(part.value)
    }
    pop()

    addSentenceHighlights(
        highlights = part.highlights,
        wordStart = wordStart,
        wordEnd = length,
        styles = styles,
    )
}

private fun AnnotatedString.Builder.addSentenceHighlights(
    highlights: List<SenseeTextRangeHighlight>,
    wordStart: Int,
    wordEnd: Int,
    styles: SenseeSentenceTextStyles,
) {
    highlights.forEach { highlight ->
        val start = (wordStart + highlight.start).coerceIn(wordStart, wordEnd)
        val end = (wordStart + highlight.end).coerceIn(wordStart, wordEnd)

        if (start < end) {
            addStyle(
                style = highlight.style.toSpanStyle(styles),
                start = start,
                end = end,
            )
        }
    }
}

private fun AnnotatedString.Builder.appendSentencePlaceholder(part: SenseeSentencePart.Placeholder) {
    pushStringAnnotation(
        tag = SentenceAnnotation.PLACEHOLDER,
        annotation = SentencePlaceholderAnnotationPayload.encode(part),
    )
    appendInlineContent(
        id = part.inlineContentId(),
        alternateText = part.value ?: part.fallbackText(),
    )
    pop()
}

@Composable
private fun rememberSenseeSentenceInlineContent(
    sentence: SenseeSentence,
    colors: SenseeSentenceTextColors,
    metrics: SenseeSentencePlaceholderMetrics,
    textStyle: TextStyle,
    onClick: ((SenseeSentenceClick) -> Unit)?,
): Map<String, InlineTextContent> =
    remember(sentence, colors, metrics, textStyle, onClick) {
        sentence.parts
            .filterIsInstance<SenseeSentencePart.Placeholder>()
            .associate { placeholder ->
                placeholder.inlineContentId() to
                    InlineTextContent(
                        placeholder =
                            TextPlaceholder(
                                width = placeholder.placeholderWidth(metrics),
                                height = metrics.height,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                            ),
                    ) {
                        SenseeSentencePlaceholderPill(
                            placeholder = placeholder,
                            colors = colors,
                            metrics = metrics,
                            textStyle = textStyle,
                            onClick = onClick,
                        )
                    }
            }
    }

@Composable
private fun SenseeSentencePlaceholderPill(
    placeholder: SenseeSentencePart.Placeholder,
    colors: SenseeSentenceTextColors,
    metrics: SenseeSentencePlaceholderMetrics,
    textStyle: TextStyle,
    onClick: ((SenseeSentenceClick) -> Unit)?,
) {
    val shape = RoundedCornerShape(percent = 50)
    val palette = placeholder.state.toPillPalette(colors)
    val fontSizeDp = textStyle.resolveFontSizeDp()
    val horizontalPadding = fontSizeDp * metrics.horizontalPaddingEm

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(palette.background)
                .border(
                    width = palette.borderWidth,
                    color = palette.border,
                    shape = shape,
                ).then(
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier.clickable {
                            onClick(
                                SenseeSentenceClick.Placeholder(
                                    id = placeholder.id,
                                    groupId = placeholder.groupId,
                                    slotIndex = placeholder.slotIndex,
                                ),
                            )
                        }
                    },
                ).padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        val value = placeholder.value

        // Empty pill renders no inner content — its shape/border alone communicates
        // "missing word", and the placeholder width (set via `emptySingleWordWidthEm` etc.
        // in `placeholderWidth`) scales with `wordCount` so multi-word answers (phrasal
        // verbs) still read as wider.
        if (value != null) {
            BasicText(
                text = value,
                style =
                    textStyle.merge(
                        TextStyle(
                            color = palette.content,
                            fontWeight = FontWeight.SemiBold,
                            fontSize =
                                textStyle.fontSize.takeOrElse {
                                    14.sp
                                },
                        ),
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TextStyle.resolveFontSizeDp(): Dp {
    val density = LocalDensity.current
    val size = fontSize
    return with(density) {
        when {
            size.isUnspecified -> 14.sp.toDp()
            size.type == TextUnitType.Sp -> size.toDp()
            else -> 14.sp.toDp()
        }
    }
}

private fun SenseeSentencePart.Placeholder.placeholderWidth(metrics: SenseeSentencePlaceholderMetrics): TextUnit {
    // Priority: a filled `value` defines the rendered text → width matches it. If we know the
    // eventual answer via `expectedText`, size the empty pill to match. Otherwise fall back to
    // a generic per-word-slot width.
    val sizingText = value ?: expectedText

    val widthEm =
        if (sizingText != null) {
            max(
                metrics.filledMinWidthEm,
                sizingText.length * metrics.filledCharWidthEm,
            )
        } else {
            val safeWordCount = wordCount.coerceAtLeast(1)

            safeWordCount * metrics.emptySingleWordWidthEm +
                (safeWordCount - 1) * metrics.emptyWordSpacingEm
        }

    return widthEm.em
}

private fun SenseeSentencePart.Placeholder.inlineContentId(): String = "sensee-placeholder:$id"

private fun SenseeSentencePart.Placeholder.fallbackText(): String =
    List(wordCount.coerceAtLeast(1)) { "____" }
        .joinToString(separator = " ")

private fun SenseeSentenceWordState.toSpanStyle(styles: SenseeSentenceTextStyles): SpanStyle =
    when (this) {
        SenseeSentenceWordState.Normal ->
            SpanStyle()

        SenseeSentenceWordState.Target ->
            styles.targetWord

        SenseeSentenceWordState.Selected ->
            styles.selectedWord

        SenseeSentenceWordState.Correct ->
            styles.correctWord

        SenseeSentenceWordState.Incorrect ->
            styles.incorrectWord
    }

private fun SenseeSentenceHighlightStyle.toSpanStyle(styles: SenseeSentenceTextStyles): SpanStyle =
    when (this) {
        SenseeSentenceHighlightStyle.Accent ->
            styles.accent

        SenseeSentenceHighlightStyle.Muted ->
            styles.muted

        SenseeSentenceHighlightStyle.Danger ->
            styles.danger

        SenseeSentenceHighlightStyle.Success ->
            styles.success

        SenseeSentenceHighlightStyle.Grammar ->
            styles.grammar
    }

private fun SenseeSentencePlaceholderState.toPillPalette(colors: SenseeSentenceTextColors): SenseeSentencePillPalette =
    when (this) {
        SenseeSentencePlaceholderState.Empty ->
            SenseeSentencePillPalette(
                background = colors.placeholderEmptyBackground,
                border = colors.placeholderEmptyBorder,
                content = colors.placeholderEmptyContent,
                borderWidth = 1.dp,
            )

        SenseeSentencePlaceholderState.Selected ->
            SenseeSentencePillPalette(
                background = colors.placeholderSelectedBackground,
                border = colors.placeholderSelectedBorder,
                content = colors.placeholderSelectedContent,
                borderWidth = 1.5.dp,
            )

        SenseeSentencePlaceholderState.Filled ->
            SenseeSentencePillPalette(
                background = colors.placeholderFilledBackground,
                border = colors.placeholderFilledBorder,
                content = colors.placeholderFilledContent,
                borderWidth = 1.dp,
            )

        SenseeSentencePlaceholderState.Correct ->
            SenseeSentencePillPalette(
                background = colors.placeholderCorrectBackground,
                border = colors.placeholderCorrectBorder,
                content = colors.placeholderCorrectContent,
                borderWidth = 1.5.dp,
            )

        SenseeSentencePlaceholderState.Incorrect ->
            SenseeSentencePillPalette(
                background = colors.placeholderIncorrectBackground,
                border = colors.placeholderIncorrectBorder,
                content = colors.placeholderIncorrectContent,
                borderWidth = 1.5.dp,
            )
    }

@Stable
private data class SenseeSentencePillPalette(
    val background: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
    val borderWidth: Dp,
)

private object SentenceAnnotation {
    const val WORD = "sensee_sentence_word"
    const val PLACEHOLDER = "sensee_sentence_placeholder"
}

private data class SentencePlaceholderAnnotationPayload(
    val id: String,
    val groupId: String,
    val slotIndex: Int,
) {
    companion object {
        private const val SEPARATOR = "\u001F"

        fun encode(placeholder: SenseeSentencePart.Placeholder): String =
            listOf(
                placeholder.id,
                placeholder.groupId,
                placeholder.slotIndex.toString(),
            ).joinToString(SEPARATOR)

        fun decode(value: String): SentencePlaceholderAnnotationPayload {
            val parts = value.split(SEPARATOR)

            return SentencePlaceholderAnnotationPayload(
                id = parts.getOrNull(0).orEmpty(),
                groupId = parts.getOrNull(1).orEmpty(),
                slotIndex = parts.getOrNull(2)?.toIntOrNull() ?: 0,
            )
        }
    }
}

private inline fun TextUnit.takeOrElse(fallback: () -> TextUnit): TextUnit =
    if (isUnspecified) {
        fallback()
    } else {
        this
    }
