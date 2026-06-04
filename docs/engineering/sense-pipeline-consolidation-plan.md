# План: консолидация пайплайна `Sense` — единый sense-store, стабильный `sense_id`, табурет

Рабочий план рефакторинга (не постоянная архитектурная справка и не backlog-пункт).
Описывает целевое состояние и шаги к нему. Удалить после завершения — история
остаётся в git. Идентификаторы, пути, команды и имена типов — в исходной английской
форме в backticks.

## 1. Контекст и цель

`Sense` — центральная лексическая модель ([`shared/lexicon/domain/Sense.kt`](../../shared/lexicon/domain/src/commonMain/kotlin/app/sensee/lexicon/domain/Sense.kt)).
По продуктовому замыслу результат AI enrichment, ручная карточка (будущий конструктор)
и единица набора от сервиса — это **один и тот же** `Sense`. Наборы сервиса приходят как
**уже идеальные** `Sense`. Сейчас вокруг модели накопилось переусложнение: легаси-слой
имён, два параллельных suggest-use-case, неподключённая в живой флоу машинерия
верификации, лишний враппер `EnrichedSense`, зеркальные DTO на границах, промежуточный
enrichment-крюк на пути наборов сервиса и дублирование `Sense` в двух durable-схемах с
размытым ownership.

**Табурет (несущий принцип).** Из всей сложности фич делаем максимально просто. У
консолидации **четыре ноги**, и только они — ядро:

1. **Один `Sense`, один стор** (`sense`, blob = истина, производные колонки).
2. **Стабильный `sense_id` = идентичность = якорь SRS** (минтим на новом, сохраняем на правке).
3. **Один транзакционный write API** (явный `WriteIntent`: `ResolveOrMint` · `UpdateExisting(sense_id)` · `ForceMint`).
4. **Одна write-сторона** (editor-state) **+ две derived read-проекции** (`library`, practice) — ни одна не владеет identity/SRS.

Всё остальное (embedding, example-quality, граф, поиск, decks, draft-очередь, свап
service-wire) — **навесное за портом**: каждое = порт + тупейшая impl, отрезаемо и не
трогает ноги. Ядро (§6, Фазы 0–4′) доводим первым; навесное — отдельными изменениями
после ядра (§6 «После ядра»).

**Mock-backend + integration — это продакшн-вариант.** `shared/ai/curated-enrichment`,
verification-провайдеры и mock-каталог — это **mock-backend-реализация стабильных швов**
(`AiEnrichmentClient`, `verification/integration`, `CatalogRemoteDataSource`), а не
временные леса. Это полноценный продакшн-режим; будущий бэкенд **подменяет impl за тем же
швом** (паттерн `<area>/integration` + mock-vs-real engine). Слово «временный» к этим
вещам не применяем: они работают как продакшн сейчас и переезжают на сервер без смены
контрактов. В пределе бэкенд вернёт `Sense`+`cefr`+`embedding`, и capture-путь сойдётся с
service-путём (`SenseDto`).

**Сквозной принцип (governing).** enrichment, verification, **граф связей**
(леммы/sub-леммы/смыслы/синонимы), **поиск** и embedding в пределе — backend. Клиент
держит **кэш + минимальные производные проекции за портами**; на клиенте **не строим
авторитетные движки графа или поиска** — только тонкие производные impl, заменяемые на
backend. Любую новую возможность закладываем как порт + минимальную локальную impl.

**Embedding — один путь.** `EmbeddingPort.embed()` на save для любого смысла, попадающего
в наборы пользователя (capture-confirm, добавление service-набора, ручной/правленый).
Вектор провайдерный (сетевой), не on-device; **нет отдельного wire-поля и нет
baked-фикстур** — один и тот же вызов для всех. Покрытие = членство в наборах пользователя. Вызов **eager на save синхронно онлайн**
(таймаут-бюджет), при офлайне/сбое — отложенно; save **никогда не блокируется**. Это навесное
за портом (после ядра); near-dup → **выбор пользователя**, не пассивный баннер (см. §2 и §6).

**Контекстный пример — инвариант модели (для всех origin).** Смысл существует в употреблении:
подтверждённый `Sense` несёт **минимум один** `ContextualApplication` — и Personal, и Service.
Это инвариант канона (рядом с непустым `translation`), а не опциональная фича. Точка истины —
**единственная write-граница**: confirm-gate для Personal `Draft→Confirmed` (§2) и валидация
Service-ingest (§4: смысл без примера не входит в `sense` как `Confirmed` — drop + diagnostic +
fixture-completeness тест). AI-кандидат с нулём примеров отсеивается на маппинге; ручной смысл
неконфёрмабелен, пока примера нет. Тихий example-filter (§4) **никогда не опустошает** список.

Цель: упростить так, чтобы все флоу сходились на одном `Sense` и одном источнике истины;
на каждом пути держать минимум моделей; идентичность и SRS-профиль не привязывать к
изменяемому контенту.

**Активная разработка — обратную совместимость не держим.** Нет внешних потребителей и
продакшен-данных: **никаких миграций** (единый v1-снимок, `dev-reset` при drift схемы),
схему БД и wire **ломаем свободно** (re-key фикстур, переименования, удаление полей);
**легаси удаляем, а не депрекейтим**. Это не отменяет дешёвую устойчивость чтения
(ignore-unknown) и структуру wire-шва ADR-005/0006 — они нужны не ради обратной
совместимости (см. §3). Cutover — жёсткий `dev-reset`: локальные captured-смыслы и
история SRS на переходе стираются (для активной разработки ок).

## 2. Зафиксированные решения

* **Verification → две узкие роли (не evidence-only).** (1) **pre-AI grounding** — словарные
  подсказки в промпт; (2) **post-AI example-quality** — тихий, не блокирующий отсев слабых
  примеров перед save. Снимаем только **per-candidate snapshot-оркестрацию**
  (`VerifyVocabularySuggestionsUseCase`, `SenseVerificationSnapshot`/findings/scope — собрана,
  протестирована, но **не подключена**; доки описывают её как реализованную — правится в
  Фазе 0). `ExampleQualityChecker` и модуль `languagetool` **сохраняем** (мок-backed продакшн
  за швом), но без Set-фан-аута/`SourceCatalog`-полноты — один опциональный вызов. На AI-стороне
  тип переименовываем по роли: **`EnrichmentEvidence → EnrichmentGrounding`** (+
  `GroundingModifier`; `EnrichmentEvidenceBuilder` в `vocabulary-editor/domain` →
  `EnrichmentGroundingBuilder`); слово «evidence» остаётся только за verification.
* **Единый sense-store с бинарным статусом + read-model `StoredSense`.** Один durable-store
  `Sense` (`status = Draft|Confirmed`, без промежуточных — §8). Чтение (`SenseReadRepository`) отдаёт **не
  голый `Sense`**, а плоскую запись `StoredSense` (`sense_id · status · origin · source_ref ·
  cefr · updated · sense: Sense`): `sense_id`/метаданные живут **здесь**, `Sense` остаётся
  чистым контентом. `vocabulary-editor` = authoring/edit; `library` = проекция (Confirmed →
  практика; Draft → очередь «незавершённое»); `practice` = SRS поверх того же канона. Никаких
  двух durable-копий.
* **Draft-payload + confirm-gate.** `translation: String` остаётся обязательным в
  `Sense`/`SenseDto` (канон строгий, nullable-правки нет), но `""` — легальное значение
  **только при `status=Draft`** (draft несёт `surfaceForm=term`, перевод/поля заполняются позже).
  Переход **Draft→Confirmed** проходит **confirm-gate**: `require(translation.isNotBlank())`
  **и** `require(contextualApplications.isNotEmpty())`. `status` несёт «незавершённость» — не
  магия в полях, не отдельный `isDraft`. (Будущий авто-переводчик предзаполняет `translation`
  до enrichment, сжимая окно пустого draft — отдельный `EB`, на инвариант не влияет.)
  Draft **минтит `sense_id` один раз при создании** (далее id-preserving обновления); `content_key`-резолв
  применяется только к **полному** ключу (непустой `translation`) → черновики одного слова **не схлопываются**
  (не затираем незавершённое), дедуп — на confirm, где ключ полон.
* **Идентичность → стабильный `sense_id`; `content_key`/`embedding` — производные сигналы.**
  PK канона и якорь SRS — типизированный `sense_id` (**value class `SenseId(val raw: String)`** в
  `lexicon.domain`, симметрично существующему `SrsCardId`): **Personal → `kotlin.uuid.Uuid`**
  (stdlib, все таргеты, без clock; каталог уже на Kotlin `2.4.0` — генерация инкапсулируется в
  `SenseIdFactory` (`lexicon/data`), `@OptIn(ExperimentalUuidApi)` локализован там, если
  `Uuid.random()` ещё требует opt-in), **Service → детерминированный namespaced id из `source_ref`**
  (стабилен между синками) — **не сырой `source_ref`**: либо name-based hash (`SHA-256(source_ref)` →
  формат UUID), либо префикс `svc:` с экранированием. Причина: form-key адресуется строкой
  `${sense_id}:form:${formId}` — сырой `source_ref` с `:`/`:form:` сломал бы парсинг, а общий PK-столбец
  `sense_id` не должен путать Personal-UUID и Service-id. **Четыре ключа, по
  одной задаче:** `sense_id` = кто это (PK) · `lemma_key` = как группируем · `content_key`
  (`surfaceForm|unitType|translation`) = дешёвая проверка точного дубля · `embedding` = «похоже» →
  **выбор пользователя** (опц.). `content_key`/`embedding` — **не** PK и **не** идентичность.
  `content_key` нормализуется **консервативно** (регистр, повторные/обрамляющие пробелы,
  обрамляющая пунктуация `translation`; слоты `surfaceForm` уже нормализует `display()`) — шире
  ловит точные дубли, не склеивая разные смыслы; `cefr` в ключ не входит.
