package app.sensee.feature.practice.presentation.impl.deck

import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.feature.practice.presentation.api.DeckPracticeSpeechPhase
import app.sensee.feature.practice.presentation.api.DeckPracticeSpeechUiState
import app.sensee.tts.core.Speaker
import app.sensee.tts.core.SpeechHandle
import app.sensee.tts.core.SpeechLocale
import app.sensee.tts.core.SpeechRequest
import app.sensee.tts.core.SpeechState
import app.sensee.tts.core.TtsError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class DeckPracticeSpeechControllerTest {
    @Test
    fun `speech state moves from loading to speaking and clears after completion`() {
        val scope = testScope()
        val speaker = FakeSpeaker()
        val controller = DeckPracticeSpeechController(speaker)

        try {
            controller.speak(
                targetId = "card-1:headword",
                text = "hello",
                locale = SpeechLocale.Russian,
                scope = scope,
            )
            assertEquals(
                DeckPracticeSpeechUiState("card-1:headword", DeckPracticeSpeechPhase.Loading),
                controller.state.value,
            )
            assertEquals(SpeechLocale.Russian, speaker.lastRequest.locale)

            speaker.lastHandle.update(SpeechState.Speaking)
            assertEquals(
                DeckPracticeSpeechUiState("card-1:headword", DeckPracticeSpeechPhase.Speaking),
                controller.state.value,
            )

            speaker.lastHandle.update(SpeechState.Done)
            assertEquals(DeckPracticeSpeechUiState(), controller.state.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `clicking the active speaking target cancels playback`() {
        val scope = testScope()
        val speaker = FakeSpeaker()
        val controller = DeckPracticeSpeechController(speaker)

        try {
            controller.speak(
                targetId = "card-1:headword",
                text = "hello",
                locale = SpeechLocale.English,
                scope = scope,
            )
            speaker.lastHandle.update(SpeechState.Speaking)

            controller.speak(
                targetId = "card-1:headword",
                text = "hello",
                locale = SpeechLocale.English,
                scope = scope,
            )

            assertEquals(1, speaker.lastHandle.cancelCount)
            assertEquals(DeckPracticeSpeechUiState(), controller.state.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `starting another target cancels the previous handle and tracks the new target`() {
        val scope = testScope()
        val speaker = FakeSpeaker()
        val controller = DeckPracticeSpeechController(speaker)

        try {
            controller.speak(
                targetId = "card-1:headword",
                text = "same",
                locale = SpeechLocale.English,
                scope = scope,
            )
            val firstHandle = speaker.lastHandle

            controller.speak(
                targetId = "card-2:headword",
                text = "same",
                locale = SpeechLocale.English,
                scope = scope,
            )

            assertEquals(1, firstHandle.cancelCount)
            assertEquals(
                DeckPracticeSpeechUiState("card-2:headword", DeckPracticeSpeechPhase.Loading),
                controller.state.value,
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `failed speech clears the active indicator`() {
        val scope = testScope()
        val speaker = FakeSpeaker()
        val controller = DeckPracticeSpeechController(speaker)

        try {
            controller.speak(
                targetId = "card-1:headword",
                text = "hello",
                locale = SpeechLocale.English,
                scope = scope,
            )
            speaker.lastHandle.update(SpeechState.Speaking)

            speaker.lastHandle.update(SpeechState.Failed(TtsError.Unknown("playback failed")))

            assertEquals(DeckPracticeSpeechUiState(), controller.state.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `blank speech text is ignored without creating a handle`() {
        val scope = testScope()
        val speaker = FakeSpeaker()
        val controller = DeckPracticeSpeechController(speaker)

        try {
            controller.speak(
                targetId = "card-1:headword",
                text = "   ",
                locale = SpeechLocale.English,
                scope = scope,
            )

            assertEquals(0, speaker.requestCount)
            assertEquals(DeckPracticeSpeechUiState(), controller.state.value)
        } finally {
            scope.cancel()
        }
    }

    private fun testScope(): CoroutineScope {
        val dispatchers = immediateAppDispatchers()
        return CoroutineScope(dispatchers.main.immediate + SupervisorJob())
    }

    private class FakeSpeaker : Speaker {
        private val handles = mutableListOf<FakeSpeechHandle>()
        private val recordedRequests = mutableListOf<SpeechRequest>()

        val lastHandle: FakeSpeechHandle
            get() = handles.last()

        val lastRequest: SpeechRequest
            get() = recordedRequests.last()

        val requestCount: Int
            get() = recordedRequests.size

        override fun speak(request: SpeechRequest): SpeechHandle {
            recordedRequests += request
            return FakeSpeechHandle().also(handles::add)
        }

        override fun stop() {
            handles.forEach { it.cancel() }
        }
    }

    private class FakeSpeechHandle : SpeechHandle {
        private val mutableState = MutableStateFlow<SpeechState>(SpeechState.Loading)
        override val state: StateFlow<SpeechState> = mutableState.asStateFlow()
        var cancelCount = 0

        fun update(state: SpeechState) {
            mutableState.value = state
        }

        override fun cancel() {
            cancelCount++
            mutableState.value = SpeechState.Failed(TtsError.Cancelled())
        }
    }
}
