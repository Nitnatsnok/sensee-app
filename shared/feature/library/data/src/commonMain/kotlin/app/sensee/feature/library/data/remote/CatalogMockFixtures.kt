package app.sensee.feature.library.data.remote

import app.sensee.core.mockBackend.MockFixtureSet
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Library's Service suggestion catalog (mock backend). Three themed decks of ten
 * cards each. Four cards are deliberately shared across two decks to exercise
 * true many-to-many: a shared card is one [Practice_card][app.sensee.core.database.Practice_card]
 * row referenced by several decks. The shared-card JSON is defined once as a
 * constant and interpolated into both decks so the two copies stay byte-identical.
 */
@ContributesIntoSet(AppScope::class)
@Inject
public class CatalogMockFixtures : MockFixtureSet {
    override val fixtures: Map<String, String> =
        mapOf(
            "practice/decks" to DECK_LIST_FIXTURE,
            "practice/decks/phrasal-verbs-come" to PHRASAL_VERBS_COME_DECK_FIXTURE,
            "practice/decks/architecture-basics" to ARCHITECTURE_DECK_FIXTURE,
            "practice/decks/daily-essentials" to DAILY_ESSENTIALS_DECK_FIXTURE,
            "practice/lemmas/lemma-come" to LEMMA_COME_FIXTURE,
            "practice/lemmas/lemma-resilient" to LEMMA_RESILIENT_FIXTURE,
            "practice/lemmas/lemma-eventually" to LEMMA_EVENTUALLY_FIXTURE,
            "practice/lemmas/lemma-trade-off" to LEMMA_TRADE_OFF_FIXTURE,
            "practice/lemmas/lemma-cohesion" to LEMMA_COHESION_FIXTURE,
            "practice/lemmas/lemma-boundary" to LEMMA_BOUNDARY_FIXTURE,
            "practice/lemmas/lemma-coupling" to LEMMA_COUPLING_FIXTURE,
            "practice/lemmas/lemma-scalable" to LEMMA_SCALABLE_FIXTURE,
            "practice/lemmas/lemma-latency" to LEMMA_LATENCY_FIXTURE,
            "practice/lemmas/lemma-throughput" to LEMMA_THROUGHPUT_FIXTURE,
            "practice/lemmas/lemma-bottleneck" to LEMMA_BOTTLENECK_FIXTURE,
            "practice/lemmas/lemma-reliable" to LEMMA_RELIABLE_FIXTURE,
            "practice/lemmas/lemma-schedule" to LEMMA_SCHEDULE_FIXTURE,
            "practice/lemmas/lemma-meeting" to LEMMA_MEETING_FIXTURE,
            "practice/lemmas/lemma-deadline" to LEMMA_DEADLINE_FIXTURE,
            "practice/lemmas/lemma-priority" to LEMMA_PRIORITY_FIXTURE,
            "practice/lemmas/lemma-feedback" to LEMMA_FEEDBACK_FIXTURE,
        )
}

private const val CARD_COME_UP = """
    {
      "id": "card-come-up",
      "lemma_id": "lemma-come",
      "headword": "come up",
      "translation": "возникать, неожиданно появиться",
      "context_sentence": "Something has come up at work, so I'll be late tonight.",
      "unit_type": "PhrasalVerb",
      "grammar_tags": [
        { "category": "verb", "form": "infinitive" },
        { "category": "transitivity", "form": "intransitive" },
        { "category": "separability", "form": "inseparable" }
      ],
      "sense_summary": "о теме, проблеме, событии — внезапно появиться",
      "explanation": "Используется про события и темы, которые «всплыли» сами по себе."
    }
"""

private const val CARD_COME_ACROSS = """
    {
      "id": "card-come-across",
      "lemma_id": "lemma-come",
      "headword": "come across",
      "translation": "случайно встретить, наткнуться",
      "context_sentence": "I often come across old letters from my grandmother.",
      "unit_type": "PhrasalVerb",
      "grammar_tags": [
        { "category": "verb", "form": "infinitive" },
        { "category": "transitivity", "form": "transitive" },
        { "category": "separability", "form": "inseparable" }
      ],
      "sense_summary": "найти что-то или встретить кого-то случайно",
      "explanation": "Подчеркивает случайность находки или встречи."
    }
"""

