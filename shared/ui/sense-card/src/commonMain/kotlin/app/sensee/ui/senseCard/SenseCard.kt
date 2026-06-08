package app.sensee.ui.senseCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import app.sensee.core.presentation.text.TextProvider
import app.sensee.grammar.domain.GrammarLabelForm
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.SentenceSegment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceToken
import app.sensee.grammar.domain.UsageValue
import app.sensee.lexicon.domain.Sense
import app.sensee.ui.designSystem.component.badge.SenseeBadge
import app.sensee.ui.designSystem.component.badge.SenseeBadgeDefaults
import app.sensee.ui.designSystem.component.badge.SenseeBadgeFlow
import app.sensee.ui.designSystem.component.layout.SenseeLabeledSection
import app.sensee.ui.designSystem.component.senseCard.SenseeSenseCard
import app.sensee.ui.designSystem.component.sentence.SenseeSentence
import app.sensee.ui.designSystem.component.sentence.SenseeSentencePart
import app.sensee.ui.designSystem.component.sentence.SenseeSentenceText
import app.sensee.ui.designSystem.component.sentence.SenseeSentenceWordState
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * The full rich rendering of a single lexical [sense] as a card — shared by capture's
 * selectable candidate list and Library's deck browsing, so a sense looks identical wherever
 * it appears. Maps the rich [sense] (+ resolved grammar [labels]) into the domain-neutral
 * design-system [SenseeSenseCard]: surface form with an inline part-of-speech badge as the
 * title, translation + explanation + the first example as the primary tier, and the grammar
 * detail (preposition government, complementation, grammar tags, usage, irregular forms, note)
 * behind the expandable secondary slot.
 *
 * Selection: pass [onClick] to make the card a toggle and [selected] to show the accent border
 * + corner checkmark (capture's "selection IS confirmation", ADR-001). Leave [onClick] null for
 * a static, read-only card (Library browsing). [footer] attaches extra controls below the detail.
 *
 * [labels] resolve the part-of-speech / complementation / grammar / usage chip text; pass
 * [GrammarLabels.EMPTY] and the card degrades to raw taxonomy ids rather than failing.
 */
@Composable
public fun SenseCard(
    sense: Sense,
    labels: GrammarLabels,
    studyLanguageTag: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    initiallyExpanded: Boolean = false,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    textProvider: TextProvider = rememberSenseCardTextProvider(),
) {
    SenseeSenseCard(
        title = {
            sense.surfaceForm?.let { form ->
                SenseSurfaceFormTokens(tokens = form.tokens.toPersistentList())
            }
        },
        translation = sense.translation,
        modifier = modifier,
        explanation = sense.explanation,
        example =
            sense.contextualApplications
                .firstOrNull()
                ?.sentence
                ?.toSenseeSentence(),
        badge =
            sense.unitType?.let { type ->
                {
                    SenseeBadge(
                        text = labels.unitType(type, studyLanguageTag, GrammarLabelForm.Short) ?: type.id,
                        colors = SenseeBadgeDefaults.accentColors(),
                    )
                }
            },
        selected = selected,
        onClick = onClick,
        initiallyExpanded = initiallyExpanded,
        expandResetKey = sense,
        expandShowLabel = textProvider.text(SenseCardTextKeys.ToggleShow),
        expandHideLabel = textProvider.text(SenseCardTextKeys.ToggleHide),
        secondaryDetail =
            if (sense.hasSecondaryDetail()) {
                { SenseSecondaryDetail(sense, labels, studyLanguageTag, textProvider) }
            } else {
                null
            },
        footer = footer,
    )
}

@Composable
private fun SenseSurfaceFormTokens(
    tokens: ImmutableList<SurfaceToken>,
    modifier: Modifier = Modifier,
) {
    val spacing = SenseeTheme.spacing
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        tokens.forEach { token -> SurfaceTokenText(token) }
    }
}

@Composable
private fun SurfaceTokenText(token: SurfaceToken) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography

    when (token) {
        is SurfaceToken.Literal ->
            Text(
                text = token.text,
                color = colors.textPrimary,
                style = typography.titleMedium,
            )
        is SurfaceToken.Optional ->
            Text(
                text = "[${token.text}]",
                color = colors.textMuted,
                style = typography.titleMedium,
            )
        is SurfaceToken.Slot ->
            Text(
                text = "<${token.name}>",
                color = colors.textMuted,
                style = typography.titleMedium.copy(fontStyle = FontStyle.Italic),
            )
    }
}