* **Один транзакционный write API (цель: один смысл → беречь SRS, по возможности без дублей).**
  - **Новые write** (capture-confirm нового, ручной save нового, service ingest) идут с явным
    `WriteIntent`: `ResolveOrMint` ищет существующий `sense_id` и переиспользует его (SRS цел),
    минтит только при отсутствии совпадения; `UpdateExisting(sense_id)` заменяет контент выбранной
    строки; `ForceMint` создаёт новую строку даже при совпавшем `content_key`. `resolveOrMintSenseId`
    вызывается **только** для `ResolveOrMint`, не после пользовательского выбора `ForceMint`.
    **Origin-scoped:** user-write резолвит `content_key` **только среди `origin=Personal`**;
    Service-ingest резолвит **только по `source_ref`**. `origin`-precedence: синк `Service` **не
    перетирает** строку, уже `Personal`.
  - **Рост леммы = per-sense resolve, не term-агрегат.** Лемма не хранится агрегатом — это
    производная группа по `lemma_key`. Добавление по существующей лемме = независимый
    `ResolveOrMint` на каждый входящий смысл без явного выбора (точный `content_key` →
    переиспользование `sense_id`, SRS цел; новый → строка-сиблинг под тем же `lemma_key`).
    Если точный дубль предъявлен пользователю, выбор «создать новую» идёт через `ForceMint`.
    Term-уровневый merge и
    `updateConfirmedEntry` delete+reindex **упраздняются** (orphan-баг исчезает: нет entry — нечего
    удалять, SRS на `sense_id`). Пакет одного confirm дедуплицируется по `content_key` **до записи**
    (иначе два кандидата с одним `content_key` оба сохранятся и форкнут SRS).
  - **Tie-break при неуникальном `content_key`.** Среди Personal `content_key` **не уникален** (его
    плодят ветка `ForceMint` и правка-в-коллизию ниже), поэтому `ResolveOrMint` может найти ≥2
    кандидата. Детерминированно: **переиспользуем строку с max `updated`**. Именно `updated` —
    он lexicon-owned (konsist не пускает `lexicon/data` к SRS, а это и есть единственный доступный сигнал).
    Оговорка: tie-break даёт **детерминизм, не максимум сохранённого SRS** — на редком пути новый write
    может приклеиться не к SRS-носителю; приемлемо, дубли консолидируются позже. `ForceMint`
    tie-break не запускает.
  - **Правка существующего** (`Sense` уже в сторе) — **id-preserving**: берёт известный `sense_id`,
    пересчитывает производные колонки (`content_key` может смениться), **не** резолвит и **не**
    мёрджит. Если post-edit `content_key` совпал с другим смыслом → **информационное
    предупреждение** (без choice-диалога, без merge): беречь SRS важнее редкого дубля-от-правки,
    консолидация дублей — позже и явно (удаление лишней карточки / «возможные дубли» в `SearchPort`).
  - **Дубликат при добавлении — решение принимает пользователь, не эвристика; два слоя, разный
    таймлайн.** **Точный `content_key`** среди Personal — синхронно, всегда → диалог «заменить контент
    существующей карточки (`UpdateExisting(sense_id)`, тот же `sense_id`/SRS) / создать новую
    (`ForceMint`, новый `sense_id`, resolver не схлопывает обратно по `content_key`)». **Близкий embedding** — `embed()`
    **eager на save синхронно онлайн** (таймаут-бюджет), near-match → тот же выбор + «связать как
    родственные» (lemma-семья/synonyms) + запомнить «другой смысл» (`dismissed-pairs`, чтобы
    ре-эмбеддинг не переспрашивал). **Офлайн/таймаут/сбой** → строка коммитится тихим сиблингом, save
    **не блокируется**; пара уходит в «возможные дубли» (`SearchPort`), решение предъявляется
    отложенно (ленивый ре-эмбеддинг → та же поверхность). Без тихого merge. Контракт выбора и
    `WriteIntent` — решение **ядра**; вычисление вектора/`findSimilar` — навесное.
  - **Профилактика — first, остаточный хвост — принимаем.** `ResolveOrMint`+точный `content_key` —
    дефолт на всех путях; диалог точного совпадения по умолчанию ведёт к «это тот же смысл»
    (reuse/replace), `ForceMint` («создать новую») — намеренно **вторичное** действие. «Полностью
    дубли не убрать, но не даём появиться случайно»: остаточный хвост (`ForceMint`,
    правка-в-коллизию, кросс-origin Personal+Service, офлайн-отложенный near-dup) принимаем явно — он
    всплывает в «возможные дубли» (`SearchPort`) для ручной консолидации; tie-break `max(updated)` =
    детерминизм, не максимум сохранённого SRS.
  - **`resolve + upsert` — одна транзакция.** Реальная гонка — **два писателя в одном origin-скоупе**
    (двойной confirm; autosync vs ручное добавление того же `source_ref`); confirm и sync в *разных*
    скоупах и так минтят разные id. Service-путь: `source_ref` обязателен (`CHECK(origin != 'Service'
    OR source_ref IS NOT NULL)`) + partial unique index
    `CREATE UNIQUE INDEX sense_service_source_ref ON sense(source_ref) WHERE origin = 'Service'`
    (partial uniqueness в SQLite — только индексом, не table-constraint'ом) = один id. Personal `content_key` уникальным
    быть не может → гарантия = транзакция + tie-break только для `ResolveOrMint`.
* **Сервисные наборы — две явные операции (subscribe / claim), не «адопция».**
  - **Подписаться** (`subscribe`, набор целиком) → **зеркало**: смыслы `origin=Service`, ре-синк по
    `source_ref` перетирает контент, SRS на `sense_id` цел → обновления набора **прилетают**
    пользователю. `deck_membership` ссылается на Service-`sense_id`; контент не копируется.
  - **Добавить себе / завладеть** (`claim`, один смысл, часть или весь набор) → **детачнутый
    снимок**: новый `Personal`-`sense_id` **всегда** (`ForceMint`, без claim-time дедупа), копия
    контента на момент, **связи с набором ноль** (обновления не касаются), дальше — обычный `Sense`
    (правится, кладётся в личные наборы, участвует в Personal-дедупе **впоследствии**). Клон SRS —
    **только FSRS-snapshot** на новый `sense_id` (SRS-op `copySnapshot`); review-логи **не**
    дублируются (стартуют с нуля; `getReviewLogs` сейчас всё равно не читается). Разовый снимок, без
    дальнейшей связи. **Почему всегда `ForceMint`, не `ResolveOrMint`:** иначе claim попал бы в уже
    существующий Personal `sense_id`, и `copySnapshot` затёр бы накопленный SRS — claim бьёт SRS только
    в свежий id. Случайный повторный claim — остаточный хвост → «возможные дубли» (`SearchPort`).
  - **`claim` — оркестратор вне lexicon, не метод write-репозитория.** Он трогает три владельца:
    контент-копию (`lexicon` write `upsert(ForceMint)`), `copySnapshot` (SRS-примитив) и
    `deck_membership` (`library`). Внутри `lexicon/data` это запрещено konsist'ом (`lexicon` без
    SRS/feature). Дом оркестратора — `library/data` (владелец service-ingest и deck-схемы). **Без ребра
    `library/data → practice`:** `copySnapshot` живёт на SRS-уровне (default-метод `SrsCardStore` в
    `srs.engine`, выражен через `getCard`/`saveCard`), а `library/data` уже зависит от `srs.engine`/
    `srs.fsrs` и уже инжектит `SrsStorage` — зовёт его напрямую; `PracticeSrsStorage` (impl) получает
    метод наследованием. Оркестратор (он держит `Sense`) перечисляет id карт — базовую + формы
    `${sense_id}:form:…` — и копирует каждую. **Атомарность:** все таблицы в одном `SenseeDatabase`,
    поэтому `claim` оборачивается в нейтральный `DatabaseTransactionRunner` (**в `shared/database`**,
    поверх `SenseeDatabase.transactionWithResult`; все data-impl уже зависят от `shared/database`);
    внутренние операции (`copySnapshot`/`upsert`/membership) свою транзакцию **не** открывают, а
    энлистятся, `SrsTransactionRunner` делегирует тому же DB-transaction. Тот же шов нужен confirm'у
    (`sense` + первичная материализация SRS).
  - Зеркало править нельзя → **правка подписанного смысла = `claim`, затем правка** (отдельный
    copy-on-write-fork как концепт не нужен — EB-12 схлопывается в это). **`derived_from` не вводим**
    нигде — линковки нет ни в одном режиме.
  - **В ядре (4′):** `origin`-семантика + правило `claim` + **минимальная deck-модель**
    `deck`/`deck_membership` (прямой преемник `practice_deck`/`practice_deck_card`) + service-ingest
    **как деки** + флаг `subscribed` (преемник `adopted_at`) — иначе 4′ снёс бы текущий service
    browse/adopt. **Навесное:** Personal-deck CRUD, claim-UI (добавить подмножество), reorder, витрина
    каталога (`service_catalog`).
