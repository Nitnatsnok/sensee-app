# Документация клиента Sensee

Этот каталог содержит только документацию клиентской части `Sensee`.

## Что здесь есть

- `arc42/sections/` — основной архитектурный reference.
- `domain-information-model.adoc` — product-first reference по доменным сущностям и их связям.
- `pos-and-forms.adoc` — единый источник таксономии и инварианта `GrammarCategory` ↔ `GrammarForm`.
- `adr/` — коллекция `Architecture Decision Record` для architecturally significant решений.
- `evolution-backlog.adoc` — живой список отложенных сквозных разрывов (политика — в нём же).
- `c4/` — канонические исходники архитектурных схем.
- `scenarios/` — поведенческие сценарии в формате `Gherkin` (русская локализация) для реализованных и планируемых user-flow.

## Текущее состояние

На момент обновления документации репозиторий содержит:

- общий `shared/app-shell`, запускаемый на Android, iOS, Desktop JVM, JS и Wasm;
- реализованный пользовательский срез: запуск, навигация, practice (deck/home/card detail с fixture-backed загрузкой, локальным сохранением, SRS-review), library catalog + adoption, profile AI/TTS-настройки и capture-вертикаль (ввод → enrich через AI-шов → мультивыбор + ручной смысл → durable-подтверждение); единственный нереализованный раздел — `Home` (рендерит `UnimplementedScreen`);
- переиспользуемый `shared/ui/learning-deck` с flip, swipe, keyboard support и undo;
- отдельные модули `shared/srs/*`, `shared/database`, `shared/core/network`, `shared/core/mock-backend`, `shared/ai/*` и `shared/grammar/domain`, участвующие в текущих срезах;
- `shared/core/observability`, который существует как подготовленный foundation-модуль.

## С чего читать

Если нужен только быстрый вход:

- `arc42/sections/01_introduction_and_goals.adoc`
- `arc42/sections/05_building_block_view.adoc`
- `arc42/sections/06_runtime_view.adoc`
- `arc42/sections/07_deployment_view.adoc`

Если нужен полный архитектурный reference:

- `arc42/sections/03_context_and_scope.adoc`
- `arc42/sections/04_solution_strategy.adoc`
- `arc42/sections/08_concepts.adoc`
- `domain-information-model.adoc`
- `arc42/sections/09_architecture_decisions.adoc`
- `adr/index.adoc`
- `arc42/sections/10_quality_requirements.adoc`
- `arc42/sections/11_technical_risks.adoc`

## Как смотреть схемы `C4`

Нужен `Node.js`, чтобы был доступен `npx`.

Из корня репозитория:

### Просмотр локально

macOS / Linux:

```bash
npx likec4 start docs/c4
```

Windows:

```powershell
npx likec4 start docs/c4
```

Команда поднимет локальный viewer для схем.

Если уже находитесь в `docs/c4/`, можно запускать так:

```bash
npx likec4 start .
```

### Проверка модели

macOS / Linux:

```bash
npx likec4 validate docs/c4
```

Windows:

```powershell
npx likec4 validate docs/c4
```

### Статическая сборка схем

macOS / Linux:

```bash
npx likec4 build docs/c4 -o docs/c4/dist
```

Windows:

```powershell
npx likec4 build docs/c4 -o docs/c4/dist
```

## Какие view сейчас канонические

- `index`
- `capability_map`
- `shared_modules`
- `current_learning_slice`
- `reviewer_system_context`
- `reviewer_container_overview`
- `reviewer_component_slices`
- `database_aggregation`
- `prepared_modules`
- `deployment_targets`
- `startup_flow`
- `deck_interaction_flow`
- `vocabulary_capture_flow`
- `planned_card_derivation_flow`
- `planned_library_curation_flow`
- `primary_navigation_shell`

## Важные правила

- `LikeC4`-исходники в `docs/c4/*.c4` — канонический источник диаграмм.
- Текст `arc42` должен синхронизироваться с `LikeC4` и с реальным кодом.
- Документация описывает сначала фактическое состояние клиента, а не только целевую архитектуру.
- Подготовленные модули не должны описываться как часть `current_learning_slice`, если этого ещё нет в коде.
- `reviewer_*` view — вспомогательные C4-проекции для внешнего ревью. Они не заменяют
  module/capability-first подход и не вводят новую архитектурную декомпозицию.
- Статические экспорты схем не хранятся в документации; при необходимости собирайте их
  во временный каталог или в build-output, а не коммитьте `docs/c4/dist`.
