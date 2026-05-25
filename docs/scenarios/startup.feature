# language: ru
@implemented @startup
Функция: Запуск клиента и открытие основного shell

  В качестве пользователя я запускаю приложение на любой из поддерживаемых платформ
  (Android, iOS, Desktop JVM, JS, Wasm) и попадаю в основную навигационную оболочку
  через общий стартовый экран.

  Источники реализации:
  - `shared/app-shell/.../root/createAppRoot.kt`, `DefaultRootComponent.kt`
  - `shared/app-shell/.../primary/DefaultPrimaryShellComponent.kt`
  - `shared/app-shell/.../primary/PrimaryShellConfigSerializersProvider.kt`
    (root-стек сохраняется; `StartupConfig`/`PrimaryShellConfig` сериализуемы)
  - `shared/feature/startup/.../DefaultStartupComponent.kt`
  - `shared/feature/startup/domain/.../PreloadAppStartupUseCase.kt`
  - `shared/grammar/data/.../CachingGrammarTaxonomyProvider.kt`,
    `CachingGrammarLabelsProvider.kt`, `CachingTaxonomyInvariantsProvider.kt`

  Предыстория:
    Допустим платформенная точка входа инициализировала среду выполнения и передала `PlatformEnvironment`
    И вызвал `createAppRoot(...)`, который собрал `AppGraph` и создал `RootComponent`

  Сценарий: Холодный старт показывает стартовый экран как initial configuration
    Допустим сохранённого стека Decompose нет (холодный старт)
    Когда `DefaultRootComponent` инициализирует stack Decompose
    Тогда `initialConfiguration` равен `StartupConfig`
    И `RootScreen` отображает `StartupComponent` из child stack

  Сценарий: Прогрев таксономии — app-scoped, на холодном и тёплом старте
    Когда создаётся `DefaultRootComponent`
    Тогда он один раз запускает `PreloadAppStartupUseCase` в своём scope
    И use case загружает `GrammarLabelsProvider.labels()` и
      `TaxonomyInvariantsProvider.invariants()`
    И прогрев не привязан к splash: тёплое восстановление, минующее
      `StartupComponent`, всё равно прогревает таксономию времени выполнения
    И общий `CachingGrammarTaxonomyProvider` запоминает успешную загрузку;
      повторные проекции labels/invariants не запрашивают таксономию заново

  Сценарий: Splash держится ровно пока идёт реальный прогрев
    Допустим показан `StartupComponent` (холодный старт)
    Когда `DefaultStartupComponent` запускает `PreloadAppStartupUseCase`
    Тогда экран остаётся в состоянии загрузки до результата preload,
      а не до фиксированной задержки
    И успешный preload переводит состояние в `StartupState.Loaded`
    И затем вызывается `onFinished`

  Сценарий: Сбой startup-прогрева виден и запускается повторно
    Допустим показан `StartupComponent` (холодный старт)
    Когда `PreloadAppStartupUseCase` возвращает сбой labels или invariants
    Тогда состояние становится `StartupState.Failed`
    И пользователь может повторить preload через retry
    И провайдеры не запоминают неуспешную загрузку

  Сценарий: Тёплое восстановление возвращает deep-target без splash
    Допустим сохранён root-стек с `PrimaryShellConfig`, у которого задан `target`
    Когда процесс пересоздаётся и `DefaultRootComponent` инициализирует stack
    Тогда стек восстанавливается из сохранённого состояния через `screenConfigSerializer`
    И `StartupConfig`/splash повторно не проигрывается
    И пользователь возвращается к своему разделу/экрану

  Сценарий: По окончании startup открывается основной shell с `HomeConfig.Home`
    Допустим `StartupComponent` завершил стартовый сценарий
    Когда `StartupComponent` вызывает переданный `onFinished`
    Тогда `DefaultRootComponent.open(HomeConfig.Home)` маршрутизирует запрос в `RootNavigationBranch.PrimaryShell`
    И в stack попадает `PrimaryShellConfig(target = HomeConfig.Home)`
    И `RootScreen` показывает `PrimaryShellComponent`

  Сценарий: Primary shell выбирает home-секцию как initial
    Когда `DefaultPrimaryShellComponent` создаётся из root `PrimaryShellConfig`
    Тогда внутри выбирается `PrimarySectionConfig.HomeSection()` как initial config
    И `PrimaryShellScreen` показывает базовую primary navigation shell с разделами
      | раздел  |
      | `Home`     |
      | `Practice` |
      | `Library`  |
      | `Vocabulary Editor` |
      | `Profile`  |

  Сценарий: Нереализованные разделы показывают `UnimplementedScreen`
    Допустим основной shell открыт
    Когда я перехожу в раздел без реализованного содержания (`Home`)
    Тогда раздел отображает общую заглушку дизайн-системы `UnimplementedScreen`