private const val CARD_RESILIENT = """
    {
      "id": "card-resilient",
      "lemma_id": "lemma-resilient",
      "headword": "resilient",
      "translation": "устойчивый, способный быстро восстановиться",
      "context_sentence": "A resilient system keeps working even when some parts fail.",
      "unit_type": "Adjective",
      "grammar_tags": [
        { "category": "degree", "form": "positive" }
      ],
      "sense_summary": "о системе — способна продолжать работу при отказах",
      "explanation": "Часто противопоставляется fragile: после ударов система возвращается к работе."
    }
"""

private const val CARD_EVENTUALLY = """
    {
      "id": "card-eventually",
      "lemma_id": "lemma-eventually",
      "headword": "eventually",
      "translation": "в конечном итоге",
      "context_sentence": "The cache will eventually become consistent.",
      "unit_type": "Adverb",
      "grammar_tags": [
        { "category": "degree", "form": "positive" }
      ],
      "sense_summary": "о наступлении результата спустя некоторое время",
      "explanation": "Не «потенциально», а «через какое-то время, но обязательно»."
    }
"""

private const val DECK_LIST_FIXTURE = """
{
  "decks": [
    {
      "id": "phrasal-verbs-come",
      "title": "Phrasal verbs: come",
      "description": "Семейство фразовых глаголов с базовой леммой come.",
      "card_count": 10
    },
    {
      "id": "architecture-basics",
      "title": "Architecture vocabulary",
      "description": "Базовая лексика для разговоров об архитектуре ПО.",
      "card_count": 10
    },
    {
      "id": "daily-essentials",
      "title": "Daily essentials",
      "description": "Карточки общего назначения, которые встречаются каждый день.",
      "card_count": 10
    }
  ]
}
"""

