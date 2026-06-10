package app.sensee.feature.practice.presentation.impl.deck

import app.sensee.feature.practice.presentation.api.DeckPracticeSpeechPhase
import app.sensee.feature.practice.presentation.api.DeckPracticeSpeechUiState
import app.sensee.tts.core.Speaker
import app.sensee.tts.core.SpeechHandle
import app.sensee.tts.core.SpeechLocale
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class DeckPracticeSpeechController(
    private val speaker: Speaker,
) {
    val state: StateFlow<DeckPracticeSpeechUiState>
        field = MutableStateFlow(DeckPracticeSpeechUiState())

    private var currentHandle: SpeechHandle? = null
    private var currentStateJob: Job? = null

    fun speak(
        targetId: String,
        text: String,
        locale: SpeechLocale,
        scope: CoroutineScope,
    ) {
        if (text.isBlank()) return
        val current = state.value
        if (current.activeTargetId == targetId && current.phase != DeckPracticeSpeechPhase.Idle) {
            stop()
            return
        }

        cancelCurrentHandle()

        val handle = speaker.speak(SpeechRequest(text = text, locale = locale))
        currentHandle = handle
        state.update {
            DeckPracticeSpeechUiState(
                activeTargetId = targetId,
                phase = DeckPracticeSpeechPhase.Loading,
            )
        }
        observe(handle = handle, targetId = targetId, scope = scope)
    }

    fun stop() {
        cancelCurrentHandle()
        state.update { DeckPracticeSpeechUiState() }
    }

    fun stopAll() {
        stop()
        speaker.stop()
    }

    private fun cancelCurrentHandle() {
        currentStateJob?.cancel()
        currentStateJob = null
        currentHandle?.cancel()
        currentHandle = null
    }

    private fun observe(
        handle: SpeechHandle,
        targetId: String,
        scope: CoroutineScope,
    ) {
        currentStateJob =
            scope.launch {
                handle.state
                    .filter { it != SpeechState.Idle }
                    .onEach { state -> updateFromHandle(handle, targetId, state) }
                    .first { it.isTerminal() }
                if (currentHandle === handle) {
                    currentHandle = null
                    currentStateJob = null
                    state.update { DeckPracticeSpeechUiState() }
                }
            }
    }

    private fun updateFromHandle(
        handle: SpeechHandle,
        targetId: String,
        speechState: SpeechState,
    ) {
        if (currentHandle !== handle) return
        val phase =
            when (speechState) {
                SpeechState.Loading -> DeckPracticeSpeechPhase.Loading
                SpeechState.Speaking -> DeckPracticeSpeechPhase.Speaking
                SpeechState.Idle,
                SpeechState.Done,
                is SpeechState.Failed,
                -> return
            }
        state.update {
            DeckPracticeSpeechUiState(
                activeTargetId = targetId,
                phase = phase,
            )
        }
    }
}

private fun SpeechState.isTerminal(): Boolean =
    when (this) {
        SpeechState.Done,
        is SpeechState.Failed,
        -> true
        SpeechState.Idle,
        SpeechState.Loading,
        SpeechState.Speaking,
        -> false
    }
