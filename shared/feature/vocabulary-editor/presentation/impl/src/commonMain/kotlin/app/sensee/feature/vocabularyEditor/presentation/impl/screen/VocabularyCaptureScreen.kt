package app.sensee.feature.vocabularyEditor.presentation.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureAction
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureComponent
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureUiState
import app.sensee.feature.vocabularyEditor.presentation.impl.component.grammarLabelsLoadStatusItem
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.component.textField.SenseeTextField
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import app.sensee.ui.senseCard.SenseCard
import com.composeunstyled.Text

// Grammar/usage labels still come native from GrammarLabels; only the UI chrome
// is localised via the TextProvider.
@Composable
public fun VocabularyCaptureScreen(
    component: VocabularyCaptureComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberVocabularyCaptureTextProvider(),
) {
    val uiState by component.uiState.collectAsState()
    val formState = rememberVocabularyCaptureFormState()
    val colors = SenseeTheme.colors
    val spacing = SenseeTheme.spacing
    val layoutMetrics = LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = layoutMetrics.screenHorizontalPadding,
                    top = layoutMetrics.screenVerticalPadding,
                    end = layoutMetrics.screenHorizontalPadding,
                    bottom = layoutMetrics.screenVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            captureIntro(textProvider, layoutMetrics)

            val confirmedTerm = uiState.confirmedTerm
            if (confirmedTerm != null) {
                confirmedTermBlock(component, formState, confirmedTerm, textProvider, layoutMetrics)
            } else {
                termInputBlock(component, uiState, formState, textProvider, layoutMetrics)
                candidateList(component, uiState, layoutMetrics)
                if (uiState.isPostSuggestionPhase) {
                    manualEntryBlock(component, uiState, formState, textProvider, layoutMetrics)
                    manualSenseList(component, uiState, textProvider, layoutMetrics)
                    confirmButton(component, uiState, textProvider, layoutMetrics)
                }
            }
        }
    }
}

/**
 * Form-side state for [VocabularyCaptureScreen]. Groups the term entry, the
 * manual-sense fields, and a reset hook for the "capture another" / "post-add"
 * paths so callbacks don't have to thread five remembers individually.
 */
@Stable
internal class VocabularyCaptureFormState(
    val term: TextFieldState,
    val manualTranslation: TextFieldState,
    val manualSurfaceForm: TextFieldState,
    val manualExample: TextFieldState,
) {
    var manualUnitType: GrammarUnitType? by mutableStateOf(null)

    fun resetAll() {
        term.setTextAndPlaceCursorAtEnd("")
        resetManual()
    }

    fun resetManual() {
        manualTranslation.setTextAndPlaceCursorAtEnd("")
        manualSurfaceForm.setTextAndPlaceCursorAtEnd("")
        manualExample.setTextAndPlaceCursorAtEnd("")
        manualUnitType = null
    }
}

@Composable
internal fun rememberVocabularyCaptureFormState(): VocabularyCaptureFormState {
    val term = rememberTextFieldState()
    val manualTranslation = rememberTextFieldState()
    val manualSurfaceForm = rememberTextFieldState()
    val manualExample = rememberTextFieldState()
    return remember(term, manualTranslation, manualSurfaceForm, manualExample) {
        VocabularyCaptureFormState(
            term = term,
            manualTranslation = manualTranslation,
            manualSurfaceForm = manualSurfaceForm,
            manualExample = manualExample,
        )
    }
}

// The "post-suggestion phase" lets the user add a missed sense or confirm the
// selection — gated on a settled, non-Loading enrichment lifecycle.
private val VocabularyCaptureUiState.isPostSuggestionPhase: Boolean
    get() = loadingState != DataLoadingState.Loading && loadingState != DataLoadingState.Idle

private fun LazyListScope.captureIntro(
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            IntroHeader(
                title = textProvider.text(VocabularyCaptureTextKeys.Title),
                intro = textProvider.text(VocabularyCaptureTextKeys.Intro),
            )
        }
    }
}

private fun LazyListScope.confirmedTermBlock(
    component: VocabularyCaptureComponent,
    formState: VocabularyCaptureFormState,
    confirmedTerm: String,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            ConfirmedTermText(
                text = textProvider.text(VocabularyCaptureTextKeys.Saved, confirmedTerm),
            )
        }
    }
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SenseeButton(
                onClick = {
                    formState.resetAll()
                    component.onAction(VocabularyCaptureAction.Reset)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(textProvider.text(VocabularyCaptureTextKeys.CaptureAnother))
            }
        }
    }
}

