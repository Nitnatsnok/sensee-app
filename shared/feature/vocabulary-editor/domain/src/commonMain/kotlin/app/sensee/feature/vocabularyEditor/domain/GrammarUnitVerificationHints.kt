package app.sensee.feature.vocabularyEditor.domain

import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.verification.core.LexicalEntryTypeHint
import app.sensee.verification.core.PartOfSpeechHint

internal fun List<SenseCandidate>.expectedEntryTypeHint(): LexicalEntryTypeHint? =
    mapNotNull { it.unitType?.toEntryTypeHint() }
        .distinct()
        .singleOrNull()

internal fun List<SenseCandidate>.expectedPartOfSpeechHint(): PartOfSpeechHint? =
    mapNotNull { it.unitType?.toPartOfSpeechHint() }
        .distinct()
        .singleOrNull()

private fun GrammarUnitType.toEntryTypeHint(): LexicalEntryTypeHint? {
    if (this is GrammarUnitType.Unknown) return null
    return LexicalEntryTypeHint(ENTRY_TYPE_BY_UNIT_TYPE_ID[id] ?: WORD_ENTRY_TYPE_ID)
}

internal fun GrammarUnitType.toPartOfSpeechHint(): PartOfSpeechHint? =
    PART_OF_SPEECH_BY_UNIT_TYPE_ID[id]?.let(::PartOfSpeechHint)

private const val WORD_ENTRY_TYPE_ID = "word"

private val ENTRY_TYPE_BY_UNIT_TYPE_ID: Map<String, String> =
    mapOf(
        GrammarUnitType.PhrasalVerb.id to "phrasal_verb",
        GrammarUnitType.Idiom.id to "idiom",
        GrammarUnitType.Phrase.id to "phrase",
    )

private val PART_OF_SPEECH_BY_UNIT_TYPE_ID: Map<String, String> =
    mapOf(
        GrammarUnitType.Noun.id to "noun",
        GrammarUnitType.Verb.id to "verb",
        GrammarUnitType.IrregularVerb.id to "verb",
        GrammarUnitType.PhrasalVerb.id to "verb",
        GrammarUnitType.ModalVerb.id to "verb",
        GrammarUnitType.AuxiliaryVerb.id to "verb",
        GrammarUnitType.Adjective.id to "adjective",
        GrammarUnitType.Adverb.id to "adverb",
        GrammarUnitType.Preposition.id to "preposition",
        GrammarUnitType.Conjunction.id to "conjunction",
        GrammarUnitType.Pronoun.id to "pronoun",
        GrammarUnitType.Determiner.id to "determiner",
        GrammarUnitType.Numeral.id to "numeral",
        GrammarUnitType.Article.id to "article",
        GrammarUnitType.Interjection.id to "interjection",
    )