private val PHRASAL_VERBS_COME_DECK_FIXTURE = """
{
  "id": "phrasal-verbs-come",
  "title": "Phrasal verbs: come",
  "description": "Семейство фразовых глаголов с базовой леммой come.",
  "cards": [
    {
      "id": "card-come",
      "lemma_id": "lemma-come",
      "headword": "come",
      "translation": "приходить, прибывать",
      "context_sentence": "Please come to the meeting at ten.",
      "unit_type": "IrregularVerb",
      "grammar_tags": [
        { "category": "verb_irregular", "form": "infinitive" }
      ],
      "sense_summary": "движение к месту или к говорящему",
      "explanation": "Базовый глагол движения «к себе». Формы came/come — основа фразовых глаголов этой леммы."
    },
    $CARD_COME_UP,
    {
      "id": "card-come-up-with",
      "lemma_id": "lemma-come",
      "headword": "come up with",
      "translation": "придумать, предложить",
      "context_sentence": "Can you come up with a better name for the project?",
      "unit_type": "PhrasalVerb",
      "grammar_tags": [
        { "category": "verb", "form": "infinitive" },
        { "category": "transitivity", "form": "transitive" },
        { "category": "separability", "form": "inseparable" }
      ],
      "sense_summary": "сгенерировать идею, решение, ответ",
      "explanation": "После come up with — то, что было придумано: идея, имя, решение."
    },
    $CARD_COME_ACROSS,
    {
      "id": "card-come-around",
      "lemma_id": "lemma-come",
      "headword": "come around",
      "translation": "прийти в сознание, очнуться",
      "context_sentence": "She'll come around after a short rest.",
      "unit_type": "PhrasalVerb",
      "grammar_tags": [
        { "category": "verb", "form": "infinitive" },
        { "category": "transitivity", "form": "intransitive" },
        { "category": "separability", "form": "inseparable" }
      ],
      "sense_summary": "вернуться в сознание после обморока или сна",
      "explanation": "В этом значении часто после fainted/passed out."
    },
    {
      "id": "card-come-back",
      "lemma_id": "lemma-come",
      "headword": "come back",
      "translation": "вернуться",
      "context_sentence": "I'll come back as soon as the call is over.",
      "unit_type": "PhrasalVerb",
      "grammar_tags": [
        { "category": "verb", "form": "infinitive" },
        { "category": "transitivity", "form": "intransitive" },
        { "category": "separability", "form": "inseparable" }
      ],
      "sense_summary": "возвращение в прежнее место",
      "explanation": "Простое возвращение туда, откуда ушёл."
    },
    {
      "id": "card-come-out",
      "lemma_id": "lemma-come",
      "headword": "come out",
      "translation": "выходить, становиться известным",
      "context_sentence": "The new version comes out next week.",
      "unit_type": "PhrasalVerb",
      "grammar_tags": [
        { "category": "verb", "form": "infinitive" },
        { "category": "transitivity", "form": "intransitive" },
        { "category": "separability", "form": "inseparable" }
      ],
      "sense_summary": "о выпуске, публикации или раскрытии факта",
      "explanation": "Про релизы, новости и то, что «вышло наружу»."
    },
    {
      "id": "card-come-along",
      "lemma_id": "lemma-come",
      "headword": "come along",
      "translation": "продвигаться, идти вместе",
      "context_sentence": "The project is coming along nicely.",
      "unit_type": "PhrasalVerb",
      "grammar_tags": [
        { "category": "verb", "form": "infinitive" },
        { "category": "transitivity", "form": "intransitive" },
        { "category": "separability", "form": "inseparable" }
      ],
      "sense_summary": "о прогрессе или о том, чтобы присоединиться",
      "explanation": "Часто про то, как «движется» работа."
    },
    {
      "id": "card-come-down",
      "lemma_id": "lemma-come",
      "headword": "come down",
      "translation": "снижаться, падать",
      "context_sentence": "Prices have come down since last year.",
      "unit_type": "PhrasalVerb",
      "grammar_tags": [
        { "category": "verb", "form": "infinitive" },
        { "category": "transitivity", "form": "intransitive" },
        { "category": "separability", "form": "inseparable" }
      ],
      "sense_summary": "уменьшение величины или уровня",
      "explanation": "Про снижение цен, чисел, температуры."
    },
    {
      "id": "card-come-over",
      "lemma_id": "lemma-come",
      "headword": "come over",
      "translation": "заходить в гости",
      "context_sentence": "Come over this weekend and we'll talk it through.",
      "unit_type": "PhrasalVerb",
      "grammar_tags": [
        { "category": "verb", "form": "infinitive" },
        { "category": "transitivity", "form": "intransitive" },
        { "category": "separability", "form": "inseparable" }
      ],
      "sense_summary": "прийти к кому-то домой ненадолго",
      "explanation": "Неформальное приглашение зайти."
    }
  ]
}
"""