private fun LazyListScope.termInputBlock(
    component: VocabularyCaptureComponent,
    uiState: VocabularyCaptureUiState,
    formState: VocabularyCaptureFormState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SenseeTextField(
                state = formState.term,
                accessibilityLabel = textProvider.text(VocabularyCaptureTextKeys.TermLabel),
                label = { Text(textProvider.text(VocabularyCaptureTextKeys.TermLabel)) },
                placeholder = { Text(textProvider.text(VocabularyCaptureTextKeys.TermPlaceholder)) },
            )
        }
    }
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SenseeButton(
                onClick = {
                    component.onAction(VocabularyCaptureAction.Suggest(formState.term.text.toString()))
                },
                enabled = uiState.loadingState != DataLoadingState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    textProvider.text(
                        if (uiState.loadingState == DataLoadingState.Loading) {
                            VocabularyCaptureTextKeys.Working
                        } else {
                            VocabularyCaptureTextKeys.GetSuggestions
                        },
                    ),
                )
            }
        }
    }
    grammarLabelsLoadStatusItem(uiState, component, textProvider, layoutMetrics)
    uiState.statusNote?.let { note ->
        item {
            SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
                BodyMutedText(textProvider.statusNoteText(note))
            }
        }
    }
    if (uiState.loadingState is DataLoadingState.Error) {
        item {
            SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
                BodyMutedText(textProvider.text(VocabularyCaptureTextKeys.SuggestError))
            }
        }
    }
}

private fun LazyListScope.candidateList(
    component: VocabularyCaptureComponent,
    uiState: VocabularyCaptureUiState,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    items(uiState.candidates, key = { it.contentKey }) { candidate ->
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SenseCard(
                sense = candidate.sense,
                labels = uiState.grammarLabels,
                studyLanguageTag = uiState.studyLanguageTag,
                selected = candidate.selected,
                onClick = {
                    component.onAction(VocabularyCaptureAction.ToggleCandidate(candidate.contentKey))
                },
            )
        }
    }
}

private fun LazyListScope.manualEntryBlock(
    component: VocabularyCaptureComponent,
    uiState: VocabularyCaptureUiState,
    formState: VocabularyCaptureFormState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SectionTitle(textProvider.text(VocabularyCaptureTextKeys.MissedSense))
        }
    }
    manualTranslationItem(formState, textProvider, layoutMetrics)
    manualSurfaceFormItem(formState, textProvider, layoutMetrics)
    manualExampleItem(formState, textProvider, layoutMetrics)
    manualUnitTypeItem(uiState, formState, textProvider, layoutMetrics)
    manualAddButtonItem(component, formState, textProvider, layoutMetrics)
}

private fun LazyListScope.manualSenseList(
    component: VocabularyCaptureComponent,
    uiState: VocabularyCaptureUiState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    itemsIndexed(uiState.manualSenses) { manualIndex, sense ->
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            ManualSenseBlock(
                sense = sense,
                labels = uiState.grammarLabels,
                studyLanguageTag = uiState.studyLanguageTag,
                nativeLanguageTag = uiState.nativeLanguageTag,
                textProvider = textProvider,
                onComplete = {
                    component.onAction(VocabularyCaptureAction.CompleteManualWithAssistant(manualIndex))
                },
                onToggleSuggestion = { suggestionContentKey ->
                    component.onAction(
                        VocabularyCaptureAction.ToggleManualSuggestion(
                            manualIndex = manualIndex,
                            suggestionContentKey = suggestionContentKey,
                        ),
                    )
                },
            )
        }
    }
}

private fun LazyListScope.confirmButton(
    component: VocabularyCaptureComponent,
    uiState: VocabularyCaptureUiState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SenseeButton(
                onClick = { component.onAction(VocabularyCaptureAction.ConfirmSelected) },
                enabled = uiState.canConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(textProvider.text(VocabularyCaptureTextKeys.AddSelected))
            }
        }
    }
}

@Composable
private fun IntroHeader(
    title: String,
    intro: String,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Text(text = title, color = colors.textPrimary, style = typography.titleLarge)
        Text(text = intro, color = colors.textMuted, style = typography.bodyMedium)
    }
}

@Composable
private fun ConfirmedTermText(text: String) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    Text(text = text, color = colors.textPrimary, style = typography.titleMedium)
}

@Composable
private fun SectionTitle(text: String) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    Text(text = text, color = colors.textSecondary, style = typography.titleSmall)
}

@Composable
private fun BodyMutedText(text: String) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    Text(text = text, color = colors.textMuted, style = typography.bodyMedium)
}
