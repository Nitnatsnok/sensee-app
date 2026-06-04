package app.sensee.feature.vocabularyEditor.presentation.impl.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.vocabularyEditor.presentation.impl.screen.VocabularyCaptureTextKeys
import app.sensee.grammar.domain.GrammarLabelForm
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.SentenceSegment
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.grammar.domain.SurfaceToken
import app.sensee.grammar.domain.UsageValue
import app.sensee.lexicon.domain.Sense
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.badge.SenseeBadge
import app.sensee.ui.designSystem.component.badge.SenseeBadgeColors
import app.sensee.ui.designSystem.component.badge.SenseeBadgeDefaults
import app.sensee.ui.designSystem.component.layout.SenseeSurface
import app.sensee.ui.designSystem.component.layout.SenseeSurfaceDefaults
import app.sensee.ui.designSystem.component.sentence.SenseeSentence
import app.sensee.ui.designSystem.component.sentence.SenseeSentencePart
import app.sensee.ui.designSystem.component.sentence.SenseeSentenceText
import app.sensee.ui.designSystem.component.sentence.SenseeSentenceWordState
import app.sensee.ui.designSystem.icons.ArrowDownwardAlt24px
import app.sensee.ui.designSystem.icons.Check
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

/**
 * One AI-proposed sense as a selectable card. The whole surface toggles
 * selection (the user picks which senses to add — selection IS the
 * confirmation, ADR-001); a selected card gets an accent border and a small
 * checkmark icon tucked into the top-right corner. The primary tier carries
 * only what disambiguates a sense (surface form with an inline part-of-speech
 * badge, translation, explanation, one example); the rest of the grammar
 * detail is behind a per-card expand so a rich phrasal verb does not bury the
 * choice. [footer] lets the screen attach extra controls (e.g. "complete with
 * assistant" for a hand-authored sense) without the card knowing about that
 * flow. [initiallyExpanded] seeds the expand state (previews / tests).
 */
@Composable
internal fun SenseSelectionCard(
    candidate: Sense,
    labels: GrammarLabels,
    studyLanguageTag: String,
    textProvider: TextProvider,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing

    var expanded by remember(candidate) { mutableStateOf(initiallyExpanded) }

    val surfaceColors =
        if (selected) {
            SenseeSurfaceDefaults.colors(border = colors.accent)
        } else {
            SenseeSurfaceDefaults.colors()
        }

    val hasSecondaryDetail = candidate.hasSecondaryDetail()
    val hasTrailingBlock = hasSecondaryDetail || footer != null

    SenseeSurface(
        // The whole card is the toggle; expose selection to assistive tech
        // (the corner checkmark + accent border are the visual cues).
        modifier = modifier.fillMaxWidth().semantics { this.selected = selected },
        onClick = onToggle,
        colors = surfaceColors,
        // The expand handle is edge-to-edge (hairline across the whole card,
        // chevron Box bottom-touching the surface border when collapsed). The
        // surface drops its own padding so each section controls its inset.
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            // Primary tier — padded inset block. Bottom padding is suppressed
            // when a handle or footer follows; the handle's own 40dp chevron
            // area, or the trailing block's bottom padding, takes its role.
            Column(
                modifier =
                    Modifier.padding(
                        start = spacing.large,
                        top = spacing.large,
                        end = spacing.large,
                        bottom = if (hasTrailingBlock) 0.dp else spacing.large,
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                // Header + meaning are one tight group: the surface form with the
                // part-of-speech badge inline, then the translation as the visual
                // anchor, then the explanation. A looser gap sets the example apart.
                Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                    SenseCandidateTitleRow(
                        candidate = candidate,
                        labels = labels,
                        studyLanguageTag = studyLanguageTag,
                        selected = selected,
                    )
                    Text(
                        text = candidate.translation,
                        color = colors.textPrimary,
                        style = typography.titleSmall,
                    )
                    candidate.explanation?.let {
                        Text(text = it, color = colors.textSecondary, style = typography.bodyMedium)
                    }
                }
                candidate.contextualApplications.firstOrNull()?.let {
                    SenseeSentenceText(
                        sentence = it.sentence.toSenseeSentence(),
                        textStyle = typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (hasSecondaryDetail) {
                SenseExpandHandle(
                    expanded = expanded,
                    onToggle = { expanded = !expanded },
                    textProvider = textProvider,
                )
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(trailingInset(spacing.large))) {
                        SenseSecondaryDetail(candidate, labels, studyLanguageTag, textProvider)
                    }
                }
            }
            footer?.let { content ->
                Column(modifier = Modifier.padding(trailingInset(spacing.large))) {
                    content()
                }
            }
        }
    }
}

private fun trailingInset(side: Dp): PaddingValues = PaddingValues(start = side, end = side, bottom = side)

