# language: ru
@profile
Функция: Профиль и полный набор пользовательских настроек

  Profile — пользовательский интерфейс полного набора настроек поверх `UserSettingsRepository`.
  Раздел открывается экраном-меню `ProfileHome` с группой «Настройки» и
  пунктами по категориям `UserSettingsCategory`. На широких экранах меню и
  экран категории показаны рядом (двухпанельный режим), на узких — по очереди.
  Реализованный экран категории — `Ai` (`ProfileAiSettingsScreen`): выбор
  AI/TTS-провайдера, верификация ключа и постепенное раскрытие полей.
  Категории `App`/`Practice`/`Experimental` пока открывают явную заглушку
  planned-состояния; в `Learning` реализован выбор тем для примеров.
  Навигация `About`, Account/синхронизация и статистика — планируемое.

  Сценарии без тега описывают текущий код; `@planned` — спроектированное,
  но ещё не реализованное. Объём сознательно бережный: ядро — настройки и
  About; Account/синхронизация и data management пока не реализованы и не
  имитируются фиктивным UI.

  Связанные view в `docs/c4/`:
  - `capability_map`

  Источники реализации:
  - `shared/feature/profile/presentation/impl`: `DefaultProfileSectionComponent`
    (трёхпанельная навигация через `appChildPanels` — `Home` + категория +
    опциональную дополнительную панель для выбора тем; категория `Learning` открывает выбор тем
    через `NavigationDispatcher.open(ProfileExtraConfig.TopicPicker)`),
    `DefaultProfileHomeComponent`/`ProfileHomeScreen` (меню категорий),
    `ProfileAiSettingsLogic`/`ProfileAiSettingsScreen` (категория `Ai`),
    `ProfileLearningSettingsLogic`/`ProfileLearningSettingsScreen` (категория
    `Learning` — пока только строка-триггер выбора тем),
    `DefaultProfileTopicPickerComponent`/`ProfileTopicPickerLogic`/
    `ProfileTopicPickerScreen` (сам выбор тем),
    `ProfileSettingsPlaceholderScreen` (явная заглушка остальных категорий)
  - `shared/feature/profile/presentation/api`: `ProfileSectionComponent`,
    `ProfileHomeComponent`, `ProfileAiSettingsUiState`/`Action`,
    `ProfileLearningSettingsComponent`/`UiState`/`Action`,
    `ProfileTopicPickerComponent`/`UiState`/`Action`, `KeyCheckStatus`
  - `shared/feature/profile/presentation/navigation-api`: `ProfileConfig`
    (`Home` + `Settings.App`/`Settings.Learning`/`Settings.Practice`/
    `Settings.Ai`/`Settings.Experimental`),
    `ProfileExtraConfig.TopicPicker`
  - `shared/settings/domain`: `UserSettingsRepository`,
    `UserSettingsCategory{App, Learning, Practice, Ai, Experimental}`,
    `UserSettingsScope{Device, User}`, `AiSettings` (ключи/провайдер),
    `LearningSettings.preferredTopicIds`, `LearningTopic`,
    `TopicCatalogRepository`
  - `shared/settings/data/topic/*`: каталог тем с mock-бэкенда (`learning/topics`)
  - design-system: `SenseeTextField` со скрытым (secure) режимом для ключей,
    `SenseeCheckbox` + `SenseeModalBottomSheet` + `SenseeSheetHeader` для выбора тем
  - границы AI/TTS-интеграций `shared/ai/*` / `shared/tts/*` (ADR-005): список
    совместимых провайдеров и выбор модели (из API провайдера, офлайн-fallback)
  - Планируемое: экраны категорий `App`/`Practice`, языки в `Learning`, `About`

  Предыстория:
    Допустим пользователь открыл раздел `Profile`

  @implemented
  Сценарий: Меню Profile с группой настроек
    Когда пользователь открывает раздел `Profile`
    Тогда показан экран-меню `ProfileHome` с группой «Настройки»
    И в группе по пункту на каждую `UserSettingsCategory`: `App`, `Learning`, `Practice`, `Ai`, `Experimental`
    Когда пользователь выбирает пункт
    Тогда открывается экран этой категории

  @implemented
  Сценарий: Двухпанельная раскладка Profile на широких экранах
    Допустим окно достаточно широкое (`supportsTwoPanes`)
    Когда пользователь открывает раздел `Profile`
    Тогда показано меню; при выборе пункта экран категории открывается рядом во второй панели
    И открытый экран категории закрывается кнопкой в его верхней панели
    Если окно узкое (компактная раскладка)
    Тогда показано меню; выбор пункта открывает экран категории на весь экран
    И кнопка «Назад» в верхней панели возвращает к меню
    И открытие и закрытие экрана категории анимированы

  @implemented
  Сценарий: Нереализованные категории показывают явную заглушку
    Когда пользователь выбирает категорию `App`, `Practice` или `Experimental`
    Тогда показан экран «раздел появится позже» без неработающих полей

  @planned
  Сценарий: Настройки категории показывают свои поля
    Когда пользователь открывает экран категории `App`, `Learning` или `Practice`
    Тогда показаны поля категории из `UserSettingsSnapshot`

  @planned
  Сценарий: Правка настроек App
    Когда пользователь меняет `themeMode` или язык интерфейса в категории `App`
    Тогда изменение сохраняется через `UserSettingsRepository.updateAppSettings`
    И применяется к приложению

  @planned
  Сценарий: Правка настроек Learning
    Когда пользователь меняет язык изучения или язык перевода в категории `Learning`
    Тогда изменение сохраняется через `UserSettingsRepository.updateLearningSettings`

  @implemented
  Сценарий: Выбор предпочитаемых тем для генерации примеров
    Допустим каталог тем загружается с mock-бэкенда (`learning/topics`)
    Когда пользователь в категории `Learning` нажимает строку выбора «Темы для примеров»
    Тогда `DefaultProfileLearningSettingsComponent` вызывает
      `navigation.open(ProfileExtraConfig.TopicPicker)`
    И `DefaultProfileSectionComponent` переводит `appChildPanels` в состояние extra =
      `ProfileExtraConfig.TopicPicker` и поднимает `DefaultProfileTopicPickerComponent`
    Если окно поддерживает supporting-pane раскладку (`ContentLayoutType.SupportingPane`)
    Тогда выбор тем отображается третьей панелью рядом с категорией
    Иначе (list-detail или компакт)
    Тогда выбор тем отображается в `SenseeModalBottomSheet` поверх раскладки
    И в выборе тем показан список тем чекбоксами
    Когда пользователь отмечает или снимает темы
    Тогда выбор сохраняется как идентификаторы тем в `LearningSettings.preferredTopicIds`
    И сохранение оптимистично — UI обновляется сразу, ошибка сохранения откатывает локальное состояние
    И триггер-строка в `Learning` видит изменение через `observeSettings`
    И список тем приходит с бэкенда, а не зашит в клиенте
    И системная «назад» закрывает выбор тем прежде, чем выйти из категории `Learning`
      (каскад back-обработки секции: extra -> details -> unhandled)
    И выбранные темы используются границей AI-интеграции для подбора примеров (см. `vocabulary-capture.feature`)

    # Категория `Learning` пока содержит только выбор тем; правка языков
    # (см. сценарий выше) остаётся `@planned`.

  @implemented
  Сценарий: Категория Ai — поля раскрываются по мере заполнения
    Когда пользователь открывает категорию `Ai`
    Тогда сначала показан только выбор AI-провайдера из списка совместимых провайдеров (не ввод строкой)
    И базовый URL предопределён выбранным провайдером, а не вводится вручную
    И ниже показано поле ввода ключа со скрытым (secure) режимом и кнопкой `Проверить`
    И поле выбора модели не показано, пока ключ не проверен

  @implemented
  Сценарий: Проверка ключа AI по кнопке открывает выбор модели
    Допустим пользователь ввёл AI-ключ
    Когда пользователь нажимает `Проверить`
    Тогда ключ проверяется одним запросом списка моделей провайдера (`AiModelCatalog.verifyKey`)
    И при валидном ключе показывается выбор модели из полученного списка
    И при невалидном или недоступном провайдере показывается причина и поле ручного ввода модели (деградация ADR-005, без жёсткой блокировки)
    И изменение сохраняется через `UserSettingsRepository` в `AiSettings` (scope `Device`)

  @implemented
  Сценарий: Смена провайдера сбрасывает зависимые поля
    Допустим ключ AI уже проверен и выбрана модель
    Когда пользователь выбирает другого AI-провайдера
    Тогда состояние проверки ключа и выбранная модель сбрасываются
    И список моделей пуст до новой проверки

  @implemented
  Сценарий: TTS-провайдер выбором, поля раскрываются после проверки
    Когда пользователь открывает категорию `Ai`
    Тогда TTS-провайдер выбирается из списка (`ElevenLabs`, `OpenAI`), а не вводится строкой
    И поля модели и голоса показываются только после валидной проверки ключа TTS
    Если выбран `ElevenLabs`
    Тогда модель и голос берутся из API провайдера (`/v1/models`, `/v1/voices`); при невалидном ключе — ручной ввод
    Если выбран `OpenAI`
    Тогда голоса берутся из фиксированного известного набора (у OpenAI нет API списка голосов), а ключ проверяется как OpenAI-совместимый
    И провайдеры TTS не API-совместимы, поэтому у каждого свой адаптер за контрактом `Speaker`
    И изменение сохраняется через `UserSettingsRepository` в `AiSettings.tts*` (scope `Device`)

  @implemented
  Сценарий: Общий ключ TTS при совпадении провайдера с AI
    Допустим AI-провайдер `OpenAI` и его ключ проверен как валидный
    Когда пользователь выбирает TTS-провайдер `OpenAI`
    Тогда поле ключа TTS не запрашивается отдельно, а показывается явное состояние «используется ключ AI-провайдера»
    И статус валидности и поля модели/голоса наследуются от проверки ключа AI
    И доступен переключатель «задать отдельный ключ», возвращающий обычное поле ключа с кнопкой `Проверить`
    И смена AI-провайдера прочь от `OpenAI` снимает наследование и возвращает отдельное поле ключа

  @implemented
  Сценарий: Поле ключа — скрытый ввод из дизайн-системы
    Когда пользователь вводит API-ключ (AI или TTS)
    Тогда поле использует скрытый (secure) режим `SenseeTextField` из дизайн-системы, а не самописное маскирование в экране
    И доступен переключатель показать/скрыть значение
    И ключ хранится device-scoped и не отображается открытым по умолчанию

  @planned
  Сценарий: Полный набор practice-настроек в Profile
    Когда пользователь открывает категорию `Practice`
    Тогда доступен полный набор practice-настроек (включая планируемые `tapToFlip`, `answerInput`, `practiceFront` override, `contextTranslation`)
    И локальный экран настроек practice показывает то же подмножество над тем же `UserSettingsRepository`
    И изменение в одном месте видно в другом — единый источник

  @planned
  Сценарий: Категория Experimental явно помечена как planned-состояние
    Допустим у категории `Experimental` пока нет data class и полей в `UserSettingsSnapshot`
    Когда пользователь открывает категорию `Experimental`
    Тогда показывается состояние «нет данных», а не неработающие тумблеры

  @planned
  Сценарий: Область хранения настроек по умолчанию Device, User — будущая синхронизация
    Допустим аккаунта и авторизации в клиенте нет
    Когда пользователь правит любые настройки
    Тогда они сохраняются в области `Device` по умолчанию
    И `UserSettingsScope.User` зарезервирован под будущую per-user синхронизацию
    И переключатель Device/User не показывается, пока нет auth

  @planned
  Сценарий: Account и синхронизация пока не реализованы
    Когда пользователь открывает блок аккаунта
    Тогда показывается «локальный профиль, вход и синхронизация позже» без неработающего UI

  @planned
  Сценарий: Экран About
    Когда пользователь открывает `About`
    Тогда показываются версия приложения и ссылки
