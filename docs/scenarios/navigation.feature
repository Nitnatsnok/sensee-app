# language: ru
@implemented @navigation
Функция: Распространение навигации чистыми конфигами

  Источник навигации описывает целевой экран сериализуемым `ScreenConfig`,
  а не прямым вызовом Compose UI. Локальный host пытается обработать конфиг
  своим `NavigationDispatcher`, а нераспознанные запросы поднимаются по
  цепочке `local -> parent` до корневого `RootComponent`.

  На `JS`/`Wasm` тот же активный путь nav-дерева проецируется в историю
  браузера как производный URL (Decompose Web Navigation): URL — выход
  навигации, а не вход. Вход URL только в двух местах: разовый разбор
  cold-start deep-link и browser back/forward.

  Источники реализации:
  - `shared/core/decompose/.../NavigationDispatcher.kt`
  - `shared/app-shell/.../root/DefaultRootComponent.kt`
  - `shared/app-shell/.../primary/DefaultPrimaryShellComponent.kt`
  - `shared/app-shell/.../primary/PrimarySectionConfig.kt` (`WebSectionRoute`)
  - `shared/feature/practice/presentation/impl/DefaultPracticeSectionComponent.kt`
  - `shared/feature/practice/presentation/navigation-api/.../PracticeWebRoute.kt`
  - `apps/webApp/src/{js,wasmJs}Main/.../createEnvironment.kt` (`withWebHistory`)

  Сценарий: Локальный host обрабатывает свой конфиг
    Допустим активен `DefaultPracticeSectionComponent`
    Когда источник навигации вызывает `NavigationDispatcher.open(PracticeConfig.DeckPractice(deckId))`
    Тогда `DefaultPracticeSectionComponent.open` возвращает `NavigationRequestStatus.Handled`
    И stack переходит на `PracticeConfig.DeckPractice(deckId)` через `bringToFront`

  Сценарий: Нераспознанный конфиг поднимается к родителю
    Допустим активен `DefaultPracticeSectionComponent`
    Когда `NavigationDispatcher.open` вызван с `HomeConfig.Home`
    Тогда локальный host возвращает `NavigationRequestStatus.Unhandled`
    И запрос обрабатывается выше — в `PrimaryShellComponent`, который переключает раздел на `Home`

  Сценарий: `RootComponent` маршрутизирует бизнес-конфиг в основной shell
    Допустим основной shell ещё не открыт (top of stack — `StartupConfig`)
    Когда источник навигации вызывает `RootComponent.open(PracticeConfig.Home)`
    Тогда `DefaultRootComponent.toRootBranch` возвращает `RootNavigationBranch.PrimaryShell`
    И stack получает `PrimaryShellConfig(target = PracticeConfig.Home)` через `bringToFront`
    И `PrimaryShellComponent` создаётся уже с нужным target

  Сценарий: При уже открытом shell `RootComponent` переносит его наверх и передаёт target внутрь
    Допустим основной shell уже присутствует в stack
    Когда `RootComponent.open(LibraryConfig.Home)` маршрутизируется в `RootNavigationBranch.PrimaryShell`
    Тогда существующий `PrimaryShellComponent` перемещается на вершину stack через `stackNavigation.navigate(transformer)`
    И вызов `PrimaryShellComponent.open(LibraryConfig.Home, ...)` обрабатывает target внутри shell
    И повторный `PrimaryShellComponent` НЕ создаётся

  Структура сценария: Поддерживаемые бизнес-конфиги для primary shell
    Допустим вызывается `RootComponent.open(<конфиг>)`
    Тогда `toRootBranch` возвращает `PrimaryShell` для следующих конфигов

    Примеры:
      | конфиг                  |
      | `HomeConfig`              |
      | `PracticeConfig`          |
      | `LibraryConfig`           |
      | `VocabularyEditorConfig`  |
      | `ProfileConfig`           |

  Сценарий: Обратная навигация возвращает статус через `NavigationRequestStatus`
    Допустим активен `DefaultPracticeSectionComponent` со stack `[Home, DeckPractice]`
    Когда вызывается `NavigationDispatcher.back(onResult)`
    Тогда `stackNavigation.pop` снимает верхний конфиг
    И `onResult` получает статус обработки запроса

  Сценарий: Bottom bar primary shell скрывается на детальных экранах practice
    Допустим активен `DefaultPracticeSectionComponent`
    Когда top of stack — `PracticeConfig.Home`
    Тогда `showBottomBar` равен `true`
    Когда top of stack — `PracticeConfig.DeckPractice` или `PracticeConfig.CardDetail`
    Тогда `showBottomBar` равен `false`

  Сценарий: URL отражает активную секцию (section-tab режим)
    Допустим клиент запущен на `JS`/`Wasm` с `withWebHistory`
    Когда активна секция `practice`
    Тогда путь URL — `/practice` (`WebSectionRoute.pathFor`)
    И browser back не циклит по табам (`PrimaryShell` `enableHistory = false`)

  Сценарий: Cold deep-link в экран practice восстанавливает вложенный конфиг
    Допустим набран `/practice/deck/d1?focus=c9`
    Когда `DefaultRootComponent.deepLinkLanding` разбирает path и query
    Тогда `WebSectionRoute.landingForPath` возвращает `PracticeConfig.DeckPractice("d1", "c9")`
    И после `Startup` стек секции открыт сразу на этом экране

  Сценарий: Within-section back-stack отражается в истории браузера
    Допустим активна `practice`, стек `[Home, DeckPractice]`
    Когда пользователь жмёт browser back
    Тогда стек секции снимает `DeckPractice` (`DefaultPracticeSectionComponent` `enableHistory = true`)

  Сценарий: Splash не имеет URL и не попадает в историю
    Допустим top of root stack — `StartupConfig`
    Тогда `RootComponent` web-nav `pathMapper` отдаёт `null` и `childSelector` — `null`
    И переход на `PrimaryShell` идёт через `replaceCurrent` (не push) — без своей записи истории

  Сценарий: Warm reload минует Startup и поднимает позицию
    Допустим reload с тем же URL (`deepLink == null`)
    Когда StateKeeper восстанавливает root stack как `[PrimaryShellConfig]`
    Тогда `Startup` не проигрывается
    И активная секция и экран восстановлены из собственного стека `PrimaryShell`
    И `PrimaryShellConfig.target` (`@Transient`) в этом не участвует

  @planned
  Сценарий: Deep-link во `VocabularyEditorConfig.Editor(entryId)`
    Допустим набран `/vocabulary/...` глубже секции
    Тогда сейчас резолвится только до `VocabularyEditorConfig.QuickCapture` (section-level)
    # Полный разбор требует Decompose-навигатора в секции — отложенный разрыв `EB-7`