* **Цепочка моделей схлопывается.** Канон: `EnrichmentItemV1 (wire) → EnrichmentSuggestion
  (neutral) → Sense (канон) ↔ SenseDto (persist/service-wire)`. Удаляем `EnrichedSense`
  (его единственная *дополнительная* нагрузка — `contentFingerprint` — больше не идентичность;
  маппер отдаёт `Sense` напрямую) и доменный `SenseCandidate`. Editor-типы — доменный
  `SenseCandidate` (presentation использует его через alias `MeaningCandidate`) + `ManualSense`
  (`presentation/api`) — сворачиваются в **один presentation-state тип** (in-progress `Sense` + источник
  AI/manual + статус + выбор); выбор по `content_key`. `Card` — read-вьюха сохранённого `Sense`.
  Ни `SenseCandidate`, ни `Card` не звенья конвейера — это две тонкие вьюхи `Sense`.
* **Ручной ввод/редактирование без verify+AI.** Создать/править `Sense` руками без авто-цепочки;
  обогащение **всего смысла** по требованию (`completeManualSense`). Посекционное обогащение пока
  не вводим (→ `EB-11`). Редактирование сохраняет `sense_id`.
* **`cefr` → first-class и типизированный.** Канонический enum `CefrLevel` (`A1..C2`) в
  **`lexicon.domain`** (лексический атрибут `Sense`, ноль новых зависимостей); `Sense.cefr:
  CefrLevel?`. `SenseDto.cefr` — `String?` на wire (forward-compat), `toDomain`/маппер парсят в
  enum. `verification.core` держит **намеренный** локальный `CefrLevel`-мирор (лист без зависимостей). cefr
  — атрибут, **не** часть `content_key`.
* **`SenseReadRepository` + `SenseWriteRepository` — раздельные контракты, один impl** в `lexicon`.
  Проекции (`library`/`practice`) видят **только** read (`observe`/`getById`/`lemma_key`/`status`);
  write (`upsert`/`WriteIntent`) — только editor + service-ingest. Это структурно (konsist)
  защищает ногу «одна write-сторона, две read-проекции», а не по соглашению. **`claim` — НЕ метод
  этого контракта:** lexicon-write минтит лишь Personal-копию контента (`upsert(copiedSense, ForceMint)`);
  полный `claim` (копия + клон SRS + deck membership) — оркестратор вне lexicon (см. service sync).
* **Организация пакетов.** Разросшиеся модули реоргуются по концернам — **в конце** той фазы,
  что доводит модуль.

## 3. Текущее состояние (срез фактов)

Представления одного смысла сейчас (7 — будут схлопнуты, см. §2):

```
EnrichmentItemV1 (wire) → EnrichmentSuggestion (neutral) → EnrichedSense (sense+fingerprint)
   → Sense (domain) → SenseDto (persist) → SenseCandidate / ManualSense (editor) → Card(+sense) (library)
```

Хранение `Sense` сейчас в двух местах:

| Где | Таблица | Владелец | Форма |
|---|---|---|---|
| capture | `lexical_entry.senses_json` | `vocabulary-editor` | `List<SenseDto>` |
| catalog | `practice_card.sense_json` | `library` | `SenseDto` на карту |

Подтверждённые проблемы (с привязкой к коду):

1. **Evidence→AI→snapshots не подключены к живому флоу.**
   [`VocabularyCaptureLogic`](../../shared/feature/vocabulary-editor/presentation/impl/src/commonMain/kotlin/app/sensee/feature/vocabularyEditor/presentation/impl/VocabularyCaptureLogic.kt)
   инжектит legacy-facade `SuggestVocabularyMeaningsUseCase` → `enrich(EnrichmentRequest(term))`
   без evidence и без verifier. `SuggestVocabularySensesUseCase`,
   `VerifyVocabularySuggestionsUseCase`, `EnrichmentEvidenceBuilder.toEnrichmentEvidence`,
   доменный `completeManualSense` — достижимы лишь из этой мёртвой цепочки (её дёргают только тесты). `LexicalVerifier` биндится
   (`PersistedCachingLexicalVerifier`) и инжектится в эти use-case'ы, но **сами use-case'ы в живой
   флоу не запрашиваются** — мёртвая цепочка целиком; UI не читает `SenseVerificationSnapshot`.
   **НО** есть живой user-facing путь `VocabularyCaptureLogic.completeManualWithAssistant`
   ([`:172`](../../shared/feature/vocabulary-editor/presentation/impl/src/commonMain/kotlin/app/sensee/feature/vocabularyEditor/presentation/impl/VocabularyCaptureLogic.kt))
   → `enrichAndMap` (проброшен в `DefaultVocabularyCaptureComponent`), идущий тем же **grounding-less**
   маршрутом, что и capture, — ручное обогащение сейчас без grounding.
2. **Legacy-слой имён.** [`Meaning.kt`](../../shared/feature/vocabulary-editor/domain/src/commonMain/kotlin/app/sensee/feature/vocabularyEditor/domain/Meaning.kt):
   `Meaning = Sense`, `MeaningCandidate = SenseCandidate`, `MeaningCandidateId`,
   `ContextualApplication = …`. Плюс параллельный `*Meaning*`-API.
3. **Дублирующая оркестрация enrich+map.** `enrichAndMap` — вынесенный хелпер, зовущий тот же
   `toMeaningCandidates`; дублируется не mapping-логика, а **оркестрация** (enrich→map делается в
   двух точках: `enrichAndMap` и `SuggestVocabularyMeaningsUseCase`).
4. **Зеркальные типы границ — почти все намеренные.** `AlignmentChunk` — 4 представления
   (`AlignmentChunkV1` wire, `AlignmentChunk` neutral в `ai.core`, `AlignmentChunk` в
   `lexicon.domain`, `AlignmentChunkDto` в `lexicon.serialization`); `UnitComponentFact` — в
   `ai.core` и `verification.core` (нулевые листья без общего предка). `EnrichmentExample`
   ↔ `ContextualApplication` — **не** зеркала (anti-corruption parse в `StudiedSentence`,
   [`EnrichmentSenseMapper.kt:116`](../../shared/lexicon/enrichment/src/commonMain/kotlin/app/sensee/lexicon/enrichment/EnrichmentSenseMapper.kt)).
5. **Промежуточный enrichment-крюк на пути наборов сервиса.** `CardDto.enrichment` —
   `EnrichmentItemV1` (тот же versioned wire `ai.core`, что и для capture). Он **не** совпадает
   поле-в-поле с `SenseDto`: примеры различаются — `EnrichmentItemV1.examples` несёт сырое
   предложение-строку, а `SenseDto.contextualApplications` — уже распарсенный `StudiedSentence`.
   Поэтому каталог гоняет уже-идеальные смыслы через **всю** enrichment-цепочку (`EnrichmentItemV1
   → EnrichmentSuggestion → Sense`, с `StudiedSentence.parse` и резолвом таксономии), а не через
   короткий `SenseDto.toDomain()`. Лишняя цепочка + зависимости `library/data` на `shared.ai.core`
   и `shared.lexicon.enrichment`.
6. **`Sense` в двух durable-схемах; идентичность и SRS привязаны к контенту/позиции.**
   Captured-дека деривится на каждом чтении ([`CapturedCatalogDerivation`](../../shared/feature/library/data/src/commonMain/kotlin/app/sensee/feature/library/data/CapturedCatalogDerivation.kt),
   фильтр `status == Confirmed`). **SRS ключуется по `card id`**
   ([`CapturedCatalogRepository.kt:47`](../../shared/feature/library/data/src/commonMain/kotlin/app/sensee/feature/library/data/CapturedCatalogRepository.kt)),
   а `card id` у sense-карт **позиционный** (`captured:${entry.id}:${index}`), у форм — **по
   lemma-key** (`captured:lemma:…:form:`). `content_key` (`deriveSenseContentKey`) участвует
   только в `mergeSenses`-дедупе, **не** в ключе SRS. Следствие (точный механизм): SRS сиротеет
   при **реордере/удалении** смысла (сдвиг индекса), правке `headLemma`/`baseLemma` (смена
   lemma-key форм-карты), term-collision merge в `updateConfirmedEntry` (удаляет запись и
   переиндексирует под чужой id) и re-capture с новым `content_key`. Стабильного `sense_id` нет.
   `Card` несёт lean-проекцию **и** полный `sense: Sense?`.
7. **Draft не имеет отдельного listing-API и не виден из Library.** `listEntries`/`observeEntries`
   отдают всё; captured-дека показывает только `Confirmed`.
8. **`cefr` молча теряется на границе домена.** `CefrEnrichmentExtension` →
   `EnrichmentSuggestion.extensions`, но `toEnrichedSense` extensions не читает → в
   `Sense`/`SenseDto` cefr не доходит. Дублируется в `EnrichmentEvidence.cefr`,
   `LexicalVerificationReport.cefr`, enum `CefrLevel` (verification.core).
9. **Embedding/семантического сходства нет вообще.** Единственный механизм «совпадения» —
   точный `deriveSenseContentKey`.

