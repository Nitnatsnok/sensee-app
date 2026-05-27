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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class DeckPracticeSpeechController(
    private val speaker: Speaker,
) {
    private val mutableState = MutableStateFlow(DeckPracticeSpeechUiState())
    val state: StateFlow<DeckPracticeSpeechUiState> = mutableState.asStateFlow()

    private var currentHandle: SpeechHandle? = null
    private var currentStateJob: Job? = null

    fun speak(
        targetId: String,
        text: String,
        locale: SpeechLocale,
        scope: CoroutineScope,
    ) {
        if (text.isBlank()) return
        val current = mutableState.value
        if (current.activeTargetId == targetId && current.phase != DeckPracticeSpeechPhase.Idle) {
            stop()
            return
        }

        cancelCurrentHandle()

        val handle = speaker.speak(SpeechRequest(text = text, locale = locale))
        currentHandle = handle
        mutableState.update {
            DeckPracticeSpeechUiState(
                activeTargetId = targetId,
                phase = DeckPracticeSpeechPhase.Loading,
            )
        }
        observe(handle = handle, targetId = targetId, scope = scope)
    }

    fun stop() {
        cancelCurrentHandle()
        mutableState.update { DeckPracticeSpeechUiState() }
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
                    mutableState.update { DeckPracticeSpeechUiState() }
                }
            }
    }

    private fun updateFromHandle(
        handle: SpeechHandle,
        targetId: String,
        state: SpeechState,
    ) {
        if (currentHandle !== handle) return
        val phase =
            when (state) {
                SpeechState.Loading -> DeckPracticeSpeechPhase.Loading
                SpeechState.Speaking -> DeckPracticeSpeechPhase.Speaking
                SpeechState.Idle,
                SpeechState.Done,
                is SpeechState.Failed,
                -> return
            }
        mutableState.update {
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
