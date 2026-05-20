package app.sensee.ui.designSystem.component.sentence

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class SenseeSentence(
    val parts: ImmutableList<SenseeSentencePart>,
)

@Immutable
public sealed interface SenseeSentencePart {
    @Immutable
    public data class Text(
        val value: String,
    ) : SenseeSentencePart

    @Immutable
    public data class Word(
        val id: String,
        val value: String,
        val state: SenseeSentenceWordState = SenseeSentenceWordState.Normal,
        val highlights: ImmutableList<SenseeTextRangeHighlight> = persistentListOf(),
    ) : SenseeSentencePart

    /**
     * Visual answer slot inside a sentence.
     *
     * Simple gap:
     * Placeholder(id = "answer", groupId = "answer")
     *
     * Split phrasal verb:
     * Placeholder(id = "answer-verb", groupId = "answer", slotIndex = 0, slotCount = 2)
     * Text(" it ")
     * Placeholder(id = "answer-particle", groupId = "answer", slotIndex = 1, slotCount = 2)
     */
    @Immutable
    public data class Placeholder(
        val id: String,
        val groupId: String = id,
        val slotIndex: Int = 0,
        val slotCount: Int = 1,
        val wordCount: Int = 1,
        /** Text shown inside the pill when it has been filled in. `null` = empty. */
        val value: String? = null,
        /**
         * Hint for the word that will fill this placeholder when answered. Used only for
         * sizing the empty pill so it visually approximates the eventual answer's width.
         * `null` means "no hint available", and the pill falls back to a generic
         * [SenseeSentencePlaceholderMetrics.emptySingleWordWidthEm]-based width.
         */
        val expectedText: String? = null,
        val state: SenseeSentencePlaceholderState = SenseeSentencePlaceholderState.Empty,
    ) : SenseeSentencePart
}

@Immutable
public data class SenseeTextRangeHighlight(
    val start: Int,
    val end: Int,
    val style: SenseeSentenceHighlightStyle = SenseeSentenceHighlightStyle.Accent,
)

public enum class SenseeSentenceHighlightStyle {
    Accent,
    Muted,
    Danger,
    Success,
    Grammar,
}

public enum class SenseeSentenceWordState {
    Normal,
    Target,
    Selected,
    Correct,
    Incorrect,
}

public enum class SenseeSentencePlaceholderState {
    Empty,
    Selected,
    Filled,
    Correct,
    Incorrect,
}

@Immutable
public sealed interface SenseeSentenceClick {
    @Immutable
    public data class Word(
        val id: String,
    ) : SenseeSentenceClick

    @Immutable
    public data class Placeholder(
        val id: String,
        val groupId: String,
        val slotIndex: Int,
    ) : SenseeSentenceClick
}