Что НЕ трогаем (оставить): anti-corruption граница `EnrichmentSuggestion → Sense` и parse в
`StudiedSentence`/таксономию; `EnrichmentResult.availability` как first-class;
forward-compatible read (`ignoreUnknownKeys`, [`JsonProviders.kt`](../../shared/core/network/src/commonMain/kotlin/app/sensee/core/network/JsonProviders.kt));
versioned wire `EnrichmentResponseV1`/`EnrichmentItemV1` (ADR-005/0006) для capture-обогащения.
Эти «compat»-вещи держим **не ради обратной совместимости**: forward-compatible read — дешёвая
устойчивость к вариативности вывода LLM/бэкенда; versioned wire — структура шва под бэкенд. Сам
wire/схему ломаем свободно (re-key, dev-reset).

## 4. Целевая модель (ownership)

`shared/lexicon` становится владельцем канона, оставаясь **минимально-зависимым**:

* **`lexicon/domain`** — чистые контракты + чистые утилиты, **ноль новых зависимостей**
  (как сейчас: `grammar.domain` + coroutines):
  - `SenseReadRepository` (`observe`/`getById`/по `lemma_key`/`status`, отдаёт `StoredSense` =
    `sense_id` + метаданные + `Sense`) — для проекций; `SenseWriteRepository`
    (`upsert`/`WriteIntent`) — для editor+ingest (`claim` здесь **нет** — он оркестратор вне lexicon, §2).
    Один impl, два интерфейса; konsist: проекции видят только read.
  - `StoredSense` — read-model канона; `deriveSenseContentKey`, lemma-резолвер, cosine-util, `CefrLevel`.
  - Порты навесного (контракты здесь, impl — `lexicon/data`): `EmbeddingPort`,
    `SearchPort`/`SenseQuery`, `LexicalGraphReader`.
* **`lexicon/database-schema`** (новый, schema-only `.sq`) **+ `lexicon/data`** (impl обоих
  контрактов, `WriteIntent`, `resolveOrMintSenseId`), агрегируется в `SenseeDatabase` (`shared/database`),
  как seam-owned `verification/database-schema` (ADR-002). **Konsist по слоям:** `lexicon/domain` —
  чистый (нет persistence/SRS/feature); `lexicon/data` — **можно** `shared/database` +
  `lexicon/database-schema`, **нельзя** feature/SRS; `lexicon/database-schema` — schema-only. Цикла нет
  (`lexicon/database-schema ← shared/database ← lexicon/data`, как у `practice`/`verification`).
  SRS-джойн остаётся в `library`.
  Checklist cutover: добавить `include(":shared:lexicon:database-schema")` и
  `include(":shared:lexicon:data")` в `settings.gradle.kts`; в `shared/database/build.gradle.kts`
  добавить `evaluationDependsOn` и `sqldelight dependency(...)`; обновить LikeC4 ownership
  canonical `sense` schema с `vocabulary-editor` на `lexicon`; проверить `npx likec4 validate docs/c4`.
* **`sense`** — единственная durable-таблица `Sense`:
  `sense_id (PK) · status · sense_json (SenseDto blob = единственная истина агрегата) ·
  lemma_key · content_key · origin · source_ref · cefr · unitType · updated`.
  - `sense_json` blob — атомарная запись/чтение без джойнов, эволюция без миграций. Остальные
    колонки — **производный индекс**, пересчитываемый на `upsert`; колонку заводим только под
    реальный запрос/фильтр. Заводить колонки **легко** (миграций нет, dev-reset); сложность
    была лишь у `json_extract` **внутрь** blob (ненадёжно на web sql.js) — его не используем.
    Глубокое вложенное (`examples`/`alignment`) в колонки не выносим.
  - `sense_id` — типизированный `SenseId`, стабильный (Personal `Uuid` / Service namespaced из
    `source_ref`, §2), переживает любые правки.
  - `source_ref` — nullable для `Personal`, но обязателен для `Service` через
    `CHECK(origin != 'Service' OR source_ref IS NOT NULL)`; Service-уникальность — partial unique index
    `CREATE UNIQUE INDEX sense_service_source_ref ON sense(source_ref) WHERE origin = 'Service'`, не голый
    `UNIQUE(origin, source_ref)`, потому что SQLite допускает несколько `NULL` в unique-индексе.
  - `status` — бинарь `Draft|Confirmed`. Промежуточные статусы не вводим (§8).
  - `updated` пишем на каждый `upsert` (clock инжектится в `lexicon/data`, не в `domain`; нужно
    под draft-очистку EB-6).
* **`sense_embedding`** 〔навесное / после ядра — в 4′ **не** создаём〕 (отдельная таблица, чтобы строка
  была lean): `sense_id (PK/FK `ON DELETE CASCADE`) · vector (BLOB, нормализованный float32 через
  `FloatArray↔ByteArray`) · model_ref · dim`. Вектор пишет `EmbeddingPort.embed()` на save —
  один путь, не on-device. Схема целиком уходит в post-core embedding-изменение (план: «таблицу заводим
  только под реальный запрос», миграций нет → добавить позже бесплатно); 4′ создаёт лишь `sense`
  (+ deck-схему), от которой FK `ON DELETE CASCADE` и зависит.
* **Идентичность, дедуп, SRS:**
  - SRS ключуется по `sense_id` (формы — `${sense_id}:form:${formId}`, `formId` = грамматический
    слот, не позиция → стабильно при правках).
  - **Новые write** → `WriteIntent`: `ResolveOrMint` (origin-scoped: user-write по `content_key`
    среди `origin=Personal`; Service-ingest по `source_ref`) + `resolve + upsert` в одной транзакции;
    `UpdateExisting(sense_id)` для явной замены; `ForceMint` для явного «создать новую» без
    повторного resolve. Service-путь защищают `CHECK` по `source_ref` и partial unique index.
    `content_key` среди Personal **не уникален** → при ≥2 кандидатах tie-break **max `updated`**
    (lexicon-owned, konsist-safe) только для `ResolveOrMint`.
  - **Правка** → id-preserving (тот же `sense_id`, без re-resolve/merge); коллизия `content_key`
    с другим смыслом → информационное предупреждение, дубль разрешён (0 потерь SRS).
  - **Дубль при добавлении пользователем** → выбор «заменить контент» (`UpdateExisting`, сохраняет
    `sense_id`/SRS выбранной строки) / «создать новую» (`ForceMint`, новый `sense_id`). Без тихого merge.
  - `origin`-precedence: синк `Service` **не перетирает** `sense_json`, если строка уже `Personal`.
  - **Due-шов для Home (требует разрыва `practice/domain → library/domain`).** Контракт
    `DuePracticeRepository` в `practice/domain`: `countDue(now)`/`observeDueCount(now)` по `sense_id`,
    impl в `practice/data` поверх `SrsCardStore`/`SrsStorage`. **NB:** сейчас `practice/domain`
    `api`-зависит от `library/domain` — единственная причина — `PracticeReviewRepository.submitReview`
    возвращает `library.domain.Card`, поэтому `Home → practice/domain` **транзитивно** притащил бы
    `library`. В 4′ `submitReview` возвращает practice-owned результат (`SrsCardSnapshot`/`ReviewOutcome`),
    ребро `practice/domain → library/domain` **снимается**, `practice/domain` становится листом над
    `srs.core`, а `Card` собирает `library` как проекция. `Home` зависит только от `DuePracticeRepository`
    + navigation action в Practice; konsist-ассерт: `home/presentation:impl` транзитивно **не видит**
    `library.domain`. due-list, если нужен, собирает Practice как due-`sense_id` ⋈ lean
    `StoredSense`-проекция из blob.
* **`vocabulary-editor`** — три режима в один store: (A) AI-захват (`enrich` за швом → `Sense`
  → выбор → save); (B) ручной ввод/редактирование без авто-цепочки + `completeManualSense` по
  требованию; (C) правка существующего `Sense` (сохраняет `sense_id`). Editor держит **один**
  presentation-state тип (in-progress `Sense` + источник + статус + выбор + **lemma-контекст** для
  входа «добавить к лемме»: проставляет `headLemma`, чтобы сиблинг не уехал в чужую группу,
  derive-on-read — fallback для capture). Собственная durable
  `lexical_entry.senses_json` уходит.
* **`library`** — проекция: `Card`/`Deck`/`Lemma` над (`sense`-строка + SRS); SRS-джойн по
  `sense_id`. lean-поля `Card` — **projection-on-read** из `sense_json` (без денормализованных
  колонок; промоут только если список замерен медленным). `Confirmed` → практикуемые карты,
  `Draft` → отдельный список «незавершённое» в Library (SRS у draft нет). Статус — **per-sense**
  (не per-entry): Draft- и Confirmed-сиблинги одной леммы расходятся по проекциям; кросс-связь
  lemma↔draft не вводим — списка достаточно. `practice_card.sense_json` и derive-on-read уходят.
