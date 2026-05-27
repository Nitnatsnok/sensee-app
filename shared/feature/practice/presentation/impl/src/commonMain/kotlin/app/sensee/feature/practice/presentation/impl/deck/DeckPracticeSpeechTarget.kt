package app.sensee.feature.practice.presentation.impl.deck

import app.sensee.feature.practice.presentation.api.DeckPracticeAction
import app.sensee.feature.practice.presentation.api.DeckPracticeCardUiState
import app.sensee.feature.practice.presentation.api.DeckPracticeSpeechPhase
import app.sensee.feature.practice.presentation.api.DeckPracticeSpeechUiState
import app.sensee.ui.designSystem.component.button.SenseeSpeakIconButtonState

internal fun DeckPracticeSpeechUiState.buttonStateFor(
    card: DeckPracticeCardUiState,
    target: SpeechTarget,
): SenseeSpeakIconButtonState =
    if (activeTargetId != target.id(card)) {
        SenseeSpeakIconButtonState.Idle
    } else {
        when (phase) {
            DeckPracticeSpeechPhase.Idle -> SenseeSpeakIconButtonState.Idle
            DeckPracticeSpeechPhase.Loading -> SenseeSpeakIconButtonState.Loading
            DeckPracticeSpeechPhase.Speaking -> SenseeSpeakIconButtonState.Speaking
        }
    }

internal fun speakTextAction(
    card: DeckPracticeCardUiState,
    target: SpeechTarget,
): DeckPracticeAction.SpeakText =
    DeckPracticeAction.SpeakText(
        targetId = target.id(card),
        text =
            when (target) {
                SpeechTarget.Headword -> card.headword
                SpeechTarget.ContextSentence -> card.contextSentence
            },
    )

internal enum class SpeechTarget(
    private val suffix: String,
) {
    Headword("headword"),
    ContextSentence("context-sentence"),
    ;

    fun id(card: DeckPracticeCardUiState): String = "${card.presentationKey}:$suffix"
}