private val ARCHITECTURE_DECK_FIXTURE = """
{
  "id": "architecture-basics",
  "title": "Architecture vocabulary",
  "description": "Базовая лексика для разговоров об архитектуре ПО.",
  "cards": [
    $CARD_RESILIENT,
    {
      "id": "card-trade-off",
      "lemma_id": "lemma-trade-off",
      "headword": "trade-off",
      "translation": "компромисс между вариантами",
      "context_sentence": "Every architecture decision has a trade-off.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "number", "form": "singular" },
        { "category": "countability", "form": "countable" }
      ],
      "sense_summary": "баланс между альтернативами при выборе решения",
      "explanation": "Подразумевает, что выгоду в одном измерении мы оплачиваем в другом."
    },
    {
      "id": "card-cohesion",
      "lemma_id": "lemma-cohesion",
      "headword": "cohesion",
      "translation": "связность внутри модуля или компонента",
      "context_sentence": "High cohesion makes a module easier to understand.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "countability", "form": "uncountable" }
      ],
      "sense_summary": "степень логической связанности элементов модуля",
      "explanation": "Высокая cohesion = одна ответственность; низкая = «свалка»."
    },
    {
      "id": "card-boundary",
      "lemma_id": "lemma-boundary",
      "headword": "boundary",
      "translation": "граница ответственности",
      "context_sentence": "A clear boundary protects the domain model.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "number", "form": "singular" },
        { "category": "countability", "form": "countable" }
      ],
      "sense_summary": "разделение зон ответственности между частями системы",
      "explanation": "Архитектурная метафора: внутри boundary действуют свои правила, снаружи — другие."
    },
    $CARD_EVENTUALLY,
    {
      "id": "card-coupling",
      "lemma_id": "lemma-coupling",
      "headword": "coupling",
      "translation": "связанность между модулями",
      "context_sentence": "Tight coupling makes change expensive.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "countability", "form": "uncountable" }
      ],
      "sense_summary": "степень зависимости модулей друг от друга",
      "explanation": "Цель — loose coupling: модули знают друг о друге как можно меньше."
    },
    {
      "id": "card-scalable",
      "lemma_id": "lemma-scalable",
      "headword": "scalable",
      "translation": "масштабируемый",
      "context_sentence": "We need a scalable design before traffic grows.",
      "unit_type": "Adjective",
      "grammar_tags": [
        { "category": "degree", "form": "positive" }
      ],
      "sense_summary": "о системе — растёт под нагрузкой без переписывания",
      "explanation": "Способность выдержать рост нагрузки добавлением ресурсов."
    },
    {
      "id": "card-latency",
      "lemma_id": "lemma-latency",
      "headword": "latency",
      "translation": "задержка отклика",
      "context_sentence": "Caching reduces read latency.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "countability", "form": "uncountable" }
      ],
      "sense_summary": "время между запросом и ответом",
      "explanation": "Не путать с throughput: latency — про задержку одного запроса."
    },
    {
      "id": "card-throughput",
      "lemma_id": "lemma-throughput",
      "headword": "throughput",
      "translation": "пропускная способность",
      "context_sentence": "Batching improved throughput tenfold.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "countability", "form": "uncountable" }
      ],
      "sense_summary": "объём работы в единицу времени",
      "explanation": "Сколько запросов система обрабатывает за секунду."
    },
    {
      "id": "card-bottleneck",
      "lemma_id": "lemma-bottleneck",
      "headword": "bottleneck",
      "translation": "узкое место",
      "context_sentence": "The database is the current bottleneck.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "number", "form": "singular" },
        { "category": "countability", "form": "countable" }
      ],
      "sense_summary": "самый медленный участок, ограничивающий систему",
      "explanation": "Оптимизировать имеет смысл именно bottleneck, а не всё подряд."
    }
  ]
}
"""

private val DAILY_ESSENTIALS_DECK_FIXTURE = """
{
  "id": "daily-essentials",
  "title": "Daily essentials",
  "description": "Карточки общего назначения, которые встречаются каждый день.",
  "cards": [
    $CARD_COME_UP,
    $CARD_COME_ACROSS,
    $CARD_RESILIENT,
    $CARD_EVENTUALLY,
    {
      "id": "card-reliable",
      "lemma_id": "lemma-reliable",
      "headword": "reliable",
      "translation": "надёжный",
      "context_sentence": "She is a reliable teammate.",
      "unit_type": "Adjective",
      "grammar_tags": [
        { "category": "degree", "form": "positive" }
      ],
      "sense_summary": "о том, на что можно положиться",
      "explanation": "Делает то, что обещал, стабильно."
    },
    {
      "id": "card-schedule",
      "lemma_id": "lemma-schedule",
      "headword": "schedule",
      "translation": "расписание, график",
      "context_sentence": "The release is back on schedule.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "number", "form": "singular" },
        { "category": "countability", "form": "countable" }
      ],
      "sense_summary": "план дел во времени",
      "explanation": "on schedule = по плану; behind schedule = с опозданием."
    },
    {
      "id": "card-meeting",
      "lemma_id": "lemma-meeting",
      "headword": "meeting",
      "translation": "встреча, совещание",
      "context_sentence": "Let's move the meeting to the afternoon.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "number", "form": "singular" },
        { "category": "countability", "form": "countable" }
      ],
      "sense_summary": "запланированный сбор людей",
      "explanation": "Базовое слово рабочего календаря."
    },
    {
      "id": "card-deadline",
      "lemma_id": "lemma-deadline",
      "headword": "deadline",
      "translation": "крайний срок",
      "context_sentence": "We have a tight deadline on Friday.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "number", "form": "singular" },
        { "category": "countability", "form": "countable" }
      ],
      "sense_summary": "момент, к которому работа должна быть готова",
      "explanation": "miss a deadline = не успеть к сроку."
    },
    {
      "id": "card-priority",
      "lemma_id": "lemma-priority",
      "headword": "priority",
      "translation": "приоритет",
      "context_sentence": "Security is our top priority this quarter.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "number", "form": "singular" },
        { "category": "countability", "form": "countable" }
      ],
      "sense_summary": "то, что делается раньше остального",
      "explanation": "top priority = самое важное сейчас."
    },
    {
      "id": "card-feedback",
      "lemma_id": "lemma-feedback",
      "headword": "feedback",
      "translation": "обратная связь",
      "context_sentence": "Thanks for the quick feedback on the draft.",
      "unit_type": "Noun",
      "grammar_tags": [
        { "category": "countability", "form": "uncountable" }
      ],
      "sense_summary": "реакция, помогающая улучшить работу",
      "explanation": "Неисчисляемое: a piece of feedback, не a feedback."
    }
  ]
}
"""