* **service sync — две операции владения (subscribe / claim), без `derived_from`.**
  `CatalogRemoteDataSource` наполняет **просматриваемый** каталог (`origin=Service`, ключ
  `source_ref`), без verify/AI-enrich; ingest **валидирует example-инвариант** (смысл без примера не
  входит как `Confirmed` — drop + diagnostic).
  - **Подписка (зеркало).** `deck_membership` ссылается на Service-`sense_id`; контент **не
    копируется**; ре-синк (`source_ref → тот же sense_id → upsert`) **перезаписывает контент**, SRS
    по `sense_id` цел → «service идеален, синк hands-off». Удаление смысла upstream → синк убирает
    membership.
  - **Claim (завладеть, детач).** Один смысл/часть/весь набор → копия в `sense` (`origin=Personal`,
    новый `sense_id` **всегда** через `ForceMint`, контент-снимок, клон **FSRS-snapshot** если был —
    `copySnapshot` на свежий id, без review-логов); **без claim-time дедупа** (иначе `copySnapshot` затёр
    бы SRS существующей Personal-строки) — результат участвует в Personal-дедупе впоследствии. Ложится в
    личный набор по ссылке на **Personal**-`sense_id`. **Связи с набором ноль** — обновления не касаются.
    Правка подписанного смысла = `claim` + правка. **`claim` — оркестратор в `library/data`** (не метод
    lexicon-write): композирует lexicon `upsert(ForceMint)` + `SrsCardStore.copySnapshot` + deck membership
    в **одной** транзакции через нейтральный `DatabaseTransactionRunner` (все таблицы в одном
    `SenseeDatabase`); `copySnapshot` — SRS-примитив в `srs.engine` поверх `getCard`/`saveCard`, **без**
    ребра `library/data → practice`.
  - **В ядре (Фаза 4′):** ingest прежним маппером в `sense` **как деки** (`deck`/`deck_membership` +
    флаг `subscribed`) + правило `claim` — прямой преемник `practice_deck`/`practice_deck_card`/
    `adopted_at`; без него 4′ ломает browse/adopt. **Навесное:** свап wire `CardDto→SenseDto`,
    Personal-deck CRUD, claim-UI, reorder, витрина каталога.
* **`practice`/SRS** — состояние по `sense_id`; формы `${sense_id}:form:…` из `Sense.irregularForms`.
  **Деталь карты** (rich detail hub) — read-модель тянет полный `Sense` (blob) + рёбра через
  `LexicalGraphReader` + SRS. **TTS-кэш** (`tts_audio_cache`) адресуется текстом+голосом,
  **verification-кэш** — запросом; **ни один не привязан к `sense_id`/card-id** — смена `sense_id`
  их не трогает (мигрировать нечего).
* **`verification`** — две узкие роли, обе развязаны со store: (1) **pre-AI grounding**
  (free-dictionary/datamuse/sensee-curated): `verify(term) → EnrichmentGroundingBuilder →
  EnrichmentRequest.grounding → промпт`; (2) **post-AI example-quality** — тихий, не блокирующий
  отсев слабых `contextualApplications` перед save, который **никогда не опустошает** список
  (оставляет лучший пример). Verifier не читает/пишет канон. Снесена только snapshot-оркестрация.
* **Навесное за портами (impl в `lexicon/data`, контракт в `lexicon/domain`):**
  - **Граф связей** — `LexicalGraphReader` деривит рёбра из blob + `lemma_key` на чтении, **без
    таблицы рёбер**. Форма ответа — обобщённое ребро `(from, to, kind, rank)`,
    `kind ∈ participates_lemma|sublemma|family|synonym|antonym|related|…`. `lemma_key` = **head-лемма**;
    обратное участие = скан корпуса (для личного словаря ок; participation-индекс — только под скорость).
  - **Поиск** — `SearchPort`/`SenseQuery` (headword/translation/lemma + фильтры
    cefr/unitType/origin/deck), минимальная in-memory impl над проекционными колонками; авторитетный
    (FTS/семантика) — backend за тем же портом.
  - **Embedding** — `EmbeddingPort.embed()` на save (один путь), `findSimilar` brute-force cosine.
  - **Наборы — UX поверх ядровой модели.** Схема `deck`/`deck_membership` (`sense_id`, many-to-many,
    `position`) + `subscribe`-флаг + service-ingest-as-decks — **в ядре (4′)**. Здесь (навесное):
    Personal-deck CRUD (editing-UX → `EB-9`), claim-UI (добавить подмножество = `claim` →
    Personal-копии), unsubscribe-UX. Членство Personal-набора — **только Personal-`sense_id`**
    (сервисное — через `claim`), чтобы отписка не оставляла висячих ссылок; смысл в N наборах = N строк.
  - **Сессия практики (эфемерная)** — `buildSession(selection)` в `practice/domain` над списком
    `sense_id` (due / подмножество деки / **несколько дек вперемешку** / ручной набор). Ничего не
    персистит (опц. дескриптор для resume), кросс-origin ок. SRS — **на смысл** (`sense_id`), один на
    все наборы и сессии: практика в любом месте двигает прогресс везде (учим слово, не
    карточку-в-списке). `EB-3`.

## 5. Связи (схемы)

Легенда: `▣` — durable-таблица; «НЕ ПОДКЛЮЧЕНО» — есть в коде/тестах/DI, но вне живого флоу;
`〔навесное〕` — порт + тупейшая impl, после ядра.

### 5.1 Текущие связи (as-is)

```
ЖИВОЙ ФЛОУ ЗАХВАТА
  Пользователь ─term─► VocabularyCaptureLogic ─inject─► SuggestVocabularyMeaningsUseCase (facade, LEGACY)
                                          ▼ AiEnrichmentClient.enrich(term)  ◄── БЕЗ evidence
                            toMeaningCandidates → SenseCandidate → выбор/confirm
                            DurableVocabularyRepository ─► ▣ lexical_entry.senses_json   ◄ store #1
                            (id производен от контента/позиции; правка → SRS расходится)

НЕ ПОДКЛЮЧЕНО  (код + тесты + DI есть, use-case'ы никто не запрашивает)
  SuggestVocabularySensesUseCase / VerifyVocabularySuggestionsUseCase ─► LexicalVerifier ; SenseVerificationSnapshot (UI не читает)

LIBRARY / ВТОРОЙ STORE
  LexiconRepository(read) ─► CapturedCatalogDerivation ─► captured deck (derive-on-read, только Confirmed; id НЕ стабилен)
  CatalogRemoteDataSource ─► CardDto(enrichment=EnrichmentItemV1) ─► EnrichmentResponseMapper ─► ▣ practice_card.sense_json   ◄ store #2
  library ⋈ SRS (ключ = card id)
```

### 5.2 Целевые связи (to-be) — три входа, общий store по `sense_id`

```
КАНОН (одна линия):  EnrichmentItemV1 → EnrichmentSuggestion → Sense ↔ SenseDto

 A: AI-захват                 B: ручной ввод / правка        C: сервисный набор (subscribe | claim)
  term                         Sense (руками) ± completeManualSense   ingest (origin=Service; example-gate)
   │ grounding→enrich за швом   │ правка → id-preserving (+warning)    │ subscribe = зеркало (SRS на Service sense_id)
   │ example-quality (тихо)     │                                     │ claim = копия в Personal (новый sense_id, детач)
   ▼                           │                                      ▼
  EnrichmentSuggestion ─map─► Sense ◄─── editor-state (in-progress Sense + статус + выбор)
                               │
        новый write → WriteIntent(ResolveOrMint|UpdateExisting|ForceMint) ; правка → тот же sense_id ; транзакция
        confirm-gate: translation непустой И ≥1 contextualApplication
                                               ▼
        ┌──────────────── ▣ sense  (владелец: lexicon — единственный store) ─────────────────┐
        │  StoredSense = sense_id PK · status(Draft|Confirmed) · origin · source_ref · cefr ·  │
        │  updated · sense_json(SenseDto)  ; + lemma_key · content_key · unitType (произв.)     │
        │  〔навесное〕 ▣ sense_embedding: sense_id · vector · model_ref · dim  (EmbeddingPort)  │
        │  origin-precedence: синк Service НЕ перетирает Personal; с наборами польз. не сравн.   │
        └───────────────┬───────────────────────────┬───────────────────────────┬─────────────┘
            status=Draft │                status=Confirmed                       │ 〔навесное〕
                         ▼                           ▼                            ▼
            library: очередь «незавершённое»  library: Card/Deck/Lemma     similar-sense выбор
            (read, без SRS)                   = проекция ⋈ SRS по sense_id  (EmbeddingPort, не гейт)
                                              practice/SRS (ключ = sense_id; формы ${sense_id}:form:…)
                                              〔навесное〕 SearchPort / LexicalGraphReader / deck-UX/claim
```

### 5.3 Ownership хранения

```
AS-IS                                        TO-BE
  vocabulary ─► lexical_entry.senses_json      lexicon   ─► ▣ sense  (canonical, draft+confirmed)  ◄ один store
  library    ─► practice_card.sense_json                   ─► 〔навесное〕 ▣ sense_embedding (вектор)
  └─ две durable-копии; SRS по контент/поз.-id  vocabulary = authoring/edit-writer (3 режима, один editor-state)
  service: CardDto(EnrichmentItemV1)           library    = projection: Confirmed→практика, Draft→очередь
                                               practice   = SRS by sense_id (стабильно при правках)
                                               service    = subscribe (зеркало, origin=Service) | claim (копия→Personal, детач)
```

## 6. Фазы

Ядро = **0 → 1 → 2 → 3 → 4′** (четыре ноги §1). Навесное — после ядра, по одному (§6 «После
ядра»). Порядок ядра: 0 и 1 — независимо и сразу; 2 — независимо; 3 — после 1–2; 4′ — атомарный
cutover стора. Поведенческие изменения сопровождаются тестом в том же изменении (тест падает без
изменения). **Реорг пакетов** — последним шагом фазы, что доводит модуль.

> **Статус реализации (2026-06-05).** Фазы 0–3 закоммичены; Фаза 4′ — под-коммиты 1–4
> (sense-store фундамент → practice-лист → атомарный cutover → claim) реализованы и зелёные.
> Остаются: под-коммит 6 (этот docs/ADR/arc42/LikeC4 sync — описывает уже реализованное
> состояние 1–4), 5 (чистка dead-input в `verification/core`), 7 (реорг пакетов). Текст ниже —
> рабочий план целевого состояния; он намеренно не переписан под «уже сделано» и удаляется по
> завершении консолидации.

