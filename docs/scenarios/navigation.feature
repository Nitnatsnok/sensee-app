# language: ru
@navigation
Функция: Распространение навигации чистыми конфигурациями

  Источник навигации описывает целевой экран сериализуемым `ScreenConfig`,
  а не прямым вызовом Compose UI. Локальный хост пытается обработать конфигурацию
  своим `NavigationDispatcher`, а нераспознанные запросы поднимаются по
  цепочке `local -> parent` до корневого `RootComponent`.

  На `JS`/`Wasm` активный путь nav-дерева проецируется в историю браузера
  как производный URL (Decompose Web Navigation). URL строится из текущего
  состояния навигации; входом он становится только при прямой ссылке на
  холодном старте и кнопках назад/вперёд браузера.

  Источники реализации:
  - `shared/core/decompose/.../NavigationDispatcher.kt`
  - `shared/app-shell/.../root/DefaultRootComponent.kt`
  - `shared/app-shell/.../primary/DefaultPrimaryShellComponent.kt`
  - `shared/app-shell/.../primary/PrimaryNavLayout.kt` (`selectPrimaryNavLayout`)
  - `shared/app-shell/.../primary/PrimarySectionConfig.kt` (`WebSectionRoute`)
  - `shared/feature/practice/presentation/impl/DefaultPracticeSectionComponent.kt`
  - `shared/feature/practice/presentation/navigation-api/.../PracticeWebRoute.kt`
  - `apps/webApp/src/{js,wasmJs}Main/.../createEnvironment.kt` (`withWebHistory`)

  @implemented
  Сценарий: Локальный хост обрабатывает свою конфигурацию
    Допустим активен `DefaultPracticeSectionComponent`
    Когда источник навигации вызывает `NavigationDispatcher.open(PracticeConfig.DeckPractice(deckId))`
    Тогда `DefaultPracticeSectionComponent.open` возвращает `NavigationRequestStatus.Handled`
    И stack переходит на `PracticeConfig.DeckPractice(deckId)` через `bringToFront`

  @implemented
  Сценарий: Нераспознанная конфигурация поднимается к родителю
    Допустим активен `DefaultPracticeSectionComponent`
    Когда `NavigationDispatcher.open` вызван с `HomeConfig.Home`
    Тогда локальный хост возвращает `NavigationRequestStatus.Unhandled`
    И запрос обрабатывается выше — в `PrimaryShellComponent`, который переключает раздел на `Home`

  @implemented
  Сценарий: `RootComponent` маршрутизирует бизнес-конфигурацию в основной shell
    Допустим основной shell ещё не открыт (top of stack — `StartupConfig`)
    Когда источник навигации вызывает `RootComponent.open(PracticeConfig.Home)`
    Тогда `DefaultRootComponent.toRootBranch` возвращает `RootNavigationBranch.PrimaryShell`
    И stack получает `PrimaryShellConfig(target = PracticeConfig.Home)` через `bringToFront`
    И `PrimaryShellComponent` создаётся уже с нужным target

  @implemented
  Сценарий: При уже открытом shell `RootComponent` переносит его наверх и передаёт target внутрь
    Допустим основной shell уже присутствует в stack
    Когда `RootComponent.open(LibraryConfig.Home)` маршрутизируется в `RootNavigationBranch.PrimaryShell`
    Тогда существующий `PrimaryShellComponent` перемещается на вершину stack через `stackNavigation.navigate(transformer)`
    И вызов `PrimaryShellComponent.open(LibraryConfig.Home, ...)` обрабатывает target внутри shell
    И повторный `PrimaryShellComponent` не создаётся

  @implemented
  Структура сценария: Поддерживаемые бизнес-конфигурации для основной навигационной оболочки
    Допустим вызывается `RootComponent.open(<конфигурация>)`
    Тогда `toRootBranch` возвращает `PrimaryShell` для следующих конфигураций

    Примеры:
      | конфигурация            |
      | `HomeConfig`              |
      | `PracticeConfig`          |
      | `LibraryConfig`           |
      | `VocabularyEditorConfig`  |
      | `ProfileConfig`           |

  @implemented
  Сценарий: Обратная навигация возвращает статус через `NavigationRequestStatus`
    Допустим активен `DefaultPracticeSectionComponent` со stack `[Home, DeckPractice]`
    Когда вызывается `NavigationDispatcher.back(onResult)`
    Тогда `stackNavigation.pop` снимает верхнюю конфигурацию
    И `onResult` получает статус обработки запроса

  @implemented
  Сценарий: Нижняя панель основной навигационной оболочки скрывается на детальных экранах practice
    Допустим активен `DefaultPracticeSectionComponent`
    Когда top of stack — `PracticeConfig.Home`
    Тогда `showBottomBar` равен `true`
    Когда top of stack — `PracticeConfig.DeckPractice` или `PracticeConfig.CardDetail`
    Тогда `showBottomBar` равен `false`

  @implemented
  Сценарий: Тип основной навигационной панели выбирается по платформе и размеру окна
    Допустим основная навигационная оболочка выбирает панель через `selectPrimaryNavLayout`
    Когда окно широкое (`showNavigationRail == true`) и платформа — `JS` или `Wasm`
    Тогда отрисовывается горизонтальный top bar (`SenseeTopNavigationBar`)
    Когда окно широкое и платформа — `Android`, `Ios` или `Desktop`
    Тогда отрисовывается вертикальный rail (`SenseeNavigationRail`)
    Когда окно компактное (`showNavigationRail == false`)
    Тогда на любой платформе отрисовывается нижняя панель (`SenseeBottomNavigationBar`)

  @implemented
  Сценарий: URL отражает активную секцию (section-tab режим)
    Допустим клиент запущен на `JS`/`Wasm` с `withWebHistory`
    Когда активна секция `practice`
    Тогда путь URL — `/practice` (`WebSectionRoute.pathFor`)
    И кнопка назад браузера не циклит по табам (`PrimaryShell` `enableHistory = false`)

  @implemented
  Сценарий: Прямая ссылка на холодном старте восстанавливает вложенный экран practice
    Допустим набран `/practice/deck/d1?focus=c9`
    Когда `DefaultRootComponent.deepLinkLanding` разбирает path и query
    Тогда `WebSectionRoute.landingForPath` возвращает `PracticeConfig.DeckPractice("d1", "c9")`
    И после `Startup` стек секции открыт сразу на этом экране

  @implemented
  Сценарий: Within-section back-stack отражается в истории браузера
    Допустим активна `practice`, стек `[Home, DeckPractice]`
    Когда пользователь нажимает кнопку назад браузера
    Тогда стек секции снимает `DeckPractice` (`DefaultPracticeSectionComponent` `enableHistory = true`)

  @implemented
  Сценарий: Splash не имеет URL и не попадает в историю
    Допустим top of root stack — `StartupConfig`
    Тогда `RootComponent` web-nav `pathMapper` отдаёт `null` и `childSelector` — `null`
    И переход на `PrimaryShell` идёт через `replaceCurrent` (не push) — без своей записи истории

  @implemented
  Сценарий: Warm reload минует Startup и поднимает позицию
    Допустим reload с тем же URL (`deepLink == null`)
    Когда StateKeeper восстанавливает root stack как `[PrimaryShellConfig]`
    Тогда `Startup` не проигрывается
    И активная секция и экран восстановлены из собственного стека `PrimaryShell`
    И `PrimaryShellConfig.target` (`@Transient`) в этом не участвует

  @planned
  Сценарий: Deep-link во `VocabularyEditorConfig.Editor(entryId)`
    Допустим набран `/vocabulary/...` глубже секции
    Тогда сейчас URL разбирается только до `VocabularyEditorConfig.QuickCapture` (уровень секции)
    # Полный разбор требует Decompose-навигатора в секции — отложенный разрыв `EB-7`