private const val LEMMA_COME_FIXTURE = """
{
  "id": "lemma-come",
  "text": "come",
  "related_cards": [
    { "id": "card-come", "headword": "come", "unit_type": "IrregularVerb", "translation": "приходить, прибывать", "sense_summary": "движение к месту или к говорящему" },
    { "id": "card-come-up", "headword": "come up", "unit_type": "PhrasalVerb", "translation": "возникать, неожиданно появиться", "sense_summary": "о теме, проблеме, событии — внезапно появиться" },
    { "id": "card-come-up-with", "headword": "come up with", "unit_type": "PhrasalVerb", "translation": "придумать, предложить", "sense_summary": "сгенерировать идею, решение, ответ" },
    { "id": "card-come-across", "headword": "come across", "unit_type": "PhrasalVerb", "translation": "случайно встретить, наткнуться", "sense_summary": "найти что-то или встретить кого-то случайно" },
    { "id": "card-come-around", "headword": "come around", "unit_type": "PhrasalVerb", "translation": "прийти в сознание, очнуться", "sense_summary": "вернуться в сознание после обморока или сна" },
    { "id": "card-come-back", "headword": "come back", "unit_type": "PhrasalVerb", "translation": "вернуться", "sense_summary": "возвращение в прежнее место" },
    { "id": "card-come-out", "headword": "come out", "unit_type": "PhrasalVerb", "translation": "выходить, становиться известным", "sense_summary": "о выпуске, публикации или раскрытии факта" },
    { "id": "card-come-along", "headword": "come along", "unit_type": "PhrasalVerb", "translation": "продвигаться, идти вместе", "sense_summary": "о прогрессе или о том, чтобы присоединиться" },
    { "id": "card-come-down", "headword": "come down", "unit_type": "PhrasalVerb", "translation": "снижаться, падать", "sense_summary": "уменьшение величины или уровня" },
    { "id": "card-come-over", "headword": "come over", "unit_type": "PhrasalVerb", "translation": "заходить в гости", "sense_summary": "прийти к кому-то домой ненадолго" }
  ]
}
"""

private const val LEMMA_RESILIENT_FIXTURE = """
{
  "id": "lemma-resilient",
  "text": "resilient",
  "related_cards": [
    { "id": "card-resilient", "headword": "resilient", "unit_type": "Adjective", "translation": "устойчивый, способный быстро восстановиться", "sense_summary": "о системе — способна продолжать работу при отказах" }
  ]
}
"""

private const val LEMMA_EVENTUALLY_FIXTURE = """
{
  "id": "lemma-eventually",
  "text": "eventually",
  "related_cards": [
    { "id": "card-eventually", "headword": "eventually", "unit_type": "Adverb", "translation": "в конечном итоге", "sense_summary": "о наступлении результата спустя некоторое время" }
  ]
}
"""

private const val LEMMA_TRADE_OFF_FIXTURE = """
{
  "id": "lemma-trade-off",
  "text": "trade-off",
  "related_cards": [
    { "id": "card-trade-off", "headword": "trade-off", "unit_type": "Noun", "translation": "компромисс между вариантами", "sense_summary": "баланс между альтернативами" }
  ]
}
"""

private const val LEMMA_COHESION_FIXTURE = """
{
  "id": "lemma-cohesion",
  "text": "cohesion",
  "related_cards": [
    { "id": "card-cohesion", "headword": "cohesion", "unit_type": "Noun", "translation": "связность модуля", "sense_summary": "логическая связанность элементов модуля" }
  ]
}
"""