### Фаза 0 — синхронизация docs / ADR / LikeC4: только снятие over-claim

Цель: убрать **фактическую неточность** о текущем коде — доки описывают post-AI verification
snapshots как **реализованные** (нарушение `docs/AGENTS.md`), хотя это неподключённый seam.
**Описание целевой структуры** здесь **не пишем** — оно ложится в коммиты фаз ядра. Новые ADR
не создаём.

Действия: вместо списка файлов — **grep-свип** по `docs/` (`snapshot` / «verification snapshots» /
«два вызова» / post-AI), правим **все** найденные over-claim-точки: подтверждённо `docs/README.md`,
ADR-0007, `docs/c4/views.c4`/`model.c4`, `arc42/sections/03|05|06`, `08_concepts`,
`vocabulary-capture.feature`, `profile.feature` (список неисчерпывающий — канон = свип).
Описываем **только текущую реальность**: verification-seam есть, но живой capture/manual идут через
bare `enrich` — **ни grounding, ни snapshots не подключены**. Целевые «две узкие роли» здесь **не**
описываем (станут правдой в Фазе 2 — тогда и попадут в доки; до тех пор — «seam готов, не подключён»
/ `@planned`). Проверка: `npx likec4 validate docs/c4`. Скиллы:
`architecture-docs-sync`, `likec4-architecture-model-review`.

### Фаза 1 — снос legacy + стабилизация моделей (независимо)

Цель: один линейный authoring-путь, одна терминология, **замороженная форма `Sense`/`SenseDto`**
до ядра.

* Удалить `Meaning.kt` и весь `*Meaning*`-API; вызовы → `Sense`-именованные.
* Удалить facade `SuggestVocabularyMeaningsUseCase.kt`; `VocabularyCaptureLogic` инжектит
  единственный `SuggestVocabularySensesUseCase`; удалить `enrichAndMap`.
* **Удалить `EnrichedSense`** (`toEnrichedSense`/`toEnrichedSenses`/`identityFingerprint`); маппер
  отдаёт `Sense` (`toSense`/`toSenses`).
* **Удалить доменный `SenseCandidate`**; use-case маппит `EnrichmentResult` в несохранённый
  editor-`Sense` state (seam канон не производит — `shared/ai/AGENTS.md`). **Свернуть editor-типы
  `SenseCandidate`(presentation) + `ManualSense` в ОДИН** presentation-state тип (in-progress
  `Sense` + источник AI/manual + статус + выбор); выбор по `content_key`. Переформулировать
  `ai/AGENTS.md`: «кандидат = несохранённый `Sense` в редакторе; подтверждение = сохранение».
* **`cefr` first-class типизированный**: `CefrLevel` enum в `lexicon.domain`; `Sense.cefr:
  CefrLevel?`; `SenseDto.cefr: String?`; `toSense` читает `extensions[CefrEnrichmentExtension.KEY]`;
  cefr **не** в `content_key`. Тест: round-trip wire→`Sense`→`SenseDto`→`Sense`; cefr не меняет
  `content_key`.
* **Confirm-gate как чистое доменное правило (без durable draft).** Per-sense `Draft|Confirmed`
  появляется только в 4′; здесь — функция-инвариант на замороженном `Sense`: конфёрмабелен ⇔ непустой
  `translation` **и** `contextualApplications.isNotEmpty()` + тесты. Durable draft-payload (`""` при
  `status=Draft`) и enforcement на write-границе — в 4′, где есть per-sense `status`.
* **Реорг пакетов** (в конце фазы): `vocabulary-editor/domain` → `model/`/`usecase/`/`mapping/`;
  `presentation/impl` → `screen/`/`component/`/`card/`.
* **DoD:** живой capture-путь использует один `SuggestVocabularySensesUseCase`; `*Meaning*` /
  `EnrichedSense` / доменный `SenseCandidate` отсутствуют в коде; форма `Sense`/`SenseDto`
  **заморожена** (контракт для 4′); `cefr` round-trips; confirm с пустым переводом или без примера падает.

Проверка: `.\gradlew.bat :shared:feature:vocabulary-editor:domain:check`,
`:shared:feature:vocabulary-editor:presentation:impl:check`, `:shared:lexicon:enrichment:check`.
После правки `ai/AGENTS.md`: `python3 scripts/agents/validate-agent-instructions.py`.

### Фаза 2 — verification → grounding + example-quality (тихий фильтр)

Цель: подключить две узкие роли, удалить snapshot-оркестрацию, развести нэйминг.

Подключить:

* `SuggestVocabularySensesUseCase` зовёт `verifier.verify(query)` **без** `senseHints`/
  `examplesToValidate`; `EnrichmentGroundingBuilder` → `EnrichmentRequest.grounding`. Сбой
  verifier не блокирует suggestion (graceful degrade).
* **Провести живой `completeManualWithAssistant` через тот же grounding-enabled use-case**, что и
  capture (сейчас `enrichAndMap`-путь идёт без grounding) — иначе ручное обогащение останется без него.
* **Example-quality — тихий фильтр**: один опциональный вызов `ExampleQualityChecker` над
  `Sense.contextualApplications` перед save; отсев/флаг слабых **без UI-сигнала**, **никогда не
  опустошает** список (оставляет лучший пример — инвариант «≥1 пример»).
* **Rename по роли:** `EnrichmentEvidence → EnrichmentGrounding`, `EnrichmentEvidenceBuilder →
  EnrichmentGroundingBuilder`, `EvidenceModifier → GroundingModifier`. «evidence» остаётся за
  verification.
* **Маршрутизация grounding — только LLM** (curated самодостаточен, grounding игнорирует; по
  умолчанию, фиксируем явно). Тест: grounding доезжает до prompt на реально исполняемом пути.

Удалить (только snapshot-оркестрацию, **не** example-quality):

* `VerifyVocabularySuggestionsUseCase`, `SenseVerificationSnapshot` (+ проекции/findings/scope),
  поле верификации в editor-state.
* В `verification/core`: `LexicalVerificationQuery.senseHints`/`examplesToValidate`; в `SenseMapping`
  оставить только `DictionarySenseSummary`; `SentenceHint` (если только для AI-self-validation).
* `GrammarUnitVerificationHints.kt` — упростить/удалить.
* **Сохранить**: `ExampleQualityChecker`, модуль `shared/verification/languagetool` как опциональный
  single-checker — снять только **многоканальный fan-out**. `SourceCatalog`-регистрацию, fail-closed
  license (unknown source → restrictive `LicensePolicy` в `RoutingLexicalVerifier`) и
  `SourceCatalogCompletenessTest` **сохраняем** (ADR-007/009 — это безопасность, не оркестрация).
* **Реорг пакетов** (в конце фазы): `verification/core` → `contract/`/`grounding/`/`hierarchy/`.
* **DoD:** grounding доезжает до промпта на реально исполняемом пути (capture **и**
  `completeManualWithAssistant`); snapshot-оркестрация удалена; example-quality — один тихий вызов,
  не опустошает список; нэйминг `Grounding` разведён с verification-`evidence`.

Проверка: `.\gradlew.bat :shared:verification:integration:check`,
`:shared:feature:vocabulary-editor:domain:check`, `konsistCheck`.

### Фаза 3 — граничные зеркала (узкий объём)

Цель: пометить намеренные зеркала, дедупить только безопасное. Направление: `ai.core`/
`verification.core` — нулевые листья; `lexicon.domain → grammar.domain`;
`lexicon.enrichment → ai.core+lexicon.domain+grammar.domain`.

* `EnrichmentResponseV1`/`EnrichmentItemV1` (wire) и `*Dto` (persist) — **оставить** как
  осознанные зеркала; KDoc «intentional boundary mirror». (Embedding **не** добавляем на wire —
  один путь через `EmbeddingPort`, §«После ядра».)
* `AlignmentChunk`/`UnitComponentFact` дедупить **только** при заведении одного нейтрального листа
  (для `UnitComponentFact` кандидат — `grammar/domain`); иначе оставить + KDoc о причине.
* **Снять `EnrichmentItemV1` с service-пути на уровне типов** — подготовка к свапу wire (сама
  замена `CardDto→SenseDto` и удаление зависимостей `library/data` — навесное после ядра).
* `EnrichmentExample ↔ ContextualApplication` — не трогать.
* **Реорг пакетов** `ai.core` → `contract/`/`request/`/`wire/`/`grounding/`/`model/` — в конце фазы.
* **DoD:** намеренные зеркала помечены KDoc «intentional boundary mirror»; дедуп типов — только при
  заведении одного нейтрального листа; `EnrichmentItemV1` на service-пути помечен к снятию (реальная
  замена `CardDto→SenseDto` без него недостижима на уровне типов — это навесное); embedding на wire не добавлен.

Проверка: `.\gradlew.bat :shared:ai:core:check`, `:shared:lexicon:serialization:check`,
`:shared:verification:core:check`, `konsistCheck`.

### Фаза 4′ — атомарный cutover стора с `sense_id` (ядро)

Цель: один источник истины; идентичность и SRS — на стабильном `sense_id`. **Это
correctness-единица**: write-сторона, read-проекция и SRS-ре-кей переезжают **вместе**, старые
сторы сносятся в этом же изменении (никакого сломанного промежуточного состояния).

