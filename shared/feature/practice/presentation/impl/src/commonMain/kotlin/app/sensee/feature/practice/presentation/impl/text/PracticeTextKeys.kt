package app.sensee.feature.practice.presentation.impl.text

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.QuantityTextKey
import app.sensee.core.presentation.text.QuantityTextTemplates
import app.sensee.core.presentation.text.TextKey

internal object PracticeTextKeys {
    val LoadingDeck = TextKey("practice.deck.loading")
    val DeckOpenError = TextKey("practice.deck.open_error")
    val DeckFinished = TextKey("practice.deck.finished")
    val RevealButton = TextKey("practice.deck.reveal_button")
    val RatingAgain = TextKey("practice.rating.again")
    val RatingHard = TextKey("practice.rating.hard")
    val RatingGood = TextKey("practice.rating.good")
    val RatingEasy = TextKey("practice.rating.easy")
    val HomeLoading = TextKey("practice.home.loading")
    val HomeLoadError = TextKey("practice.home.load_error")
    val HomeTitle = TextKey("practice.home.title")
    val HomeSubtitle = TextKey("practice.home.subtitle")
    val HomeEmpty = TextKey("practice.home.empty")
    val HomeCardCount = QuantityTextKey("practice.home.card_count")
    val CardDetailLoading = TextKey("practice.card_detail.loading")
    val CardDetailOpenError = TextKey("practice.card_detail.open_error")
    val CardDetailNoData = TextKey("practice.card_detail.no_data")
    val CardDetailRelatedTitle = TextKey("practice.card_detail.related_title")
    val CardDetailSectionSense = TextKey("practice.card_detail.section_sense")
    val CardDetailSectionContext = TextKey("practice.card_detail.section_context")
    val CardDetailCurrentlyOpen = TextKey("practice.card_detail.currently_open")
    val CardDetailSheetTitle = TextKey("practice.card_detail.sheet_title")
    val HelpSheetTitle = TextKey("practice.help.sheet_title")
    val HelpSectionUnitTypes = TextKey("practice.help.section.unit_types")
    val HelpSectionGrammarForms = TextKey("practice.help.section.grammar_forms")
    val ActionClose = TextKey("practice.action.close")
    val ActionHelp = TextKey("practice.action.help")
    val ActionDetails = TextKey("practice.action.details")
    val GrammarLabelsLoading = TextKey("practice.grammar_labels.loading")
    val GrammarLabelsError = TextKey("practice.grammar_labels.error")
    val GrammarLabelsRetry = TextKey("practice.grammar_labels.retry")
}

internal val DefaultPracticeTextProvider =
    MapTextProvider(
        mapOf(
            PracticeTextKeys.LoadingDeck to "Загрузка колоды...",
            PracticeTextKeys.DeckOpenError to "Не удалось открыть колоду",
            PracticeTextKeys.DeckFinished to "Колода пройдена",
            PracticeTextKeys.RevealButton to "Перевернуть",
            PracticeTextKeys.RatingAgain to "Снова",
            PracticeTextKeys.RatingHard to "Трудно",
            PracticeTextKeys.RatingGood to "Хорошо",
            PracticeTextKeys.RatingEasy to "Легко",
            PracticeTextKeys.HomeLoading to "Загрузка колод...",
            PracticeTextKeys.HomeLoadError to "Не удалось загрузить колоды",
            PracticeTextKeys.HomeTitle to "Колоды карточек",
            PracticeTextKeys.HomeSubtitle to "Выберите колоду, чтобы начать практику.",
            PracticeTextKeys.HomeEmpty to "Пока нет колод.",
            PracticeTextKeys.CardDetailLoading to "Загружаем карточку...",
            PracticeTextKeys.CardDetailOpenError to "Не удалось открыть карточку",
            PracticeTextKeys.CardDetailNoData to "Нет данных карточки",
            PracticeTextKeys.CardDetailRelatedTitle to "Связанные карточки по лемме «{0}»",
            PracticeTextKeys.CardDetailSectionSense to "Смысл",
            PracticeTextKeys.CardDetailSectionContext to "В контексте",
            PracticeTextKeys.CardDetailCurrentlyOpen to "Открыта сейчас",
            PracticeTextKeys.CardDetailSheetTitle to "Карточка",
            PracticeTextKeys.HelpSheetTitle to "Расшифровка бейджей",
            PracticeTextKeys.HelpSectionUnitTypes to "Часть речи",
            PracticeTextKeys.HelpSectionGrammarForms to "Грамматические признаки",
            PracticeTextKeys.ActionClose to "Закрыть",
            PracticeTextKeys.ActionHelp to "Справка",
            PracticeTextKeys.ActionDetails to "Детали карточки",
            PracticeTextKeys.GrammarLabelsLoading to "Загрузка обозначений…",
            PracticeTextKeys.GrammarLabelsError to "Обозначения не загружены — карточка показывает сырые id",
            PracticeTextKeys.GrammarLabelsRetry to "Повторить",
        ),
        quantities =
            mapOf(
                PracticeTextKeys.HomeCardCount to
                    QuantityTextTemplates(
                        one = "{0} карта",
                        few = "{0} карты",
                        many = "{0} карт",
                    ),
            ),
    )
