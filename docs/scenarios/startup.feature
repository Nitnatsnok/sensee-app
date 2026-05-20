# language: ru
@implemented @startup
Функция: Запуск клиента и открытие основного shell

  В качестве пользователя я запускаю приложение на любой из поддерживаемых платформ
  (Android, iOS, Desktop JVM, JS, Wasm) и попадаю в основной primary shell
  через общий стартовый экран.

  Источники реализации:
  - `shared/app-shell/.../root/createAppRoot.kt`, `DefaultRootComponent.kt`
  - `shared/app-shell/.../primary/DefaultPrimaryShellComponent.kt`
  - `shared/app-shell/.../primary/PrimaryShellConfigSerializersProvider.kt`
    (root-стек персистится; `StartupConfig`/`PrimaryShellConfig` сериализуемы)
  - `shared/feature/startup/.../DefaultStartupComponent.kt`
  - `shared/grammar/data/.../GrammarLabelsProvider.kt` (app-scoped warm)

  Предыстория:
    Допустим платформенный entrypoint поднял runtime и `PlatformEnvironment`
    И вызвал `createAppRoot(...)`, который собрал `AppGraph` и создал `RootComponent`

  Сценарий: Холодный старт показывает стартовый экран как initial configuration
    Допустим сохранённого стека Decompose нет (холодный старт)
    Когда `DefaultRootComponent` инициализирует stack Decompose
    Тогда `initialConfiguration` равен `StartupConfig`
    И `RootScreen` отображает `StartupComponent` из child stack

  Сценарий: Прогрев словаря — app-scoped, на холодном и тёплом старте
    Когда создаётся `DefaultRootComponent`
    Тогда он один раз запускает `GrammarLabelsProvider.labels()` в своём scope
    И прогрев не привязан к splash: тёплое восстановление, минующее
      `StartupComponent`, всё равно прогревает словарь
    И провайдер мемоизирует успех; повторные вызовы не рефетчат

  Сценарий: Splash держится ровно пока идёт реальный прогрев
    Допустим показан `StartupComponent` (холодный старт)
    Когда `StartupScreen` ждёт `component.awaitReady()`
    Тогда ожидание завершается по готовности `GrammarLabelsProvider.labels()`,
      а не по фиксированной задержке
    И сбой прогрева не блокирует: `awaitReady()` всё равно завершается (EMPTY)
    И затем вызывается `onFinished`

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
    Тогда раздел рендерит общую заглушку дизайн-системы `UnimplementedScreen`