@Composable
private fun SenseCandidateTitleRow(
    candidate: Sense,
    labels: GrammarLabels,
    studyLanguageTag: String,
    selected: Boolean,
) {
    val colors = SenseeTheme.colors
    val spacing = SenseeTheme.spacing

    // Title row reserves a fixed-size slot at the trailing edge for the corner
    // checkmark, so toggling selection does not shift the surface form layout.
    // The part-of-speech badge sits inline between the form and that slot.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        candidate.surfaceForm?.let { form ->
            SenseSurfaceFormTokens(
                tokens = form.tokens.toPersistentList(),
                modifier = Modifier.weight(1f),
            )
        } ?: Spacer(Modifier.weight(1f))

        candidate.unitType?.let { type ->
            SenseeBadge(
                text = labels.unitType(type, studyLanguageTag, GrammarLabelForm.Short) ?: type.id,
                colors = SenseeBadgeDefaults.accentColors(),
            )
        }

        if (selected) {
            SenseeIcon(
                imageVector = Check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(CornerCheckSize),
            )
        } else {
            Spacer(Modifier.size(CornerCheckSize))
        }
    }
}

/**
 * Full-width expand affordance: thin hairline above a centered chevron that
 * rotates 180° on expand. Spans the card edge-to-edge so the tap target is
 * generous; sits visually as a separator between the primary block and the
 * detail section, even when the detail is collapsed.
 */
@Composable
private fun SenseExpandHandle(
    expanded: Boolean,
    onToggle: () -> Unit,
    textProvider: TextProvider,
) {
    val colors = SenseeTheme.colors
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)
    val label =
        textProvider.text(
            if (expanded) {
                VocabularyCaptureTextKeys.DetailToggleHide
            } else {
                VocabularyCaptureTextKeys.DetailToggleShow
            },
        )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle, onClickLabel = label),
    ) {
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(HandleHairlineThickness)
                    .background(colors.textMuted.copy(alpha = HANDLE_HAIRLINE_ALPHA)),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = HandleTapHeight),
            contentAlignment = Alignment.Center,
        ) {
            SenseeIcon(
                imageVector = ArrowDownwardAlt24px,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.rotate(rotation),
            )
        }
    }
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
    candidate: Sense,
    labels: GrammarLabels,
    studyLanguageTag: String,
    textProvider: TextProvider,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        candidate.contextualApplications.drop(1).forEach {
            SenseeSentenceText(
                sentence = it.sentence.toSenseeSentence(),
                textStyle = typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        candidate.governedPrepositions.forEach { government ->
            DetailBlock(label = textProvider.text(VocabularyCaptureTextKeys.DetailPrepositions)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                    government.alternatives.forEach { SenseeBadge(text = it) }
                }
                government.example?.let {
                    Text(text = it, color = colors.textMuted, style = typography.bodySmall)
                }
            }
        }

        val complementChips =
            candidate.complementation
                .map { labels.complement(it, studyLanguageTag, GrammarLabelForm.Short) ?: it.id }
                .toPersistentList()
        if (complementChips.isNotEmpty()) {
            DetailBlock(
                label = textProvider.text(VocabularyCaptureTextKeys.DetailComplementation),
            ) { ChipFlow(complementChips, SenseeBadgeDefaults.accentSubtleColors()) }
        }

        val grammarChips =
            candidate.grammarTags
                .map { labels.form(it, studyLanguageTag, GrammarLabelForm.Short) ?: it.form.id }
                .toPersistentList()
        if (grammarChips.isNotEmpty()) {
            DetailBlock(label = textProvider.text(VocabularyCaptureTextKeys.DetailGrammar)) {
                ChipFlow(grammarChips, SenseeBadgeDefaults.infoColors())
            }
        }

        val usageChips =
            candidate.usageLabels
                .filterNot { it.value in NEUTRAL_USAGE_DEFAULTS }
                .map { labels.usage(it, studyLanguageTag, GrammarLabelForm.Short) ?: it.value.id }
                .toPersistentList()
        if (usageChips.isNotEmpty()) {
            DetailBlock(label = textProvider.text(VocabularyCaptureTextKeys.DetailUsage)) {
                ChipFlow(usageChips, SenseeBadgeDefaults.warningColors())
            }
        }

        candidate.irregularForms?.let { forms ->
            DetailBlock(label = textProvider.text(VocabularyCaptureTextKeys.DetailForms)) {
                Text(
                    text = "${forms.base} / ${forms.past} / ${forms.pastParticiple}",
                    color = colors.textPrimary,
                    style = typography.bodyMedium,
                )
            }
        }

        candidate.usageNote?.let { note ->
            DetailBlock(label = textProvider.text(VocabularyCaptureTextKeys.DetailNote)) {
                Text(text = note, color = colors.textSecondary, style = typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DetailBlock(
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Text(text = label, color = colors.textSecondary, style = typography.labelMedium)
        content()
    }
}

@Composable
private fun ChipFlow(
    chips: ImmutableList<String>,
    colors: SenseeBadgeColors,
) {
    val spacing = SenseeTheme.spacing
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        modifier = Modifier.fillMaxWidth(),
    ) {
        chips.forEach { SenseeBadge(text = it, colors = colors) }
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

private val CornerCheckSize = 20.dp
private val HandleTapHeight = 40.dp
private val HandleHairlineThickness = 1.dp
private const val HANDLE_HAIRLINE_ALPHA = 0.2f

internal val NEUTRAL_USAGE_DEFAULTS: Set<UsageValue> =
    setOf(
        UsageValue.NeutralRegister,
        UsageValue.NeutralConnotation,
        UsageValue.Current,
    )

// The studied unit is already a marked span (StudiedSentence) so the exact
// occurrence is known — the preview accents it without fragile substring
// matching. Capture is a preview, so the target stays a revealed accented
// Word; cloze/placeholder rendering belongs to practice.
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
