package app.sensee.feature.practice.presentation.impl.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import app.sensee.feature.practice.domain.PracticeCardFront
import app.sensee.feature.practice.presentation.api.DeckPracticeCardUiState
import app.sensee.feature.practice.presentation.impl.grammar.grammarTagBadgeColors
import app.sensee.feature.practice.presentation.impl.grammar.grammarUnitBadgeColors
import app.sensee.feature.practice.presentation.impl.text.shortLabel
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.SentenceSegment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.ui.designSystem.component.badge.SenseeBadge
import app.sensee.ui.designSystem.component.button.SenseeSpeakIconButton
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

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(spacing.large, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpeakableHeadline(
            text = card.headword,
            style = typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            onSpeak = { onSpeak(card.headword) },
        )
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
        Text(
            text = card.translation,
            color = colors.textPrimary,
            style = typography.titleLarge,
            textAlign = TextAlign.Center,
        )
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
        Text(
            text = card.translation,
            color = colors.textPrimary,
            style = typography.titleLarge,
            textAlign = TextAlign.Center,
        )
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
private fun SpeakableHeadline(
    text: String,
    style: TextStyle,
    color: Color,
    onSpeak: () -> Unit,
) {
    val spacing = SenseeTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SenseeSpeakIconButton(onClick = onSpeak)
        Text(text = text, color = color, style = style)
    }
}

@Composable
private fun ContextSentenceLine(
    sentence: SenseeSentence,
    color: Color,
    onSpeak: (() -> Unit)?,
) {
    val spacing = SenseeTheme.spacing
    val typography = SenseeTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.small),
        horizontalArrangement =
            Arrangement.spacedBy(
                space = spacing.small,
                alignment = Alignment.CenterHorizontally,
            ),
    ) {
        if (onSpeak != null) {
            // Align the speak icon to the first line's baseline rather than the row's top —
            // for a multi-line context sentence this keeps the icon next to line 1 instead of
            // floating above it (which `Alignment.Top` would do because the icon button is
            // 32dp tall while a `bodyLarge` line is ~24dp).
            SenseeSpeakIconButton(
                onClick = onSpeak,
                modifier = Modifier.alignBy { placeable -> placeable.measuredHeight / 2 },
            )
        }
        SenseeSentenceText(
            sentence = sentence,
            textStyle = typography.bodyLarge.copy(color = color),
            modifier = Modifier.alignByBaseline(),
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
