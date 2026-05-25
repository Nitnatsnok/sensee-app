package app.sensee.feature.vocabularyEditor.presentation.impl

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextKey
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.vocabularyEditor.presentation.api.CaptureStatusNote

internal object VocabularyCaptureTextKeys {
    val Title = TextKey("vocabulary_capture.title")
    val Intro = TextKey("vocabulary_capture.intro")
    val Saved = TextKey("vocabulary_capture.saved")
    val CaptureAnother = TextKey("vocabulary_capture.capture_another")
    val TermLabel = TextKey("vocabulary_capture.term_label")
    val TermPlaceholder = TextKey("vocabulary_capture.term_placeholder")
    val Working = TextKey("vocabulary_capture.working")
    val GetSuggestions = TextKey("vocabulary_capture.get_suggestions")
    val SuggestError = TextKey("vocabulary_capture.suggest_error")
    val AiUnavailableNote = TextKey("vocabulary_capture.ai_unavailable_note")
    val AiDegradedNote = TextKey("vocabulary_capture.ai_degraded_note")
    val MissedSense = TextKey("vocabulary_capture.missed_sense")
    val ManualMeaningLabel = TextKey("vocabulary_capture.manual_meaning_label")
    val ManualMeaningPlaceholder = TextKey("vocabulary_capture.manual_meaning_placeholder")
    val SurfaceFormLabel = TextKey("vocabulary_capture.surface_form_label")
    val SurfaceFormPlaceholder = TextKey("vocabulary_capture.surface_form_placeholder")
    val PartOfSpeechLabel = TextKey("vocabulary_capture.part_of_speech_label")
    val PartOfSpeechUnset = TextKey("vocabulary_capture.part_of_speech_unset")
    val AddOwnVariant = TextKey("vocabulary_capture.add_own_variant")
    val AddSelected = TextKey("vocabulary_capture.add_selected")
    val YourSense = TextKey("vocabulary_capture.your_sense")
    val CompleteFailed = TextKey("vocabulary_capture.complete_failed")
    val CompleteWithAssistant = TextKey("vocabulary_capture.complete_with_assistant")
    val Completing = TextKey("vocabulary_capture.completing")
    val PickAssistantVersion = TextKey("vocabulary_capture.pick_assistant_version")
    val DetailToggleHide = TextKey("vocabulary_capture.detail_toggle_hide")
    val DetailToggleShow = TextKey("vocabulary_capture.detail_toggle_show")
    val DetailPrepositions = TextKey("vocabulary_capture.detail_prepositions")
    val DetailComplementation = TextKey("vocabulary_capture.detail_complementation")
    val DetailGrammar = TextKey("vocabulary_capture.detail_grammar")
    val DetailUsage = TextKey("vocabulary_capture.detail_usage")
    val DetailForms = TextKey("vocabulary_capture.detail_forms")
    val DetailNote = TextKey("vocabulary_capture.detail_note")
    val GrammarLabelsLoading = TextKey("vocabulary_capture.grammar_labels.loading")
    val GrammarLabelsError = TextKey("vocabulary_capture.grammar_labels.error")
    val GrammarLabelsRetry = TextKey("vocabulary_capture.grammar_labels.retry")
}

internal val DefaultVocabularyCaptureTextProvider =
    MapTextProvider(
        mapOf(
            VocabularyCaptureTextKeys.Title to "Добавить слово",
            VocabularyCaptureTextKeys.Intro to
                "Введите слово или фразу. Ассистент предложит значения — выберите нужные.",
            VocabularyCaptureTextKeys.Saved to "«{0}» добавлено в ваш словарь.",
            VocabularyCaptureTextKeys.CaptureAnother to "Добавить ещё",
            VocabularyCaptureTextKeys.TermLabel to "Слово или фраза",
            VocabularyCaptureTextKeys.TermPlaceholder to "напр. come across",
            VocabularyCaptureTextKeys.Working to "Загрузка…",
            VocabularyCaptureTextKeys.GetSuggestions to "Получить варианты",
            VocabularyCaptureTextKeys.SuggestError to
                "Не удалось получить варианты — добавьте значение вручную.",
            VocabularyCaptureTextKeys.AiUnavailableNote to
                "Ассистент не настроен или недоступен — укажите ключ в настройках или добавьте значение вручную.",
            VocabularyCaptureTextKeys.AiDegradedNote to "Частичный ответ ассистента: {0}",
            VocabularyCaptureTextKeys.MissedSense to "Не хватает значения?",
            VocabularyCaptureTextKeys.ManualMeaningLabel to "Значение (своими словами)",
            VocabularyCaptureTextKeys.ManualMeaningPlaceholder to "значение, которого не хватило",
            VocabularyCaptureTextKeys.SurfaceFormLabel to "Форма (необязательно)",
            VocabularyCaptureTextKeys.SurfaceFormPlaceholder to "напр. come across [as]",
            VocabularyCaptureTextKeys.PartOfSpeechLabel to "Часть речи (необязательно)",
            VocabularyCaptureTextKeys.PartOfSpeechUnset to "Не задано",
            VocabularyCaptureTextKeys.AddOwnVariant to "Добавить свой вариант",
            VocabularyCaptureTextKeys.AddSelected to "Добавить выбранные",
            VocabularyCaptureTextKeys.YourSense to "Ваше значение",
            VocabularyCaptureTextKeys.CompleteFailed to
                "Не удалось дополнить — ваше значение всё равно добавлено.",
            VocabularyCaptureTextKeys.CompleteWithAssistant to "Дополнить с ассистентом",
            VocabularyCaptureTextKeys.Completing to "Дополнение…",
            VocabularyCaptureTextKeys.PickAssistantVersion to
                "Выберите версию ассистента (заменит вашу):",
            VocabularyCaptureTextKeys.DetailToggleHide to "Свернуть",
            VocabularyCaptureTextKeys.DetailToggleShow to "Подробнее",
            VocabularyCaptureTextKeys.DetailPrepositions to "Предлоги",
            VocabularyCaptureTextKeys.DetailComplementation to "Дополнение",
            VocabularyCaptureTextKeys.DetailGrammar to "Грамматика",
            VocabularyCaptureTextKeys.DetailUsage to "Употребление",
            VocabularyCaptureTextKeys.DetailForms to "Формы",
            VocabularyCaptureTextKeys.DetailNote to "Примечание",
            VocabularyCaptureTextKeys.GrammarLabelsLoading to "Загрузка обозначений…",
            VocabularyCaptureTextKeys.GrammarLabelsError to "Обозначения не загружены — карточка показывает сырые id",
            VocabularyCaptureTextKeys.GrammarLabelsRetry to "Повторить",
        ),
    )

internal fun TextProvider.statusNoteText(note: CaptureStatusNote): String =
    when (note) {
        CaptureStatusNote.AiUnavailable -> text(VocabularyCaptureTextKeys.AiUnavailableNote)
        is CaptureStatusNote.AiDegraded -> text(VocabularyCaptureTextKeys.AiDegradedNote, note.reason)
    }
