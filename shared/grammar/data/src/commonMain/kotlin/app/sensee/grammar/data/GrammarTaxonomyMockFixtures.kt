package app.sensee.grammar.data

import app.sensee.core.mockBackend.MockFixtureSet
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * The single source of the `practice/grammar/taxonomy` fixture. Labels are
 * learner-facing Russian (the native language); ids stay the neutral
 * wire/storage contract. No other module may contribute this key.
 */
@ContributesIntoSet(AppScope::class)
@Inject
public class GrammarTaxonomyMockFixtures : MockFixtureSet {
    override val fixtures: Map<String, String> =
        mapOf("practice/grammar/taxonomy" to GRAMMAR_TAXONOMY_FIXTURE)
}

private const val GRAMMAR_TAXONOMY_FIXTURE = """
{
  "unit_types": [
    {
      "id": "noun",
      "label": "Существительное",
      "abbreviation": "n.",
      "categories": [
        {
          "id": "number",
          "label": "Число",
          "forms": [
            { "id": "singular", "label": "Единственное", "abbreviations": ["sing.", "sg."], "examples": ["cat"] },
            { "id": "plural", "label": "Множественное", "abbreviations": ["pl."], "examples": ["cats"] }
          ]
        },
        {
          "id": "case",
          "label": "Падеж",
          "forms": [
            { "id": "common_case", "label": "Общий", "abbreviations": [], "examples": ["cat"] },
            { "id": "possessive_case", "label": "Притяжательный", "abbreviations": ["poss."], "examples": ["cat's"] }
          ]
        },
        {
          "id": "countability",
          "label": "Исчисляемость",
          "forms": [
            { "id": "countable", "label": "Исчисляемое", "abbreviations": ["C"], "examples": ["apple"] },
            { "id": "uncountable", "label": "Неисчисляемое", "abbreviations": ["U"], "examples": ["water"] }
          ]
        }
      ]
    },
    {
      "id": "verb",
      "label": "Глагол",
      "abbreviation": "v.",
      "categories": [
        {
          "id": "verb",
          "label": "Формы",
          "forms": [
            { "id": "infinitive", "label": "Инфинитив", "abbreviations": ["inf."], "examples": ["play"] },
            { "id": "past_tense", "label": "Прошедшее время", "abbreviations": ["p.t.", "past"], "examples": ["played"] },
            { "id": "past_participle", "label": "Причастие прош. времени", "abbreviations": ["p.p."], "examples": ["played"] },
            { "id": "present_participle", "label": "Причастие наст. времени", "abbreviations": ["pres.p."], "examples": ["playing"] },
            { "id": "gerund", "label": "Герундий", "abbreviations": ["ger."], "examples": ["playing"] }
          ]
        },
        {
          "id": "transitivity",
          "label": "Переходность",
          "forms": [
            { "id": "transitive", "label": "Переходный", "abbreviations": ["v.t.", "vt"], "examples": ["read a book"] },
            { "id": "intransitive", "label": "Непереходный", "abbreviations": ["v.i.", "vi"], "examples": ["sleep"] }
          ]
        }
      ]
    },
    {
      "id": "irregular_verb",
      "label": "Неправильный глагол",
      "abbreviation": "irreg.v.",
      "categories": [
        {
          "id": "verb_irregular",
          "label": "Формы",
          "forms": [
            { "id": "infinitive", "label": "Инфинитив", "abbreviations": ["inf."], "examples": ["go"] },
            { "id": "past_tense", "label": "Прошедшее время", "abbreviations": ["p.t."], "examples": ["went"] },
            { "id": "past_participle", "label": "Причастие прош. времени", "abbreviations": ["p.p."], "examples": ["gone"] }
          ]
        }
      ]
    },
    {
      "id": "phrasal_verb",
      "label": "Фразовый глагол",
      "abbreviation": "phr.v.",
      "categories": [
        {
          "id": "verb",
          "label": "Формы",
          "forms": [
            { "id": "infinitive", "label": "Инфинитив", "abbreviations": ["inf."], "examples": ["give up"] },
            { "id": "past_tense", "label": "Прошедшее время", "abbreviations": ["p.t."], "examples": ["gave up"] },
            { "id": "past_participle", "label": "Причастие прош. времени", "abbreviations": ["p.p."], "examples": ["given up"] }
          ]
        },
        {
          "id": "separability",
          "label": "Разделяемость",
          "forms": [
            { "id": "separable", "label": "Разделяемый", "abbreviations": ["sep."], "examples": ["turn it on"] },
            { "id": "inseparable", "label": "Неразделяемый", "abbreviations": ["insep."], "examples": ["look after him"] }
          ]
        }
      ]
    },
    {
      "id": "adjective",
      "label": "Прилагательное",
      "abbreviation": "adj.",
      "categories": [
        {
          "id": "degree",
          "label": "Степени сравнения",
          "forms": [
            { "id": "positive", "label": "Положительная", "abbreviations": ["pos."], "examples": ["big"] },
            { "id": "comparative", "label": "Сравнительная", "abbreviations": ["comp."], "examples": ["bigger"] },
            { "id": "superlative", "label": "Превосходная", "abbreviations": ["sup."], "examples": ["biggest"] }
          ]
        }
      ]
    },
    {
      "id": "adverb",
      "label": "Наречие",
      "abbreviation": "adv.",
      "categories": [
        {
          "id": "degree",
          "label": "Степени сравнения",
          "forms": [
            { "id": "positive", "label": "Положительная", "abbreviations": ["pos."], "examples": ["fast"] },
            { "id": "comparative", "label": "Сравнительная", "abbreviations": ["comp."], "examples": ["faster"] },
            { "id": "superlative", "label": "Превосходная", "abbreviations": ["sup."], "examples": ["fastest"] }
          ]
        }
      ]
    },
    {
      "id": "pronoun",
      "label": "Местоимение",
      "abbreviation": "pron.",
      "categories": [
        {
          "id": "pronoun_type",
          "label": "Тип",
          "forms": [
            { "id": "subjective", "label": "Именительный", "abbreviations": ["subj."], "examples": ["I", "he", "they"] },
            { "id": "objective", "label": "Объектный", "abbreviations": ["obj."], "examples": ["me", "him", "them"] },
            { "id": "possessive_pronoun", "label": "Притяжательное", "abbreviations": ["poss."], "examples": ["my", "his", "their"] },
            { "id": "reflexive", "label": "Возвратное", "abbreviations": ["refl."], "examples": ["myself", "himself"] }
          ]
        }
      ]
    },
    {
      "id": "determiner",
      "label": "Определитель",
      "abbreviation": "det.",
      "categories": [
        {
          "id": "determiner_type",
          "label": "Тип",
          "forms": [
            { "id": "demonstrative", "label": "Указательный", "abbreviations": ["dem."], "examples": ["this", "these"] },
            { "id": "quantifier", "label": "Квантификатор", "abbreviations": ["quant."], "examples": ["some", "many", "all"] }
          ]
        }
      ]
    },
    {
      "id": "numeral",
      "label": "Числительное",
      "abbreviation": "num.",
      "categories": [
        {
          "id": "numeral_type",
          "label": "Тип",
          "forms": [
            { "id": "cardinal", "label": "Количественное", "abbreviations": ["card."], "examples": ["two", "ten"] },
            { "id": "ordinal", "label": "Порядковое", "abbreviations": ["ord."], "examples": ["second", "tenth"] }
          ]
        }
      ]
    },
    {
      "id": "article",
      "label": "Артикль",
      "abbreviation": "art.",
      "categories": [
        {
          "id": "definiteness",
          "label": "Определённость",
          "forms": [
            { "id": "definite", "label": "Определённый", "abbreviations": ["def."], "examples": ["the"] },
            { "id": "indefinite", "label": "Неопределённый", "abbreviations": ["indef."], "examples": ["a", "an"] }
          ]
        }
      ]
    },
    {
      "id": "preposition",
      "label": "Предлог",
      "abbreviation": "prep.",
      "categories": [
        {
          "id": "invariance",
          "label": "Неизменяемость",
          "forms": [
            { "id": "invariant", "label": "Неизменяемая", "abbreviations": ["inv."], "examples": ["in", "on", "at", "by"] }
          ]
        }
      ]
    },
    {
      "id": "conjunction",
      "label": "Союз",
      "abbreviation": "conj.",
      "categories": [
        {
          "id": "invariance",
          "label": "Неизменяемость",
          "forms": [
            { "id": "invariant", "label": "Неизменяемая", "abbreviations": ["inv."], "examples": ["and", "but", "or"] }
          ]
        }
      ]
    },
    {
      "id": "interjection",
      "label": "Междометие",
      "abbreviation": "int.",
      "categories": [
        {
          "id": "invariance",
          "label": "Неизменяемость",
          "forms": [
            { "id": "invariant", "label": "Неизменяемая", "abbreviations": ["inv."], "examples": ["oh!", "wow!", "ouch!"] }
          ]
        }
      ]
    },
    {
      "id": "idiom",
      "label": "Идиома",
      "abbreviation": "id.",
      "categories": [
        {
          "id": "expression_type",
          "label": "Тип выражения",
          "forms": [
            { "id": "fixed", "label": "Устойчивое выражение", "abbreviations": ["fixed"], "examples": ["piece of cake", "break a leg"] }
          ]
        }
      ]
    },
    {
      "id": "phrase",
      "label": "Фраза",
      "abbreviation": "phr.",
      "categories": [
        {
          "id": "expression_type",
          "label": "Тип выражения",
          "forms": [
            { "id": "fixed", "label": "Устойчивое выражение", "abbreviations": ["fixed"], "examples": ["by the way", "in order to"] }
          ]
        }
      ]
    }
  ],
  "usage_axes": [
    {
      "id": "register",
      "label": "Регистр",
      "values": [
        { "id": "formal", "label": "Формальный" },
        { "id": "informal", "label": "Неформальный" },
        { "id": "slang", "label": "Сленг" },
        { "id": "literary", "label": "Литературный" },
        { "id": "neutral", "label": "Нейтральный" }
      ]
    },
    {
      "id": "region",
      "label": "Регион",
      "values": [
        { "id": "bre", "label": "Британский" },
        { "id": "ame", "label": "Американский" },
        { "id": "ause", "label": "Австралийский" },
        { "id": "cane", "label": "Канадский" }
      ]
    },
    {
      "id": "domain",
      "label": "Область",
      "values": [
        { "id": "law", "label": "Юридическая" },
        { "id": "medicine", "label": "Медицина" },
        { "id": "it", "label": "IT" },
        { "id": "science", "label": "Наука" },
        { "id": "business", "label": "Бизнес" }
      ]
    },
    {
      "id": "connotation",
      "label": "Коннотация",
      "values": [
        { "id": "neutral_connotation", "label": "Нейтральная" },
        { "id": "positive_connotation", "label": "Положительная" },
        { "id": "pejorative", "label": "Пейоративная" },
        { "id": "euphemistic", "label": "Эвфемизм" }
      ]
    },
    {
      "id": "temporality",
      "label": "Актуальность",
      "values": [
        { "id": "current", "label": "Современное" },
        { "id": "dated", "label": "Устаревающее" },
        { "id": "archaic", "label": "Архаичное" },
        { "id": "obsolete", "label": "Вышедшее из употребления" }
      ]
    }
  ],
  "complement_types": [
    { "id": "noun", "label": "Существительное" },
    { "id": "gerund", "label": "Герундий (-ing)" },
    { "id": "to_infinitive", "label": "Инфинитив с to" },
    { "id": "bare_infinitive", "label": "Инфинитив без to" },
    { "id": "that_clause", "label": "Придаточное с that" },
    { "id": "wh_clause", "label": "Придаточное с wh-" },
    { "id": "adjective", "label": "Прилагательное" },
    { "id": "prepositional_phrase", "label": "Предложная группа" },
    { "id": "intransitive", "label": "Без дополнения" }
  ]
}
"""
