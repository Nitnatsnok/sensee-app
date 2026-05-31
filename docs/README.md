# Документация клиента Sensee

Этот каталог содержит документацию клиентской части `Sensee`.

## Что здесь есть

- `arc42/sections/` — основной архитектурный справочник.
- `domain-information-model.adoc` — предметный справочник по доменным сущностям и связям.
- `pos-and-forms.adoc` — единый источник таксономии и инварианта `GrammarCategory` ↔ `GrammarForm`.
- `adr/` — архитектурно значимые решения в формате `Architecture Decision Record`.
- `evolution-backlog.adoc` — живой список отложенных сквозных разрывов (политика — в нём же).
- `c4/` — канонические исходники архитектурных схем.
- `scenarios/` — поведенческие сценарии в формате `Gherkin` для реализованного и планируемого поведения.

## Текущее состояние

Сейчас в коде есть:

- общий `shared/app-shell`, запускаемый на Android, iOS, Desktop JVM, JS и Wasm;
- реализованные разделы: `Practice` (колода, домашний экран, детали карточки), `Library` (каталог и добавление наборов), `Profile` (AI/TTS/App/Learning-настройки) и `Vocabulary Editor` (ввод → verification evidence → AI/curated-подсказки → выбор смыслов → verification snapshots → сохранение);
- стартовая навигация и `Home`, который пока показывает `UnimplementedScreen`;
- переиспользуемый `shared/ui/learning-deck` с переворотом, свайпом, клавиатурным управлением и отменой действия;
- отдельные модули `shared/srs/*`, `shared/database`, `shared/core/network`, `shared/core/mock-backend`, `shared/core/observability`, `shared/ai/*` и `shared/grammar/domain`, участвующие в текущих срезах.

## С чего читать

Если нужен только быстрый вход:

- `arc42/sections/01_introduction_and_goals.adoc`
- `arc42/sections/05_building_block_view.adoc`
- `arc42/sections/06_runtime_view.adoc`
- `arc42/sections/07_deployment_view.adoc`

Если нужен полный архитектурный контекст:

- `arc42/sections/03_context_and_scope.adoc`
- `arc42/sections/04_solution_strategy.adoc`
- `arc42/sections/08_concepts.adoc`
- `domain-information-model.adoc`
- `arc42/sections/09_architecture_decisions.adoc`
- `adr/index.adoc`
- `arc42/sections/10_quality_requirements.adoc`
- `arc42/sections/11_technical_risks.adoc`

## Как смотреть схемы `C4`

Нужен `Node.js`, чтобы был доступен `npx`. Команды запускаются из корня репозитория.
В репозитории пока нет root `package.json` / lockfile для LikeC4, поэтому используется
ambient CLI через `npx likec4 ...`; версия инструмента не закреплена проектом.

Локальный просмотр:

```shell
npx likec4 start docs/c4
```

Проверка модели:

```shell
npx likec4 validate docs/c4
```

Статическая сборка схем:

```shell
npx likec4 build docs/c4 -o build/likec4/docs-c4
```

## Канонические представления `LikeC4`

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
- `card_derivation_flow`
- `planned_library_curation_flow`
- `primary_navigation_shell`

## Важные правила

- `LikeC4`-исходники в `docs/c4/*.c4` — канонический источник диаграмм.
- Текст `arc42` должен синхронизироваться с `LikeC4` и с реальным кодом.
- Документация описывает сначала фактическое состояние клиента, а не только целевую архитектуру.
- Подготовленные или планируемые интеграции не должны описываться как часть `current_learning_slice`, если этого ещё нет в коде.
- `reviewer_*` — вспомогательные представления для внешнего ревью; они не меняют
  модульную модель клиента.
- Статические экспорты схем не хранятся в документации; при необходимости собирайте их
  во временный каталог или в игнорируемый выходной каталог сборки, например
  `build/likec4/docs-c4`.