* `lexicon/domain`: `SenseReadRepository` + `SenseWriteRepository` (`upsert`/`WriteIntent`, без `claim`;
  один impl, отдаёт `StoredSense`), value class `SenseId`, `StoredSense`, `deriveSenseContentKey`,
  lemma-резолвер, `CefrLevel` — чистые, ноль новых зависимостей.
* `lexicon/database-schema` (`.sq`) + `lexicon/data` (impl), агрегировать в `SenseeDatabase`
  (ADR-002): таблица `sense` (`sense_id TEXT PK`, `status`, `sense_json`, `lemma_key`,
  `content_key` index, `origin`, `source_ref`, `cefr`, `unitType`, `updated`), `CHECK(origin !=
  'Service' OR source_ref IS NOT NULL)` и partial unique index
  `CREATE UNIQUE INDEX sense_service_source_ref ON sense(source_ref) WHERE origin = 'Service'`. Impl
  (один на оба контракта): генерация `SenseId` (Personal `Uuid` / Service namespaced из `source_ref`,
  не сырой), `upsert` с `origin`-precedence, `WriteIntent` (`ResolveOrMint`, `UpdateExisting(sense_id)`,
  `ForceMint`), `resolve+upsert` в одной транзакции только для `ResolveOrMint`, id-preserving правка,
  `observe`, чтение по `lemma_key`/`status`. `updated` — через clock-шов в `lexicon/data`. Konsist по
  слоям: `lexicon/data → shared/database` ок, feature/SRS — нет; `claim` здесь **не** живёт.
* Gradle/C4 cutover для `lexicon/database-schema`: добавить `include` для `:shared:lexicon:database-schema`
  и `:shared:lexicon:data`; подключить schema dependency в `shared/database/build.gradle.kts`
  (`evaluationDependsOn` + `sqldelight dependency`); обновить LikeC4 model/views, чтобы canonical
  `sense` schema принадлежала `lexicon`, а `vocabulary-editor` остался write workflow.
* `vocabulary-editor/data`: запись draft/confirmed/правки в `sense`; **confirm-gate enforced на
  write-границе** (Draft→Confirmed: непустой `translation` + ≥1 пример), per-sense `status`.
* `library`/`practice`: проекция `Card`/`Deck`/`Lemma` над (`sense`-строка + SRS); **SRS ре-кей на
  `sense_id`** (формы `${sense_id}:form:…`); private `captured:…`-id убрать. **Минимальная deck-модель
  в этом же cutover:** `deck`/`deck_membership(position)` + флаг `subscribed` (преемники
  `practice_deck`/`practice_deck_card`/`adopted_at`), service-ingest **как деки** — чтобы browse/adopt
  не регрессировали. Personal-deck CRUD/claim-UI — навесное.
* `practice/domain`: **разорвать `api(library.domain)`** — `PracticeReviewRepository.submitReview`
  возвращает practice-owned результат (`SrsCardSnapshot`/`ReviewOutcome`) вместо `library.domain.Card`
  (callerы, кому нужен `Card`, ре-проецируют через `library`); `practice/domain` становится листом над
  `srs.core`. Добавить `DuePracticeRepository` (`countDue(now)`, `observeDueCount(now)`); `practice/data`
  реализует поверх `SrsCardStore`/`SrsStorage`.
  `home/presentation:impl` зависит на этот узкий contract и **транзитивно** не видит `library`
  (konsist-ассерт); due-list остаётся в Practice.
* `srs.engine`: добавить SRS-примитив `copySnapshot(from, to)` (default-метод `SrsCardStore` через
  `getCard`/`saveCard`; нужен `SrsCardSnapshot.copy(id=…)`) — для `claim`, без feature-зависимостей.
* **Снести** `lexical_entry.senses_json`, `practice_card`/`practice_deck`/`practice_deck_card`,
  derive-on-read в `CapturedCatalogDerivation`. Service ingest — прежним маппером в `sense`
  (origin=Service) + `deck`/`deck_membership`.
* **`claim`-оркестратор + `DatabaseTransactionRunner`:** нейтральный tx-runner **в `shared/database`**
  (поверх `SenseeDatabase.transactionWithResult`), который шарят data-impl; `claim` живёт в `library/data`
  и композирует lexicon `upsert(ForceMint)` + `SrsCardStore.copySnapshot` (`srs.engine`, **без** ребра на
  `practice`) + deck membership в одной транзакции (внутренние ops свою tx не открывают, энлистятся;
  `SrsTransactionRunner` делегирует тому же DB-transaction). `claim` — **всегда `ForceMint`** (детач);
  lexicon-write отдаёт только Personal-копию контента — `claim` целиком вне lexicon.
* Konsist: `lexicon` без восходящих/feature-зависимостей. Миграций нет — dev-reset.
* **Реорг пакетов — отдельным коммитом ПОСЛЕ** correctness-коммита cutover, чтобы diff переезда
  читался без шума переименований.

Проверка: `.\gradlew.bat :shared:lexicon:domain:check :shared:lexicon:data:check`,
`:shared:database:check`, `:shared:feature:vocabulary-editor:data:check`,
`:shared:feature:library:data:check`, `:shared:srs:engine:check` (новый `copySnapshot`; + `:shared:srs:core:check`,
если под `SrsCardSnapshot.copy(id=…)` заводится helper), `:shared:feature:practice:domain:check`,
`:shared:feature:practice:data:check`, `:shared:feature:practice:presentation:api:check`,
`:shared:feature:practice:presentation:impl:check`, `npx likec4 validate docs/c4`, затем root `check`.
Тесты:
* **`sense_id`-стабильность**: реордер/удаление смысла, правка `headLemma`, re-capture того же
  `content_key`, term-collision — сохраняют `sense_id`.
* **SRS-сохранность** (теперь тестируемо здесь, рядом с ре-кеем): те же сценарии не сиротят SRS.
  **NB:** «edit translation → SRS kept» проходит **уже сегодня** (append-only `mergeSenses`) →
  как регрессия невалиден; таргетить index-shift / lemma-key пути выше.
* Синк Service не перетирает Personal; `ResolveOrMint` origin-scoped + tie-break `max(updated)`
  при дубле `content_key`; `ForceMint` не re-resolve'ится; `UpdateExisting` сохраняет `sense_id`;
  Service row без `source_ref` невозможна, а дубль `source_ref` отклоняется partial unique index;
  правка — id-preserving (+warning при коллизии); confirm-gate (translation + ≥1 пример);
  `DuePracticeRepository` отдаёт due-count без зависимости Home на `library`.
* **`copySnapshot`-примитив** (`srs.engine`, unit-тест в `commonTest` или через `test-kit`
  `InMemorySrsStorage`): копия снапшота на новый `sense_id`, исходный не тронут, отсутствующий source =
  no-op.
* **Deck/subscribe регрессия (deck-модель теперь в cutover):** service-набор browse → subscribe →
  practice работает после 4′ (зеркало по `source_ref`, SRS на Service-`sense_id`); ре-синк перетирает
  контент, SRS цел; `claim` подмножества даёт Personal-копии с `copySnapshot`.
* **Связи (konsist/структурные):** `home/presentation:impl` транзитивно не видит `library.domain`;
  `lexicon/domain` без persistence/SRS/feature, `lexicon/data` без feature/SRS; `claim` отсутствует в
  `SenseWriteRepository`. `claim`-атомарность: сбой `copySnapshot`/membership откатывает контент-копию
  (один `DatabaseTransactionRunner`). `SenseId`: Service-id без `:` → form-key `${sense_id}:form:`
  парсится однозначно.

### После ядра — навесное за портами (каждое = отдельное изменение / `EB`)

Ноги стоят; ниже — отрезаемые аксессуары, каждый порт + тупейшая impl, в любом порядке:

* **Draft-очередь UI** — read-проекция `status=Draft` (read-only, без SRS) в Library.
* **Свап service-wire — DONE.** `CardDto → SenseDto` (`toDomain → upsert origin=Service`); плоский
  enrichment-трюк убран (`CardDtoSerializer` удалён, карта = `{id, lemmaId, sense: SenseDto}`);
  `library/data` потерял `ai.core` + `lexicon.enrichment`; фикстуры ре-кеены; cefr теперь течёт
  через `SenseDto.cefr` — gap каталога закрыт.
* **Наборы — UX поверх ядровой схемы.** `deck`/`deck_membership` + `subscribe` уже в ядре (4′).
  Здесь: `DeckRepository` Personal CRUD (создать/переименовать/наполнить/reorder), claim-UI (`claim`
  подмножества → Personal-копии + `copySnapshot` SRS), unsubscribe-UX. `EB-9`.
* **Поиск** — `SearchPort`/`SenseQuery`, in-memory над проекционными колонками.
* **Граф** — `LexicalGraphReader` (рёбра из blob + `lemma_key` на чтении, без таблицы рёбер).
* **Embedding similar-sense выбор (создаёт схему `sense_embedding`)** — `EmbeddingPort.embed()` **eager
  на save синхронно онлайн** (в пределах таймаут-бюджета): near-match → **выбор пользователя** (§2), не
  пассивный баннер.
  **Офлайн/таймаут/сбой** → строка `sense` коммитится первой без вектора, offline-save **не падает**;
  отсутствие строки в `sense_embedding` = «не эмбеднуто» → бэкфилл ленивым ре-эмбеддингом, отложенный
  выбор в «возможные дубли». Вектор провайдерный (`openai/text-embedding-3-small`, `dimensions=512`
  явно, ~2 КБ/смысл; `model_ref`/`dim` в строке). `findSimilar` brute-force cosine **только по уже
  эмбеднутым**, гард `(model_ref, dim)`, ленивый ре-эмбеддинг при смене модели; `dismissed-pairs`,
  чтобы не переспрашивать про разведённую пару. Выбор **никогда не гейтит save**. `ON DELETE CASCADE`
  на `sense_embedding`.

