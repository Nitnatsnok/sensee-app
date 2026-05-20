package app.sensee.feature.practice.presentation.impl.text

import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.QuantityTextKey
import app.sensee.core.presentation.text.QuantityTextTemplates
import app.sensee.core.presentation.text.TextKey
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarUnitType

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

    /** Short dictionary-style abbreviation shown on a badge. English. */
    fun unitType(unitType: GrammarUnitType) = TextKey("practice.unit_type.${unitType.name}")

    /** Long human-readable description shown in the help glossary. Localised. */
    fun unitTypeDescription(unitType: GrammarUnitType) = TextKey("practice.unit_type.${unitType.name}.description")

    fun grammarForm(form: GrammarForm) = TextKey("practice.grammar_form.${form.name}")

    fun grammarFormDescription(form: GrammarForm) = TextKey("practice.grammar_form.${form.name}.description")
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
            PracticeTextKeys.unitType(GrammarUnitType.Noun) to "n.",
            PracticeTextKeys.unitType(GrammarUnitType.Verb) to "v.",
            PracticeTextKeys.unitType(GrammarUnitType.IrregularVerb) to "v. irr.",
            PracticeTextKeys.unitType(GrammarUnitType.PhrasalVerb) to "phr. v.",
            PracticeTextKeys.unitType(GrammarUnitType.Adjective) to "adj.",
            PracticeTextKeys.unitType(GrammarUnitType.Adverb) to "adv.",
            PracticeTextKeys.unitType(GrammarUnitType.Preposition) to "prep.",
            PracticeTextKeys.unitType(GrammarUnitType.Conjunction) to "conj.",
            PracticeTextKeys.unitType(GrammarUnitType.Pronoun) to "pron.",
            PracticeTextKeys.unitType(GrammarUnitType.Determiner) to "det.",
            PracticeTextKeys.unitType(GrammarUnitType.Numeral) to "num.",
            PracticeTextKeys.unitType(GrammarUnitType.Article) to "art.",
            PracticeTextKeys.unitType(GrammarUnitType.Idiom) to "idiom",
            PracticeTextKeys.unitType(GrammarUnitType.Phrase) to "phr.",
            PracticeTextKeys.unitType(GrammarUnitType.Interjection) to "interj.",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Noun) to "существительное",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Verb) to "глагол",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.IrregularVerb) to "неправильный глагол",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.PhrasalVerb) to "фразовый глагол",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Adjective) to "прилагательное",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Adverb) to "наречие",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Preposition) to "предлог",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Conjunction) to "союз",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Pronoun) to "местоимение",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Determiner) to "детерминатив",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Numeral) to "числительное",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Article) to "артикль",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Idiom) to "идиома",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Phrase) to "устойчивая фраза",
            PracticeTextKeys.unitTypeDescription(GrammarUnitType.Interjection) to "междометие",
            PracticeTextKeys.grammarForm(GrammarForm.Singular) to "sg.",
            PracticeTextKeys.grammarForm(GrammarForm.Plural) to "pl.",
            PracticeTextKeys.grammarForm(GrammarForm.CommonCase) to "comm.",
            PracticeTextKeys.grammarForm(GrammarForm.NounPossessiveCase) to "poss.",
            PracticeTextKeys.grammarForm(GrammarForm.Countable) to "count.",
            PracticeTextKeys.grammarForm(GrammarForm.Uncountable) to "uncount.",
            PracticeTextKeys.grammarForm(GrammarForm.Infinitive) to "V1",
            PracticeTextKeys.grammarForm(GrammarForm.PastTense) to "V2",
            PracticeTextKeys.grammarForm(GrammarForm.PastParticiple) to "V3",
            PracticeTextKeys.grammarForm(GrammarForm.PresentParticiple) to "V-ing",
            PracticeTextKeys.grammarForm(GrammarForm.Gerund) to "ger.",
            PracticeTextKeys.grammarForm(GrammarForm.Transitive) to "vt.",
            PracticeTextKeys.grammarForm(GrammarForm.Intransitive) to "vi.",
            PracticeTextKeys.grammarForm(GrammarForm.Separable) to "sep.",
            PracticeTextKeys.grammarForm(GrammarForm.Inseparable) to "insep.",
            PracticeTextKeys.grammarForm(GrammarForm.Positive) to "pos.",
            PracticeTextKeys.grammarForm(GrammarForm.Comparative) to "comp.",
            PracticeTextKeys.grammarForm(GrammarForm.Superlative) to "sup.",
            PracticeTextKeys.grammarForm(GrammarForm.Subjective) to "subj.",
            PracticeTextKeys.grammarForm(GrammarForm.Objective) to "obj.",
            PracticeTextKeys.grammarForm(GrammarForm.PronounPossessive) to "poss.",
            PracticeTextKeys.grammarForm(GrammarForm.Reflexive) to "refl.",
            PracticeTextKeys.grammarForm(GrammarForm.Demonstrative) to "dem.",
            PracticeTextKeys.grammarForm(GrammarForm.Quantifier) to "quant.",
            PracticeTextKeys.grammarForm(GrammarForm.Cardinal) to "card.",
            PracticeTextKeys.grammarForm(GrammarForm.Ordinal) to "ord.",
            PracticeTextKeys.grammarForm(GrammarForm.Definite) to "def.",
            PracticeTextKeys.grammarForm(GrammarForm.Indefinite) to "indef.",
            PracticeTextKeys.grammarForm(GrammarForm.Invariant) to "inv.",
            PracticeTextKeys.grammarForm(GrammarForm.Fixed) to "fixed",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Singular) to "единственное число",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Plural) to "множественное число",
            PracticeTextKeys.grammarFormDescription(GrammarForm.CommonCase) to "общий падеж",
            PracticeTextKeys.grammarFormDescription(GrammarForm.NounPossessiveCase) to "притяжательный падеж",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Countable) to "исчисляемое",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Uncountable) to "неисчисляемое",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Infinitive) to "инфинитив / 1-я форма",
            PracticeTextKeys.grammarFormDescription(GrammarForm.PastTense) to "прошедшее время / 2-я форма",
            PracticeTextKeys.grammarFormDescription(GrammarForm.PastParticiple) to "причастие прошедшего / 3-я форма",
            PracticeTextKeys.grammarFormDescription(GrammarForm.PresentParticiple) to "причастие настоящего (-ing)",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Gerund) to "герундий",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Transitive) to "переходный глагол",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Intransitive) to "непереходный глагол",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Separable) to "разделяемый фразовый глагол",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Inseparable) to "неразделяемый фразовый глагол",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Positive) to "положительная степень",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Comparative) to "сравнительная степень",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Superlative) to "превосходная степень",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Subjective) to "именительный падеж местоимения",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Objective) to "косвенный падеж местоимения",
            PracticeTextKeys.grammarFormDescription(GrammarForm.PronounPossessive) to "притяжательное местоимение",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Reflexive) to "возвратное местоимение",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Demonstrative) to "указательное местоимение",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Quantifier) to "квантификатор",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Cardinal) to "количественное числительное",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Ordinal) to "порядковое числительное",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Definite) to "определённый",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Indefinite) to "неопределённый",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Invariant) to "не изменяется по форме",
            PracticeTextKeys.grammarFormDescription(GrammarForm.Fixed) to "фиксированная форма",
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
