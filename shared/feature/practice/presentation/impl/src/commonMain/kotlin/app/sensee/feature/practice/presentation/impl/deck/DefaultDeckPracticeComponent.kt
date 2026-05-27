package app.sensee.feature.practice.presentation.impl.deck

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.context.appChildPanels
import app.sensee.core.decompose.logic.LogicKey
import app.sensee.core.decompose.logic.getOrCreateLogic
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.feature.practice.presentation.api.CardDetailComponent
import app.sensee.feature.practice.presentation.api.DeckPracticeAction
import app.sensee.feature.practice.presentation.api.DeckPracticeChildPanels
import app.sensee.feature.practice.presentation.api.DeckPracticeComponent
import app.sensee.feature.practice.presentation.api.DeckPracticePanelConfig
import app.sensee.feature.practice.presentation.api.DeckPracticeUiState
import app.sensee.feature.practice.presentation.navigationApi.PracticeConfig
import app.sensee.tts.core.Speaker
import app.sensee.tts.core.SpeechLocale
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.router.panels.Panels
import com.arkivanov.decompose.router.panels.PanelsNavigation
import com.arkivanov.decompose.router.panels.navigate
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalDecomposeApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
@AssistedInject
public class DefaultDeckPracticeComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted public val args: DeckPracticeComponent.Args,
    private val deckPracticeLogicFactory: DeckPracticeLogic.Factory,
    private val cardDetailFactory: CardDetailComponent.Factory,
    private val speaker: Speaker,
    appDispatchers: AppDispatchers,
) : DeckPracticeComponent,
    AppComponentContext by componentContext {
    private val componentScope = CoroutineScope(appDispatchers.main.immediate + SupervisorJob())
    private val speechController = DeckPracticeSpeechController(speaker)

    private val logic =
        getOrCreateLogic(LogicKey("DeckPracticeLogic:${args.deckId}")) {
            deckPracticeLogicFactory.create(deckId = args.deckId)
        }

    init {
        lifecycle.doOnDestroy {
            speechController.stopAll()
            componentScope.cancel()
        }
    }

    private val panelsNavigation =
        PanelsNavigation<DeckPracticePanelConfig.Deck, DeckPracticePanelConfig.CardDetail, Nothing>()

    private val deckPanelHost: AppComponent = object : AppComponent {}

    override val uiState: StateFlow<DeckPracticeUiState> =
        combine(logic.uiState, speechController.state) { uiState, speech ->
            uiState.copy(speech = speech)
        }.stateIn(
            scope = componentScope,
            started = SharingStarted.Eagerly,
            initialValue = logic.uiState.value.copy(speech = speechController.state.value),
        )

    override val panels: Value<DeckPracticeChildPanels> =
        appChildPanels(
            source = panelsNavigation,
            serializers = null,
            initialPanels = {
                Panels(
                    main = DeckPracticePanelConfig.Deck(args.deckId),
                    details = args.focusedCardId?.let(DeckPracticePanelConfig::CardDetail),
                    mode = ChildPanelsMode.DUAL,
                )
            },
            handleBackButton = true,
            navigation = PanelOpenCardNavigation { cardId -> focusCard(cardId) },
            mainFactory = { _, _ -> deckPanelHost },
            detailsFactory = { config, childContext ->
                cardDetailFactory.create(
                    componentContext = childContext,
                    args = CardDetailComponent.Args(cardId = config.cardId),
                )
            },
        )

    override fun onAction(action: DeckPracticeAction) {
        when (action) {
            is DeckPracticeAction.FocusCard -> focusCard(action.cardId)
            DeckPracticeAction.DismissDetails -> dismissDetails()
            DeckPracticeAction.Close -> navigation.back()
            is DeckPracticeAction.SpeakText ->
                speechController.speak(
                    targetId = action.targetId,
                    text = action.text,
                    locale = speechLocaleFor(logic.uiState.value.studyLanguageTag),
                    scope = componentScope,
                )
            DeckPracticeAction.Retry,
            DeckPracticeAction.RetryGrammarLabels,
            DeckPracticeAction.ToggleTapToFlip,
            is DeckPracticeAction.SubmitReview,
            DeckPracticeAction.OpenHelp,
            -> logic.onAction(action)
        }
    }

    private fun focusCard(cardId: String) {
        panelsNavigation.navigate(
            details = DeckPracticePanelConfig.CardDetail(cardId),
            extra = null,
        )
    }

    private fun dismissDetails() {
        panelsNavigation.navigate(details = null, extra = null)
    }

    private fun speechLocaleFor(languageTag: String): SpeechLocale {
        val normalized = languageTag.trim()
        return when (normalized.lowercase()) {
            "",
            "en",
            -> SpeechLocale.English

            "ru" -> SpeechLocale.Russian
            else -> SpeechLocale(normalized)
        }
    }

    /**
     * Intercepts `PracticeConfig.CardDetail` open requests fired from the embedded card detail
     * panel and reroutes them to update the details panel locally instead of pushing a new screen
     * to the parent stack. Other navigation requests fall through to the parent.
     */
    private class PanelOpenCardNavigation(
        private val onCardFocused: (cardId: String) -> Unit,
    ) : NavigationDispatcher {
        override fun open(
            target: ScreenConfig,
            onComplete: (isSuccess: Boolean) -> Unit,
        ): NavigationRequestStatus =
            when (target) {
                is PracticeConfig.CardDetail -> {
                    onCardFocused(target.cardId)
                    onComplete(true)
                    NavigationRequestStatus.Handled
                }
                else -> NavigationRequestStatus.Unhandled
            }

        override fun back(onResult: (NavigationRequestStatus) -> Unit) {
            onResult(NavigationRequestStatus.Unhandled)
        }
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<DeckPracticeComponent.Factory>(),
    )
    public fun interface Factory : DeckPracticeComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            args: DeckPracticeComponent.Args,
        ): DefaultDeckPracticeComponent
    }
}
