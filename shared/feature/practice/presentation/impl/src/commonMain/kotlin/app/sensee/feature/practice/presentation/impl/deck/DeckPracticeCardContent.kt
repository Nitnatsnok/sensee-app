package app.sensee.feature.practice.presentation.impl.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import app.sensee.feature.practice.domain.PracticeCardFront
import app.sensee.feature.practice.presentation.api.DeckPracticeCardUiState
import app.sensee.feature.practice.presentation.impl.grammar.grammarTagBadgeColors
import app.sensee.feature.practice.presentation.impl.grammar.grammarUnitBadgeColors
import app.sensee.feature.practice.presentation.impl.text.shortLabel
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.SentenceSegment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.SurfaceToken
import app.sensee.ui.designSystem.component.badge.SenseeBadge
import app.sensee.ui.designSystem.component.button.SenseeSpeakIconButton
import app.sensee.ui.designSystem.component.button.SenseeSpeakIconButtonDefaults
import app.sensee.ui.designSystem.component.sentence.SenseeSentence
import app.sensee.ui.designSystem.component.sentence.SenseeSentencePart
import app.sensee.ui.designSystem.component.sentence.SenseeSentencePlaceholderState
import app.sensee.ui.designSystem.component.sentence.SenseeSentenceText
import app.sensee.ui.designSystem.component.sentence.SenseeSentenceWordState
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun PracticeCardFrontContent(
    card: DeckPracticeCardUiState,
    labels: GrammarLabels,
    studyLanguageTag: String,
    onSpeak: (text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (card.practiceFront) {
        PracticeCardFront.English ->
            EnglishCardContent(
                card = card,
                labels = labels,
                studyLanguageTag = studyLanguageTag,
                onSpeak = onSpeak,
                modifier = modifier,
            )

        PracticeCardFront.Russian ->
            RussianPlaceholderCardContent(
                card = card,
                modifier = modifier,
            )
    }
}

@Composable
internal fun PracticeCardBackContent(
    card: DeckPracticeCardUiState,
    labels: GrammarLabels,
    studyLanguageTag: String,
    onSpeak: (text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (card.practiceFront) {
        PracticeCardFront.English ->
            RussianRevealedCardContent(
                card = card,
                onSpeak = onSpeak,
                modifier = modifier,
            )

        PracticeCardFront.Russian ->
            EnglishCardContent(
                card = card,
                labels = labels,
                studyLanguageTag = studyLanguageTag,
                onSpeak = onSpeak,
                modifier = modifier,
            )
    }
}

@Composable
private fun EnglishCardContent(
    card: DeckPracticeCardUiState,
    labels: GrammarLabels,
    studyLanguageTag: String,
    onSpeak: (text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SenseeTheme.spacing
    val typography = SenseeTheme.typography
    val colors = SenseeTheme.colors
    val headwordText =
        rememberHeadwordAnnotated(
            raw = card.headword,
            mutedColor = colors.textSecondary,
        )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.large, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpeakableSlot(onSpeak = { onSpeak(card.headword) }) {
            Text(
                text = headwordText,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                style = typography.headlineMedium,
            )
        }
        BadgeRow(card = card, labels = labels, studyLanguageTag = studyLanguageTag)
        ContextSentenceLine(
            sentence = buildContextSentence(card.contextSentence, card.headword, revealed = true),
            color = colors.textPrimary,
            onSpeak = { onSpeak(card.contextSentence) },
        )
    }
}

@Composable
private fun RussianRevealedCardContent(
    card: DeckPracticeCardUiState,
    onSpeak: (text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SenseeTheme.spacing
    val typography = SenseeTheme.typography
    val colors = SenseeTheme.colors

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.large, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TranslationSlot(translation = card.translation, color = colors.textPrimary)
        if (card.explanation.isNotBlank()) {
            Text(
                text = card.explanation,
                color = colors.textSecondary,
                style = typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        ContextSentenceLine(
            sentence = buildContextSentence(card.contextSentence, card.headword, revealed = true),
            color = colors.textPrimary,
            onSpeak = { onSpeak(card.contextSentence) },
        )
    }
}

@Composable
private fun RussianPlaceholderCardContent(
    card: DeckPracticeCardUiState,
    modifier: Modifier = Modifier,
) {
    val spacing = SenseeTheme.spacing
    val typography = SenseeTheme.typography
    val colors = SenseeTheme.colors

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.large, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TranslationSlot(translation = card.translation, color = colors.textPrimary)
        if (card.explanation.isNotBlank()) {
            Text(
                text = card.explanation,
                color = colors.textSecondary,
                style = typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        // Speak button is intentionally absent: hearing the sentence would reveal the answer.
        ContextSentenceLine(
            sentence = buildContextSentence(card.contextSentence, card.headword, revealed = false),
            color = colors.textPrimary,
            onSpeak = null,
        )
    }
}

@Composable
private fun TranslationSlot(
    translation: String,
    color: Color,
) {
    val typography = SenseeTheme.typography
    SpeakableSlot(onSpeak = null) {
        Text(
            text = AnnotatedString(translation),
            textAlign = TextAlign.Center,
            color = color,
            style = typography.titleLarge,
        )
    }
}

/**
 * Centers the text horizontally as if the speak button were not there; the button
 * follows the text from the left with a small gap. A natural width that falls between
 * `centerableMax` and `singleLineMax` pins the button to the card edge and lets the
 * single line stretch to the trailing padding.
 */
@Composable
private fun SpeakableSlot(
    onSpeak: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacing = SenseeTheme.spacing
    val gap = spacing.small
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Box(contentAlignment = Alignment.Center, content = { content() })
            if (onSpeak != null) {
                SenseeSpeakIconButton(onClick = onSpeak)
            }
        },
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val containerWidth = constraints.maxWidth
        val iconVisualSize = SenseeSpeakIconButtonDefaults.Size.roundToPx()

        val buttonPlaceable = measurables.getOrNull(1)?.measure(Constraints())
        // SenseeIconButton placeable is the 48dp touch target around the visible disc;
        // shifting it left by this inset puts the visible icon edge exactly `gap` away
        // from the first letter.
        val touchInset =
            if (buttonPlaceable != null) {
                ((buttonPlaceable.width - iconVisualSize) / 2).coerceAtLeast(0)
            } else {
                0
            }
        val leftReserve =
            if (buttonPlaceable != null) touchInset + iconVisualSize + gapPx else 0
        val centerableMax = (containerWidth - 2 * leftReserve).coerceAtLeast(0)
        val singleLineMax = (containerWidth - leftReserve).coerceAtLeast(0)

        // Intrinsic query — not a `.measure()` call — so we can branch on width before
        // the single allowed measurement of the text measurable.
        val naturalWidth = measurables[0].maxIntrinsicWidth(constraints.maxHeight)
        val pinSingleLine =
            buttonPlaceable != null &&
                naturalWidth > centerableMax &&
                naturalWidth <= singleLineMax
        val textMinHeight = buttonPlaceable?.height ?: 0
        val textPlaceable =
            measurables[0].measure(
                if (pinSingleLine) {
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = singleLineMax,
                        minHeight = textMinHeight,
                    )
                } else {
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = centerableMax,
                        minHeight = textMinHeight,
                    )
                },
            )

        val containerHeight = maxOf(textPlaceable.height, buttonPlaceable?.height ?: 0)

        layout(containerWidth, containerHeight) {
            val textX =
                if (pinSingleLine) leftReserve else (containerWidth - textPlaceable.width) / 2
            textPlaceable.place(textX, 0)

            if (buttonPlaceable != null) {
                val visibleIconLeft = textX - gapPx - iconVisualSize
                val btnX = (visibleIconLeft - touchInset).coerceAtLeast(0)
                buttonPlaceable.place(btnX, 0)
            }
        }
    }
}

@Composable
private fun rememberHeadwordAnnotated(
    raw: String,
    mutedColor: Color,
): AnnotatedString {
    val parsed: SurfaceForm? =
        remember(raw) {
            if (raw.isBlank()) {
                null
            } else {
                try {
                    SurfaceForm.parse(raw)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }
    return remember(parsed, mutedColor, raw) {
        if (parsed == null) {
            AnnotatedString(raw)
        } else {
            val mutedSpan =
                SpanStyle(
                    color = mutedColor,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                )
            buildAnnotatedString {
                parsed.tokens.forEachIndexed { index, token ->
                    if (index > 0) append(" ")
                    when (token) {
                        is SurfaceToken.Literal -> append(token.text)
                        is SurfaceToken.Optional ->
                            withStyle(mutedSpan) { append("[${token.text}]") }

                        is SurfaceToken.Slot ->
                            withStyle(mutedSpan) { append("<${token.name}>") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextSentenceLine(
    sentence: SenseeSentence,
    color: Color,
    onSpeak: (() -> Unit)?,
) {
    val typography = SenseeTheme.typography
    SpeakableSlot(onSpeak = onSpeak) {
        SenseeSentenceText(
            sentence = sentence,
            textStyle = typography.bodyLarge.copy(color = color),
        )
    }
}

@Composable
private fun BadgeRow(
    card: DeckPracticeCardUiState,
    labels: GrammarLabels,
    studyLanguageTag: String,
) {
    val spacing = SenseeTheme.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        SenseeBadge(
            text = card.unitType.shortLabel(labels, studyLanguageTag),
            colors = grammarUnitBadgeColors(card.unitType),
        )
        card.grammarTags.forEach { tag ->
            SenseeBadge(
                text = tag.shortLabel(labels, studyLanguageTag),
                colors = grammarTagBadgeColors(tag),
            )
        }
    }
}

/**
 * Splits the context sentence around the studied unit. A marked `[[target]]`
 * span identifies the exact occurrence; unmarked content falls back to a
 * headword match. Hidden multi-word targets share one placeholder `groupId`.
 */
private fun buildContextSentence(
    sentence: String,
    headword: String,
    revealed: Boolean,
): SenseeSentence {
    val parsed = StudiedSentence.parse(sentence)
    if (parsed.target == null) {
        return legacyHeadwordSentence(parsed.plainText(), headword, revealed)
    }
    var targetOccurrence = 0
    return SenseeSentence(
        buildList {
            parsed.segments.forEach { segment ->
                when (segment) {
                    is SentenceSegment.Text ->
                        if (segment.value.isNotEmpty()) add(SenseeSentencePart.Text(segment.value))

                    is SentenceSegment.Target ->
                        addAll(targetParts(segment.value, revealed, "$HEADWORD_PART_ID-${targetOccurrence++}"))
                }
            }
        }.toPersistentList(),
    )
}

private fun targetParts(
    value: String,
    revealed: Boolean,
    id: String,
): List<SenseeSentencePart> {
    if (revealed) {
        return listOf(
            SenseeSentencePart.Word(id = id, value = value, state = SenseeSentenceWordState.Target),
        )
    }
    val tokens = value.split(' ').filter { it.isNotBlank() }
    val slotCount = tokens.size.coerceAtLeast(1)
    return buildList {
        tokens.forEachIndexed { index, token ->
            add(
                SenseeSentencePart.Placeholder(
                    id = "$id-$index",
                    groupId = id,
                    slotIndex = index,
                    slotCount = slotCount,
                    wordCount = 1,
                    // Width hint so the pill sizes to the eventual answer.
                    expectedText = token,
                    state = SenseeSentencePlaceholderState.Empty,
                ),
            )
            if (index < tokens.size - 1) add(SenseeSentencePart.Text(" "))
        }
    }
}

private fun legacyHeadwordSentence(
    sentence: String,
    headword: String,
    revealed: Boolean,
): SenseeSentence {
    if (headword.isBlank()) {
        return SenseeSentence(persistentListOf(SenseeSentencePart.Text(sentence)))
    }
    val matchStart = sentence.indexOf(headword, ignoreCase = true)
    if (matchStart < 0) {
        return SenseeSentence(persistentListOf(SenseeSentencePart.Text(sentence)))
    }
    val matchEnd = matchStart + headword.length
    val before = sentence.substring(0, matchStart)
    val matched = sentence.substring(matchStart, matchEnd)
    val after = sentence.substring(matchEnd)
    return SenseeSentence(
        buildList {
            if (before.isNotEmpty()) add(SenseeSentencePart.Text(before))
            addAll(targetParts(matched, revealed, HEADWORD_PART_ID))
            if (after.isNotEmpty()) add(SenseeSentencePart.Text(after))
        }.toPersistentList(),
    )
}

private const val HEADWORD_PART_ID = "headword"