@Composable
private fun SenseSecondaryDetail(
    sense: Sense,
    labels: GrammarLabels,
    studyLanguageTag: String,
    textProvider: TextProvider,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        sense.contextualApplications.drop(1).forEach {
            SenseeSentenceText(
                sentence = it.sentence.toSenseeSentence(),
                textStyle = typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        sense.governedPrepositions.forEach { government ->
            SenseeLabeledSection(label = textProvider.text(SenseCardTextKeys.Prepositions)) {
                SenseeBadgeFlow(labels = government.alternatives.toPersistentList())
                government.example?.let {
                    Text(text = it, color = colors.textMuted, style = typography.bodySmall)
                }
            }
        }

        val complementChips =
            sense.complementation
                .map { labels.complement(it, studyLanguageTag, GrammarLabelForm.Short) ?: it.id }
                .toPersistentList()
        if (complementChips.isNotEmpty()) {
            SenseeLabeledSection(
                label = textProvider.text(SenseCardTextKeys.Complementation),
            ) { SenseeBadgeFlow(labels = complementChips, colors = SenseeBadgeDefaults.accentSubtleColors()) }
        }

        val grammarChips =
            sense.grammarTags
                .map { labels.form(it, studyLanguageTag, GrammarLabelForm.Short) ?: it.form.id }
                .toPersistentList()
        if (grammarChips.isNotEmpty()) {
            SenseeLabeledSection(label = textProvider.text(SenseCardTextKeys.Grammar)) {
                SenseeBadgeFlow(labels = grammarChips, colors = SenseeBadgeDefaults.infoColors())
            }
        }

        val usageChips =
            sense.usageLabels
                .filterNot { it.value in NEUTRAL_USAGE_DEFAULTS }
                .map { labels.usage(it, studyLanguageTag, GrammarLabelForm.Short) ?: it.value.id }
                .toPersistentList()
        if (usageChips.isNotEmpty()) {
            SenseeLabeledSection(label = textProvider.text(SenseCardTextKeys.Usage)) {
                SenseeBadgeFlow(labels = usageChips, colors = SenseeBadgeDefaults.warningColors())
            }
        }

        sense.irregularForms?.let { forms ->
            SenseeLabeledSection(label = textProvider.text(SenseCardTextKeys.Forms)) {
                Text(
                    text = "${forms.base} / ${forms.past} / ${forms.pastParticiple}",
                    color = colors.textPrimary,
                    style = typography.bodyMedium,
                )
            }
        }

        sense.usageNote?.let { note ->
            SenseeLabeledSection(label = textProvider.text(SenseCardTextKeys.Note)) {
                Text(text = note, color = colors.textSecondary, style = typography.bodyMedium)
            }
        }
    }
}

private fun Sense.hasSecondaryDetail(): Boolean =
    contextualApplications.size > 1 ||
        governedPrepositions.isNotEmpty() ||
        complementation.isNotEmpty() ||
        grammarTags.isNotEmpty() ||
        usageLabels.any { it.value !in NEUTRAL_USAGE_DEFAULTS } ||
        irregularForms != null ||
        usageNote != null

internal val NEUTRAL_USAGE_DEFAULTS: Set<UsageValue> =
    setOf(
        UsageValue.NeutralRegister,
        UsageValue.NeutralConnotation,
        UsageValue.Current,
    )

// The studied unit is already a marked span (StudiedSentence) so the exact occurrence is
// known — the card accents it without fragile substring matching. The target stays a revealed
// accented Word; cloze/placeholder rendering belongs to practice.
internal fun StudiedSentence.toSenseeSentence(): SenseeSentence {
    var occurrence = 0
    return SenseeSentence(
        buildList {
            segments.forEach { segment ->
                when (segment) {
                    is SentenceSegment.Text ->
                        if (segment.value.isNotEmpty()) add(SenseeSentencePart.Text(segment.value))
                    is SentenceSegment.Target ->
                        add(
                            SenseeSentencePart.Word(
                                id = "target-${occurrence++}",
                                value = segment.value,
                                state = SenseeSentenceWordState.Target,
                            ),
                        )
                }
            }
        }.toPersistentList(),
    )
}