## 7. Влияние на ADR (редактируем, не создаём)

| ADR | Что меняем | Где |
|---|---|---|
| ADR-001 | единый sense-store (draft+confirmed); `library` — проекция (Confirmed-практика + Draft-очередь); сервисные наборы — `subscribe` (зеркало) / `claim` (детач-копия); **deck-модель + `subscribe` — в ядре** (преемник `practice_deck`/`adopted_at`), CRUD/claim-UI — после ядра | 4′, после ядра |
| ADR-002 | `lexicon` владеет канонической sense-схемой: `lexicon/database-schema` (`.sq`) + `lexicon/data` (impl), агрегация в `shared/database` (как `verification/database-schema`); konsist по слоям (`lexicon/domain` чистый, `lexicon/data → shared/database` ок, feature/SRS нет); Service-identity защищена `CHECK` + partial unique index по `source_ref`; `library` сохраняет deck-схему (`deck`/`deck_membership`) без `sense`-контента (membership ссылается на `sense_id`); `claim` — оркестратор в `library/data` поверх нейтрального `DatabaseTransactionRunner`; `sense_embedding` — навесное | 4′ |
| ADR-004 | due-count для Home — узкий `practice/domain` contract поверх SRS по `sense_id`; в 4′ снимается ребро `practice/domain → library/domain` (`submitReview` → practice-owned результат, не `Card`), Home транзитивно не видит `library` (konsist-ассерт) | 4′ |
| ADR-005/0006 | versioned wire сохраняется для capture; каталог перестаёт быть потребителем (`SenseDto`, навесное); embedding **не** на wire (один путь `EmbeddingPort`) | 3, после ядра |
| ADR-007 | scope = две узкие роли (pre-AI grounding + post-AI example-quality тихий фильтр); post-AI **snapshot-оркестрация** удалена, `ExampleQualityChecker`/`languagetool` + **fail-closed license / `SourceCatalog`-полнота сохранены**; rename `Grounding` | 0, 2 |
| ADR-008 | **переписываем** (план отменяет ядро ADR): `sense_id` = идентичность/якорь SRS; form-identity lemma-stable → `${sense_id}:form:`; `WriteIntent` разводит `ResolveOrMint`/`UpdateExisting`/`ForceMint`; `confirmSenses`-merge и `updateConfirmedEntry` delete+reindex упразднены. **Адресовать собственное отклонение ADR-008 отдельной таблицы:** новый `sense`-стор **заменяет** `lexical_entry` (один source-of-truth, не «рядом» → возражение «удваивает» снято), миграции нет (dev-reset → возражение «миграция» снято) | 4′ |
| ADR-009 | клиентский кэш — mock-backed продакшн; example-quality сетевой путь (license-policy) сохраняется; bypass для example-validating queries снять | 2 |

## 8. Влияние на backlog

* `EB-6` — **пере-скоуплен** (уже в дереве: `EB-6-abandoned-draft-cleanup`). Бинарь
  `Draft|Confirmed` + `sense_id` + `updated` закрывают draft-очередь/resume/SRS-гейт; чистка по
  `updated` — данные закладываем (Фаза 4′), реализация чистки позже.
* `EB-7` (deep-link `Editor`) — блокер = **навигационная инфра** (`DefaultVocabularyEditorSectionComponent`
  на `MutableStateFlow`, без Decompose-стека → не `WebNavigationOwner`), **не** идентичность. После
  растворения entry цель ссылки = **`lemma_key`-группа** (открыть слово целиком, все смыслы леммы;
  совпадает с lemma-контекстом §4), не один `sense_id`; прямой `sense_id`-уровень — опц. двухуровневый URL позже.
* `EB-8` (capture wizard, considering) — ортогонально.
* `EB-3` (эфемерная сессия) — **разблокируется ядром**: сессия = список `sense_id` (due/подмножество/
  несколько дек вперемешку), per-sense SRS общий на наборы/сессии. Доработок модели не требует.
* `EB-5` — ортогонален.
* `EB-9`..`EB-12` — **уже созданы** в `docs/backlog/` (держать в синхроне, не «заводить» заново):
  `EB-9` editing-UX Personal-наборов (включая `claim` частей сервисного набора); `EB-10` расширение
  языковых пар (`considering`); `EB-11` посекционное обогащение; `EB-12` — **переформулирован**:
  правка подписанного/Service-смысла = `claim` (завладеть) + правка; копия Service→Personal клонирует
  SRS если был; `derived_from` снят (детач по построению, не нужен).
* **Кандидаты на новые `EB`** (завести скиллом `backlog-maintenance`, когда дойдём): авто-переводчик
  предзаполнения `translation` до enrichment; example-**sourcing** через verifier (checker как
  источник примеров, не только валидатор).

## 9. Риски и порядок

* Фазы 0–3 — низкий риск (снос неактивного/легаси, docs, типы, cefr, rename, грань зеркал). Делать
  первыми; форму `Sense`/`SenseDto` заморозить в Фазе 1.
* Фаза 2 — grounding на curated-пути игнорируется (зафиксировано: только LLM); example-quality —
  тихий, не блокирующий. mock-backed продакшн.
* **Фаза 4′ — основная стоимость и единственная correctness-критичная** (теперь включает deck-модель +
  subscribe, см. #2): write/read/SRS/deck переезжают как один логический cutover. В одной ветке
  «атомарность» = коммитить cutover **связно** (можно под-коммитами: разрыв `practice→library` → store →
  проекции/SRS-рекей → deck/subscribe → `claim`-оркестратор), не вперемешку с несвязным. Ключевой
  инвариант (konsist по слоям): **`lexicon/domain` без persistence/SRS/feature; `lexicon/data` —
  `shared/database` ок, feature/SRS нет**; SRS-джойн в `library`; `claim` — оркестратор в `library/data`,
  не метод lexicon-write. Без миграций (dev-reset); cutover стирает локальную историю SRS.
* Навесное (после ядра) — низкий риск и независимо: каждый аксессуар отрезаем, не трогает ноги.
  Embedding — PoC: объёмы не оптимизируем (brute-force, full-blob), пересмотр при крупном корпусе;
  near-dup — **выбор пользователя** (синхронно онлайн / отложенно офлайн), **не гейтит save**.
* **Одна ветка на всю консолидацию** (local-only, без per-фаза веток/PR). Фазы/шаги = последовательные
  коммиты внутри неё по `docs/engineering/commits.md` — границы для review/bisect, **не** для
  безопасности мёрджа (всё мёрджится разом → «сломанного состояния в main между мёрджами» нет по
  построению). Коммиты не смешивают снос легаси, схему, docs и реорг пакетов; каждый желательно держать
  собираемым (для bisect).

## 10. Открытые вопросы (остаточные — эмпирика/тюнинг)

Архитектура зафиксирована в §1/§2/§4 (табурет, `StoredSense`, value class `SenseId` + namespaced
Service-id, draft-payload + confirm-gate, `WriteIntent` (`ResolveOrMint`/`UpdateExisting`/`ForceMint`) +
origin-scoped resolve + tie-break `max(updated)` + Service `CHECK`/partial unique index по `source_ref`,
id-preserving правка + транзакция, adoption live-mirror, `claim`-оркестратор вне lexicon поверх
нейтрального `DatabaseTransactionRunner`, `lexicon/database-schema` (konsist по слоям),
`DuePracticeRepository` + разрыв `practice→library`, инвариант контекстного примера, example-quality
тихий фильтр, embedding один путь best-effort, граф без таблицы рёбер).
Осталось эмпирическое:

* **Порог cosine и `dim`** (embedding): старт `text-embedding-3-small@512`, порог ~0.85 — тюнить на
  реальном словаре; механика ленивого ре-эмбеддинга при смене `model_ref` — описана, отлаживается.
* **Таймаут-бюджет eager-embed на save** — потолок провайдерного вызова на add-пути (старт ~1–1.5 с),
  за которым детерминированно уходим в отложенную ветку «возможные дубли»; иначе «eager» станет
  блокирующим на медленной сети. `findSimilar` сравнивает только с уже эмбеднутыми.
* **Порог example-quality** тихого фильтра — тюнить эмпирически.
* **Триггер participation-индекса** для обратной навигации графа (`across` → производные): держим
  derive-on-read скан; вводить индекс/таблицу рёбер, только если упрётся в скорость.
* **Гранулярность `lemma_key`** в UI: одноуровневая группировка vs read-time двухуровневая
  (`head → sub-lemma → sense`) — решить по UX при decks/поиске.
* **Сервисные микро-UX:** при отписке — предложить `claim` практикованных (иначе SRS уходит с
  зеркалом); upstream убрал смысл с накопленным SRS — `claim` vs дроп; `claim` смысла, уже бывшего в
  подписанном наборе — мягкий dup-намёк по `content_key`. Все три — навесное, на ядро не влияют.
* **Promoted-колонки** (язык-пара/`frequency`) — только под реальную фичу (`EB-10`).