private const val LEMMA_BOUNDARY_FIXTURE = """
{
  "id": "lemma-boundary",
  "text": "boundary",
  "related_cards": [
    { "id": "card-boundary", "headword": "boundary", "unit_type": "Noun", "translation": "граница ответственности", "sense_summary": "разделение зон ответственности" }
  ]
}
"""

private const val LEMMA_COUPLING_FIXTURE = """
{
  "id": "lemma-coupling",
  "text": "coupling",
  "related_cards": [
    { "id": "card-coupling", "headword": "coupling", "unit_type": "Noun", "translation": "связанность между модулями", "sense_summary": "степень зависимости модулей друг от друга" }
  ]
}
"""

private const val LEMMA_SCALABLE_FIXTURE = """
{
  "id": "lemma-scalable",
  "text": "scalable",
  "related_cards": [
    { "id": "card-scalable", "headword": "scalable", "unit_type": "Adjective", "translation": "масштабируемый", "sense_summary": "о системе — растёт под нагрузкой без переписывания" }
  ]
}
"""

private const val LEMMA_LATENCY_FIXTURE = """
{
  "id": "lemma-latency",
  "text": "latency",
  "related_cards": [
    { "id": "card-latency", "headword": "latency", "unit_type": "Noun", "translation": "задержка отклика", "sense_summary": "время между запросом и ответом" }
  ]
}
"""

private const val LEMMA_THROUGHPUT_FIXTURE = """
{
  "id": "lemma-throughput",
  "text": "throughput",
  "related_cards": [
    { "id": "card-throughput", "headword": "throughput", "unit_type": "Noun", "translation": "пропускная способность", "sense_summary": "объём работы в единицу времени" }
  ]
}
"""

private const val LEMMA_BOTTLENECK_FIXTURE = """
{
  "id": "lemma-bottleneck",
  "text": "bottleneck",
  "related_cards": [
    { "id": "card-bottleneck", "headword": "bottleneck", "unit_type": "Noun", "translation": "узкое место", "sense_summary": "самый медленный участок, ограничивающий систему" }
  ]
}
"""

private const val LEMMA_RELIABLE_FIXTURE = """
{
  "id": "lemma-reliable",
  "text": "reliable",
  "related_cards": [
    { "id": "card-reliable", "headword": "reliable", "unit_type": "Adjective", "translation": "надёжный", "sense_summary": "о том, на что можно положиться" }
  ]
}
"""

private const val LEMMA_SCHEDULE_FIXTURE = """
{
  "id": "lemma-schedule",
  "text": "schedule",
  "related_cards": [
    { "id": "card-schedule", "headword": "schedule", "unit_type": "Noun", "translation": "расписание, график", "sense_summary": "план дел во времени" }
  ]
}
"""

private const val LEMMA_MEETING_FIXTURE = """
{
  "id": "lemma-meeting",
  "text": "meeting",
  "related_cards": [
    { "id": "card-meeting", "headword": "meeting", "unit_type": "Noun", "translation": "встреча, совещание", "sense_summary": "запланированный сбор людей" }
  ]
}
"""

private const val LEMMA_DEADLINE_FIXTURE = """
{
  "id": "lemma-deadline",
  "text": "deadline",
  "related_cards": [
    { "id": "card-deadline", "headword": "deadline", "unit_type": "Noun", "translation": "крайний срок", "sense_summary": "момент, к которому работа должна быть готова" }
  ]
}
"""

private const val LEMMA_PRIORITY_FIXTURE = """
{
  "id": "lemma-priority",
  "text": "priority",
  "related_cards": [
    { "id": "card-priority", "headword": "priority", "unit_type": "Noun", "translation": "приоритет", "sense_summary": "то, что делается раньше остального" }
  ]
}
"""

private const val LEMMA_FEEDBACK_FIXTURE = """
{
  "id": "lemma-feedback",
  "text": "feedback",
  "related_cards": [
    { "id": "card-feedback", "headword": "feedback", "unit_type": "Noun", "translation": "обратная связь", "sense_summary": "реакция, помогающая улучшить работу" }
  ]
}
"""
