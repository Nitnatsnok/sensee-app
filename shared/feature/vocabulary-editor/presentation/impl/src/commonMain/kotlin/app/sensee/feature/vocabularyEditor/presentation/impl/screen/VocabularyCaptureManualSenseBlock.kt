package app.sensee.feature.vocabularyEditor.presentation.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.vocabularyEditor.presentation.api.CaptureSense
import app.sensee.feature.vocabularyEditor.presentation.api.CaptureSenseStatus
import app.sensee.feature.vocabularyEditor.presentation.impl.card.SenseSelectionCard
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.button.SenseeButtonColors
import app.sensee.ui.designSystem.component.layout.SenseeSurface
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

// The hand-authored sense is not itself a candidate (no select toggle — it is
// always added unless an assistant version replaces it), so it renders as a
// plain surface, while any assistant suggestions reuse the candidate card.
@Composable
internal fun ManualSenseBlock(
    sense: CaptureSense,
    labels: GrammarLabels,
    studyLanguageTag: String,
    nativeLanguageTag: String,
    textProvider: TextProvider,
    onComplete: () -> Unit,
    onToggleSuggestion: (String) -> Unit,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing

    SenseeSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = textProvider.text(VocabularyCaptureTextKeys.YourSense),
                color = colors.textSecondary,
                style = typography.labelMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                sense.sense.surfaceForm?.let {
                    Text(text = it.display(), color = colors.textPrimary, style = typography.titleMedium)
                }
                sense.sense.unitType?.let { type ->
                    Text(
                        text = labels.unitType(type, nativeLanguageTag) ?: type.id,
                        color = colors.textMuted,
                        style = typography.bodyMedium,
                    )
                }
            }
            Text(
                text = sense.sense.translation,
                color = colors.textPrimary,
                style = typography.bodyLarge,
            )

            ManualSenseStatusContent(
                sense = sense,
                labels = labels,
                studyLanguageTag = studyLanguageTag,
                textProvider = textProvider,
                onComplete = onComplete,
                onToggleSuggestion = onToggleSuggestion,
            )
        }
    }
}

@Composable
private fun ManualSenseStatusContent(
    sense: CaptureSense,
    labels: GrammarLabels,
    studyLanguageTag: String,
    textProvider: TextProvider,
    onComplete: () -> Unit,
    onToggleSuggestion: (String) -> Unit,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    when (sense.status) {
        CaptureSenseStatus.Completing ->
            Text(
                text = textProvider.text(VocabularyCaptureTextKeys.Completing),
                color = colors.textMuted,
                style = typography.bodyMedium,
            )

        CaptureSenseStatus.CompleteFailed -> {
            Text(
                text = textProvider.text(VocabularyCaptureTextKeys.CompleteFailed),
                color = colors.textMuted,
                style = typography.bodySmall,
            )
            CompleteWithAssistantButton(onComplete, textProvider)
        }

        // A manual sense the assistant has not (yet) completed shows the action;
        // once it carries suggestions, the user picks one to replace the draft.
        CaptureSenseStatus.Ready ->
            if (sense.assistantSuggestions.isEmpty()) {
                CompleteWithAssistantButton(onComplete, textProvider)
            } else {
                Text(
                    text = textProvider.text(VocabularyCaptureTextKeys.PickAssistantVersion),
                    color = colors.textSecondary,
                    style = typography.bodySmall,
                )
                sense.assistantSuggestions.forEach { suggestion ->
                    SenseSelectionCard(
                        candidate = suggestion.sense,
                        labels = labels,
                        studyLanguageTag = studyLanguageTag,
                        textProvider = textProvider,
                        selected = suggestion.selected,
                        onToggle = { onToggleSuggestion(suggestion.contentKey) },
                    )
                }
            }
    }
}

@Composable
private fun CompleteWithAssistantButton(
    onComplete: () -> Unit,
    textProvider: TextProvider,
) {
    SenseeButton(
        onClick = onComplete,
        colors = SenseeButtonColors.outlined(),
    ) {
        Text(textProvider.text(VocabularyCaptureTextKeys.CompleteWithAssistant))
    }
}
