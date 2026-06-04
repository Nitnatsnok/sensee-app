# Документация клиента Sensee

Этот каталог содержит документацию клиентской части `Sensee`.

## Что здесь есть

- `arc42/sections/` — основной архитектурный справочник.
- `domain/information-model.adoc` — предметный справочник по доменным сущностям и связям.
- `domain/pos-and-forms.adoc` — единый источник таксономии и инварианта `GrammarCategory` ↔ `GrammarForm`.
- `adr/` — архитектурно значимые решения в формате `Architecture Decision Record`.
- `backlog/` — живой backlog отложенных доработок: один файл на пункт плюс индекс с политикой ведения.
- `c4/` — канонические исходники архитектурных схем.
- `scenarios/` — поведенческие сценарии в формате `Gherkin` для реализованного и планируемого поведения.
- `agents/` — agent-facing каталог skills и setup-заметки для Codex / Claude Code.

## Текущее состояние

Сейчас в коде есть:

- общий `shared/app-shell`, запускаемый на Android, iOS, Desktop JVM, JS и Wasm;
- реализованные разделы: `Practice` (колода, домашний экран, детали карточки), `Library` (каталог и добавление наборов), `Profile` (AI/TTS/App/Learning-настройки) и `Vocabulary Editor` (ввод → AI/curated-подсказки → выбор смыслов → сохранение); граница лексической верификации реализована, DI-bound и подключена к живому capture в двух узких ролях: pre-AI grounding в prompt и silent post-AI фильтр качества примеров;
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
- `domain/information-model.adoc`
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

- `index` — C4 System Context
- `containers` — C4 Container overview (продуктовые зависимости; storage/сеть/DI — в спец. view)
- `capability_map` — продуктовые возможности
- `feature_practice`, `feature_vocabulary`, `feature_library` — C4 Component-проекции разделов
- `integration_seams` — AI / TTS / verification seam-ы
- `database_aggregation` — агрегация SQLDelight-схем и secure storage
- `primary_navigation_shell` — root/primary navigation
- `cross_feature_navigation` — переходы между разделами (реализованные + planned)
- `deployment` — платформенные артефакты
- `startup_flow`
- `deck_interaction_flow`
- `vocabulary_capture_flow`
- `card_derivation_flow`
- `planned_library_curation_flow`

## Важные правила

- `LikeC4`-исходники в `docs/c4/*.c4` — канонический источник диаграмм.
- Текст `arc42` должен синхронизироваться с `LikeC4` и с реальным кодом.
- Документация описывает сначала фактическое состояние клиента, а не только целевую архитектуру.
- Планируемые направления помечаются тегами `#prepared`/`#future` на элементах и связях; planned cross-feature навигация собрана в `cross_feature_navigation`, а не размазана по структурным схемам. `future_backend` показан приглушённым в системном контексте (`index`) и в `feature_library`.
- C4-уровни (Context → Container → Component) выражены вложенностью модели: `index`, `containers` и `feature_*` дают соответствующие уровни без отдельной reviewer-проекции.
- Список выше — кураторский набор явных view. `implicitViews: true` в `likec4.config.json` дополнительно генерирует per-element view, поэтому в `npx likec4 start` и в `likec4` MCP представлений больше.
- Авторитетный перечень элементов, связей и view отдаёт `likec4` MCP (`read-project-summary`) или `npx likec4 ...`; в прозе `arc42` он не дублируется, а ссылается на этот файл и модель.
- Статические экспорты схем не хранятся в документации; при необходимости собирайте их
  во временный каталог или в игнорируемый выходной каталог сборки, например
  `build/likec4/docs-c4`.
