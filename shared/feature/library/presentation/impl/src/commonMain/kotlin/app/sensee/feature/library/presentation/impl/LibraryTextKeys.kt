package app.sensee.feature.library.presentation.impl

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.QuantityTextKey
import app.sensee.core.presentation.text.QuantityTextTemplates
import app.sensee.core.presentation.text.TextKey

internal object LibraryTextKeys {
    val HomeLoading = TextKey("library.home.loading")
    val HomeLoadError = TextKey("library.home.load_error")
    val Title = TextKey("library.home.title")
    val Subtitle = TextKey("library.home.subtitle")
    val SuggestedSection = TextKey("library.home.section.suggested")
    val OwnedSection = TextKey("library.home.section.owned")
    val SuggestedEmpty = TextKey("library.home.suggested_empty")
    val OwnedEmpty = TextKey("library.home.owned_empty")
    val CardCount = QuantityTextKey("library.home.card_count")
    val Adopt = TextKey("library.home.adopt")
    val UnAdopt = TextKey("library.home.unadopt")
    val DeckBack = TextKey("library.deck.back")
    val DeckLoading = TextKey("library.deck.loading")
    val DeckLoadError = TextKey("library.deck.load_error")
    val DeckEmpty = TextKey("library.deck.empty")
    val DeckAdopt = TextKey("library.deck.adopt")
    val DeckAdoptError = TextKey("library.deck.adopt_error")
    val DeckClose = TextKey("library.deck.close")
}

internal val DefaultLibraryTextProvider =
    MapTextProvider(
        mapOf(
            LibraryTextKeys.HomeLoading to "Загрузка библиотеки...",
            LibraryTextKeys.HomeLoadError to "Не удалось загрузить библиотеку",
            LibraryTextKeys.Title to "Библиотека",
            LibraryTextKeys.Subtitle to "Добавляйте наборы в свой материал, чтобы практиковать их.",
            LibraryTextKeys.SuggestedSection to "Сервисные наборы",
            LibraryTextKeys.OwnedSection to "Мой материал",
            LibraryTextKeys.SuggestedEmpty to "Все наборы уже добавлены.",
            LibraryTextKeys.OwnedEmpty to "Пока нет своих наборов.",
            LibraryTextKeys.Adopt to "Добавить",
            LibraryTextKeys.UnAdopt to "Убрать",
            LibraryTextKeys.DeckBack to "Назад",
            LibraryTextKeys.DeckLoading to "Загрузка набора...",
            LibraryTextKeys.DeckLoadError to "Не удалось загрузить набор",
            LibraryTextKeys.DeckEmpty to "В этом наборе пока нет карточек.",
            LibraryTextKeys.DeckAdopt to "Добавить набор",
            LibraryTextKeys.DeckAdoptError to "Не удалось добавить набор",
            LibraryTextKeys.DeckClose to "Закрыть набор",
        ),
        quantities =
            mapOf(
                LibraryTextKeys.CardCount to
                    QuantityTextTemplates(
                        one = "{0} карта",
                        few = "{0} карты",
                        many = "{0} карт",
                    ),
            ),
    )
