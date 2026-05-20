# language: ru
@implemented @practice
Функция: Прохождение карточек в practice deck

  В качестве пользователя, выбравшего колоду на экране `Practice Home`,
  я открываю колоду и прохожу карточки, переворачивая каждую и выставляя оценку,
  чтобы материал колоды попадал в SRS-review и постепенно убывал.

  Источники реализации:
  - `shared/feature/practice/presentation/impl/DeckPracticeLogic.kt`
  - `shared/feature/practice/presentation/impl/DeckPracticeScreen.kt`
  - `shared/feature/practice/presentation/impl/DefaultDeckPracticeComponent.kt`

  Предыстория:
    Допустим открыт раздел `Practice` и `DefaultPracticeSectionComponent` находится на `PracticeConfig.Home`
    И `PracticeHomeLogic` подписан на список колод через `CatalogRepository.observeOwnedMaterial()` (синк сервисного каталога — `refreshFromRemote()` при открытии)
    И список содержит только собственный материал пользователя — захваченные слова и добавленные из `Library` наборы, но не сами сервисные наборы

  Сценарий: Загрузка колоды показывает состояние загрузки
    Когда я нажимаю на колоду в списке `PracticeHomeScreen`
    Тогда `DefaultPracticeSectionComponent` открывает `PracticeConfig.DeckPractice` через stack navigation
    И `DeckPracticeLogic` публикует `DataLoadingState.Loading`
    И `DeckPracticeScreen` показывает индикатор загрузки колоды
    И `top bar` содержит кнопки `close`, `help`, а кнопка `details` неактивна

  Сценарий: Ошибка загрузки колоды позволяет повторить попытку
    Допустим `CatalogRepository.loadDeck` возвращает ошибку для выбранной колоды
    Когда экран `DeckPracticeScreen` получает `DataLoadingState.Error`
    Тогда отображается сообщение об ошибке и кнопка `Retry`
    Когда я нажимаю `Retry`
    Тогда `DeckPracticeLogic` повторно вызывает `repository.loadDeck` и переходит в `DataLoadingState.Loading`

  Сценарий: До переворота карточки жесты и оценки заблокированы
    Допустим колода успешно загружена и текущая карточка не перевёрнута
    Тогда `LearningDeckConfig.gesturesEnabled` равен `false`
    И `LearningDeckConfig.keyboardEnabled` равен `false`
    И все четыре кнопки оценки (`Again`, `Hard`, `Good`, `Easy`) неактивны
    И ни один свайп не приводит к смене карточки

  Сценарий: Tap-to-flip переворачивает верхнюю карточку
    Допустим колода успешно загружена
    И `tapToFlipEnabled` в `DeckPracticeUiState` равен `true`
    Когда я касаюсь верхней карточки
    Тогда `SenseeLearningCard` показывает `back` сторону карточки
    И жесты и кнопки оценки становятся активны для текущей карточки

  Сценарий: Кнопка `Reveal meaning` доступна, когда tap-to-flip выключен
    Допустим колода успешно загружена
    И `tapToFlipEnabled` в `DeckPracticeUiState` равен `false`
    Тогда на лицевой стороне карточки отображается кнопка `Reveal meaning`
    Когда я нажимаю `Reveal meaning`
    Тогда карточка показывает `back` сторону
    И кнопка `Reveal meaning` больше не отображается, пока карточка перевёрнута

  Структура сценария: Свайп перевёрнутой карточки отправляет оценку в SRS
    Допустим верхняя карточка перевёрнута
    Когда я свайпаю карточку в направлении <направление>
    Тогда `DeckPracticeScreen` вызывает `DeckPracticeAction.SubmitReview` с рейтингом <рейтинг>
    И `DeckPracticeLogic` вызывает `PracticeReviewRepository.submitReview` с `CardReview(cardId, <рейтинг>)`
    И флаг `flipped` для этой карточки сбрасывается
    # Дальнейшая судьба карты (graduation покидает сессию и растит `completedCount`,
    # либо in-session reinjection при Learning/Relearning) — отдельные сценарии
    # переинжекта и graduation, пинятся `DeckPracticeLogicTest`.

    Примеры:
      | направление              | рейтинг |
      | `LearningSwipeDirection.Start` | `Again` |
      | `LearningSwipeDirection.Down`  | `Hard`  |
      | `LearningSwipeDirection.Up`    | `Good`  |
      | `LearningSwipeDirection.End`   | `Easy`  |

  Сценарий: Кнопка оценки выполняет программный свайп
    Допустим верхняя карточка перевёрнута
    Когда я нажимаю кнопку `Good` в `PracticeRatingRow`
    Тогда `LearningDeckState.requestSwipe(LearningSwipeDirection.End)` вызывается
    И дальнейший поток обработки совпадает с обычным свайпом в направлении `End`

  Сценарий: Завершение колоды показывает `emptyContent`
    Допустим в колоде была единственная карточка, и она прошла review
    Когда `DeckPracticeUiState.cards` становится пустым
    Тогда `LearningSwipeDeck` рендерит `emptyContent` со строкой `practice_deck_finished`
    И кнопки оценки остаются неактивными

  Сценарий: Открытие деталей карточки на широком экране
    Допустим `LocalAdaptiveInfo.supportsTwoPanes` равен `true`
    И текущая карточка существует
    Когда я нажимаю кнопку `details` в `top bar` (`DeckPracticeAction.FocusCard`)
    Тогда `DefaultDeckPracticeComponent.panels.details` становится `DeckPracticePanelConfig.CardDetail`
    И `DeckPracticeScreen` отображает `DeckPracticeDetailPane` как второй pane рядом с колодой
    И `CardDetailLogic` загружает карточку и связанный `Lemma` через `CatalogRepository`

  Сценарий: Открытие деталей карточки на узком экране
    Допустим `LocalAdaptiveInfo.supportsTwoPanes` равен `false`
    Когда я нажимаю кнопку `details` в `top bar`
    Тогда `DeckPracticeDetailSheet` отображается как bottom sheet поверх колоды
    Когда я закрываю bottom sheet (`DeckPracticeAction.DismissDetails`)
    Тогда `panels.details` становится `null` и sheet проигрывает анимацию закрытия

  Сценарий: Закрытие колоды возвращает к списку колод
    Когда я нажимаю кнопку `close` в `top bar`
    Тогда `DefaultDeckPracticeComponent` вызывает `navigation.back()`
    И `DefaultPracticeSectionComponent` возвращается на `PracticeConfig.Home`

  # Планируемые сценарии (@planned): правка и удаление конкретной карточки
  # прямо из practice. Сейчас в коде не реализовано.

  @planned
  Сценарий: Редактирование конкретной карточки из практики
    Допустим текущая карточка существует
    Когда я выбираю «редактировать» для этой карточки
    Тогда открывается deep-link в `Vocabulary Editor` на edit-path соответствующей `LexicalEntry`
    И после сохранения practice продолжается с обновлённой карточкой

  @planned
  Сценарий: Удаление конкретной карточки из практики
    Допустим текущая карточка существует
    Когда я подтверждаю удаление этой карточки
    Тогда карточка удаляется из каталога и из всех колод, в которые входила
    И practice продолжается со следующей карточкой колоды

  # Планируемые сценарии (@planned): согласованное наполнение карты,
  # detail-панели, help и локальных настроек практики.

  @planned
  Сценарий: Контекстное предложение не имеет заглушек, когда обратная сторона русская
    Допустим `practiceFront` равен `English`
    Когда я смотрю обратную (русскую) сторону карточки
    Тогда контекстное предложение показано целиком без пилюль-заглушек

  @planned
  Сценарий: Перевод контекстного предложения по тапу
    Допустим на карточке показано контекстное предложение
    И настройка `contextTranslation` равна `on-tap`
    Когда я нажимаю на контекстное предложение
    Тогда отображается его перевод
    И перевод не виден по умолчанию, пока я не нажму

  @planned
  Сценарий: Транскрипция показывается по настройке
    Допустим `practiceFront` равен `English`
    И настройка `showTranscription` включена
    Тогда на английской стороне отображается транскрипция headword
    И при выключенной настройке транскрипция не отображается

  @planned
  Сценарий: Ввод ответа как самопроверка при русском фронте
    Допустим `practiceFront` равен `Russian`
    И настройка `answerInput` равна `typed`
    Когда я ввожу ответ в группу пилюль-заглушек
    Тогда клиент сверяет ввод с ожидаемым словом для самопроверки
    И при `answerInput` равном `off` ввод недоступен, остаётся только flip

  @planned
  Сценарий: Detail-панель как хаб исследования карточки
    Когда я открываю detail текущей карточки
    Тогда показаны полный `Meaning`: переводы, контексты с предлогом/коллокацией, explanation и grammar cues с описаниями
    И показаны связанные словоформы (формы неправильного глагола, производные части речи)
    И показаны связанные карточки семьи леммы и связанные грамматические правила

  @planned
  Сценарий: Добавление связанных карточек в колоду из detail
    Допустим detail показывает связанные карточки
    Когда я выбираю часть связанных карточек и добавляю их в колоду для повторения
    Тогда выбранные карточки попадают в курируемую Library колоду
    И курирование остаётся ответственностью Library, practice его не владеет

  @planned
  Сценарий: Help объясняет процесс, а не только бейджи
    Когда я открываю help в practice
    Тогда помимо глоссария бейджей показаны: соответствие свайпов рейтингам, flip / tap-to-flip, режим заглушек и typed-input, перевод контекста по тапу

  @planned
  Сценарий: Кнопка настроек в practice открывает локальный экран настроек
    Допустим локальный экран настроек practice реализован и в `top bar` отображается кнопка `settings`
    Когда я нажимаю кнопку `settings` в `top bar`
    Тогда открывается отдельный экран только со связанными с практикой настройками
    И полный набор настроек доступен в разделе `Profile`, а не здесь
