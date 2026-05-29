# Как редактировать курированные данные верификации

Наши справочные данные (frequency, CEFR, senses, family) отдаёт бэкенд по
HTTP. В этой сборке их роль играет **мок-бэкенд**: данные лежат
редактируемыми JSON-фикстурами, по одной на фасет, под

```
shared/verification/sensee-curated/src/commonMain/mockFixtures/verification/
├── frequency.json
├── cefr.json
├── senses.json
└── family.json
```

Менять Kotlin-код для обычного добавления лемм/смыслов/семейства не нужно —
правится только JSON. Адаптеры `sensee-curated` фетчат каталог через
`HttpClient`, кэшируют его в памяти и мапят в `core`-результаты.

> Для курированного AI enrichment см.
> [`shared/ai/curated-enrichment`](../ai/curated-enrichment) — там по файлу на
> лемму под `src/commonMain/mockFixtures/enrichment/<lemma-slug>.json` в форме
> `EnrichmentResponseV1`.

## Как работает цикл

1. Правите нужный JSON-файл (форматы — ниже).
2. `SenseeCuratedMockFixturesTest` (commonTest) декодирует каждый каталог и
   проверяет, что он непустой и ключи лемм в нижнем регистре — это страховка
   от опечатки в форме/регистре. Рантайм ищет лемму по `term.trim().lowercase()`,
   поэтому **ключ должен быть в нижнем регистре**, иначе он никогда не сматчится.

Реальный бэкенд позже будет отдавать те же формы под тем же
`SenseeCuratedSource` — клиент не поменяется. Поэтому форма фикстуры = контракт
API.

## Форматы фасетов

### `frequency.json` — частотность

Лемма → Zipf-оценка (Double, шкала Брисбарта 0–8: 0 = не встречается,
~8 = `the`/`of`). Адаптер выводит частотную полосу (Top1k…Beyond20k) из Zipf.

```json
{ "lemmas": { "come": 5.4, "across": 4.8 } }
```

### `cefr.json` — уровень CEFR

Лемма → уровень `A1`..`C2`.

```json
{ "lemmas": { "come": "A1", "elucidate": "C1" } }
```

### `senses.json` — инвентарь смыслов

`defaultPos` применяется ко всем смыслам, у которых нет своего `pos`. Каждый
смысл: `id` (стабильный, попадает в `LexicalSourceRef.senseId`), `label`
(глосс), опционально `cefr`.

```json
{
  "defaultPos": "verb",
  "lemmas": {
    "come": [
      { "id": "come.1", "label": "move towards the speaker or a place", "cefr": "A1" },
      { "id": "come.2", "label": "arrive at a place or event", "cefr": "A1" }
    ]
  }
}
```

### `family.json` — семейство

Head-лемма → `canonicalLemma` + список юнитов. У юнита `type` — `word` |
`phrasal` | `idiom` (мапится в entry-type `word` / `phrasal_verb` / `idiom`),
`display` — отображаемая форма. Компоненты юнита строятся так: первым всегда
идёт head (`canonicalLemma`, роль `head`), затем — частица фразового глагола
(`secondaryText` + `secondaryRole`) или фиксированный объект идиомы
(`fixedObject`, роль `fixed_object`).

```json
{
  "families": {
    "come": {
      "canonicalLemma": "come",
      "units": [
        { "type": "word", "display": "come" },
        { "type": "phrasal", "display": "come across", "secondaryText": "across", "secondaryRole": "particle" },
        { "type": "idiom", "display": "come of age", "fixedObject": "of age" }
      ]
    }
  }
}
```

## Лицензии

Все наши данные licence-clean: один дескриптор `SenseeCuratedSource`
(`storeContentAllowed`, `usableAsLlmContext` — оба `true`). Не добавляйте сюда
содержимое, которое мы не вправе хранить и отдавать как собственные
licence-clean данные (коммерческие словари: Cambridge / Oxford /
Merriam-Webster) — для такого нужен либо отдельный network-адаптер с честными
лицензионными флагами, либо договорённость. Подробнее про флаги — `README.md`
и ADR-007.
