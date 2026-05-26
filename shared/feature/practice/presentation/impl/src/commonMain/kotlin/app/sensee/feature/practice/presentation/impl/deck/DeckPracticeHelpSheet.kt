package app.sensee.feature.practice.presentation.impl.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.practice.presentation.impl.grammar.grammarUnitBadgeColors
import app.sensee.feature.practice.presentation.impl.text.PracticeTextKeys
import app.sensee.feature.practice.presentation.impl.text.description
import app.sensee.feature.practice.presentation.impl.text.rememberPracticeTextProvider
import app.sensee.feature.practice.presentation.impl.text.shortLabel
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.ui.designSystem.component.badge.SenseeBadge
import app.sensee.ui.designSystem.component.badge.SenseeBadgeColors
import app.sensee.ui.designSystem.component.badge.SenseeBadgeDefaults
import app.sensee.ui.designSystem.component.layout.SenseeModalBottomSheet
import app.sensee.ui.designSystem.component.layout.SenseeSheetHeader
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

/**
 * Help sheet for the practice screen. Lists every badge that can appear next to a card's
 * headword with its short dictionary-style abbreviation and a long human-readable
 * description, grouped by part-of-speech and grammatical features.
 *
 * The sheet has no decompose component of its own — it's driven by a `Boolean` local state
 * in [DeckPracticeScreen] toggled from the top bar's help icon — because the content is
 * purely static (a glossary), with no navigation or shared state to coordinate.
 */
@Composable
internal fun DeckPracticeHelpSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    labels: GrammarLabels,
    studyLanguageTag: String,
    nativeLanguageTag: String,
    textProvider: TextProvider = rememberPracticeTextProvider(),
) {
    SenseeModalBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        contentPadding = PaddingValues(0.dp),
    ) {
        val spacing = SenseeTheme.spacing

        SenseeSheetHeader(
            onClose = onDismiss,
            closeAccessibilityLabel = textProvider.text(PracticeTextKeys.ActionClose),
        ) {
            Text(
                text = textProvider.text(PracticeTextKeys.HelpSheetTitle),
                style = SenseeTheme.typography.titleMedium,
            )
        }
        Spacer(modifier = Modifier.height(spacing.small))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            contentPadding =
                PaddingValues(
                    start = spacing.large,
                    end = spacing.large,
                    bottom = spacing.large,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            item {
                HelpSectionLabel(
                    text = textProvider.text(PracticeTextKeys.HelpSectionUnitTypes),
                )
            }
            items(GrammarUnitType.knownEntries, key = { "unit-${it.id}" }) { unitType ->
                HelpRow(
                    badgeText = unitType.shortLabel(labels, studyLanguageTag),
                    badgeColors = grammarUnitBadgeColors(unitType),
                    description = unitType.description(labels, nativeLanguageTag),
                )
            }
            item {
                Spacer(modifier = Modifier.height(spacing.medium))
                HelpSectionLabel(
                    text = textProvider.text(PracticeTextKeys.HelpSectionGrammarForms),
                )
            }
            items(GrammarForm.knownEntries, key = { "form-${it.id}" }) { form ->
                HelpRow(
                    badgeText = form.shortLabel(labels, studyLanguageTag),
                    badgeColors = SenseeBadgeDefaults.neutralColors(),
                    description = form.description(labels, nativeLanguageTag),
                )
            }
        }
    }
}

@Composable
private fun HelpSectionLabel(text: String) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    Text(
        text = text,
        color = colors.textMuted,
        style = typography.labelMedium,
    )
}

@Composable
private fun HelpRow(
    badgeText: String,
    badgeColors: SenseeBadgeColors,
    description: String,
) {
    val spacing = SenseeTheme.spacing
    val typography = SenseeTheme.typography
    val colors = SenseeTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        // Anchor the badge column so descriptions line up vertically even when abbreviations
        // have different widths ("n." vs "v. irr.").
        Column(
            modifier = Modifier.fillMaxWidth(fraction = 0.32f),
            horizontalAlignment = Alignment.Start,
        ) {
            SenseeBadge(text = badgeText, colors = badgeColors)
        }
        Text(
            text = description,
            modifier = Modifier.fillMaxWidth(),
            color = colors.textPrimary,
            style = typography.bodyMedium,
        )
    }
}
