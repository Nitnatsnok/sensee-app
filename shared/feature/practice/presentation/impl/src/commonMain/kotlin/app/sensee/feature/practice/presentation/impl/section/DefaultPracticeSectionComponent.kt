package app.sensee.feature.practice.presentation.impl.section

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.context.appChildStack
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.navigation.pop
import app.sensee.feature.practice.domain.PracticeSessionSource
import app.sensee.feature.practice.presentation.api.CardDetailComponent
import app.sensee.feature.practice.presentation.api.DeckPracticeComponent
import app.sensee.feature.practice.presentation.api.PracticeHomeComponent
import app.sensee.feature.practice.presentation.api.PracticeSectionComponent
import app.sensee.feature.practice.presentation.navigationApi.PracticeConfig
import app.sensee.feature.practice.presentation.navigationApi.PracticeWebRoute
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStackWebNavigation
import com.arkivanov.decompose.router.webhistory.WebNavigation
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@AssistedInject
public class DefaultPracticeSectionComponent(
    @Assisted public val componentContext: AppComponentContext,
    @Assisted private val target: PracticeConfig?,
    private val practiceHomeComponentFactory: PracticeHomeComponent.Factory,
    private val deckPracticeComponentFactory: DeckPracticeComponent.Factory,
    private val cardDetailComponentFactory: CardDetailComponent.Factory,
) : PracticeSectionComponent,
    AppComponentContext by componentContext {
    private val stackNavigation = StackNavigation<ScreenConfig>()
    private val showBottomBarState = MutableStateFlow((target ?: PracticeConfig.Home).showsBottomBar)

    override val stack: Value<ChildStack<ScreenConfig, AppComponent>> =
        appChildStack(
            source = stackNavigation,
            serializer = screenConfigSerializer,
            initialConfiguration = target ?: PracticeConfig.Home,
            handleBackButton = true,
            navigation = this,
            childFactory = ::createChild,
        )

    // Within-section back stack is reflected in browser history (enableHistory =
    // true): browser Back pops CardDetail/DeckPractice back toward Home. The
    // PrimaryShell parent runs in tab mode (enableHistory = false), so only the
    // active section's stack contributes. Path/params come from PracticeWebRoute,
    // the single source shared with cold-start deep-link resolution.
    @OptIn(ExperimentalDecomposeApi::class)
    override val webNavigation: WebNavigation<*> =
        childStackWebNavigation(
            navigator = stackNavigation,
            stack = stack,
            serializer = screenConfigSerializer,
            enableHistory = true,
            pathMapper = { child: Child.Created<ScreenConfig, AppComponent> ->
                (child.configuration as? PracticeConfig)?.let(PracticeWebRoute::pathFor)
            },
            parametersMapper = { child: Child.Created<ScreenConfig, AppComponent> ->
                (child.configuration as? PracticeConfig)?.let(PracticeWebRoute::parametersFor)
            },
        )

    override val showBottomBar: StateFlow<Boolean> = showBottomBarState.asStateFlow()

    init {
        val stackSubscription =
            stack.subscribe { childStack ->
                showBottomBarState.update {
                    (childStack.active.configuration as? PracticeConfig)?.showsBottomBar ?: true
                }
            }
        lifecycle.doOnDestroy(stackSubscription::cancel)
    }

    override fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus =
        when (target) {
            is PracticeConfig -> {
                stackNavigation.bringToFront(target) {
                    onComplete(true)
                }
                NavigationRequestStatus.Handled
            }
            else -> NavigationRequestStatus.Unhandled
        }

    override fun back(onResult: (NavigationRequestStatus) -> Unit) {
        stackNavigation.pop(onResult = onResult)
    }

    private fun createChild(
        config: ScreenConfig,
        componentContext: AppComponentContext,
    ): AppComponent =
        when (config) {
            PracticeConfig.Home -> {
                practiceHomeComponentFactory.create(componentContext)
            }

            is PracticeConfig.DeckPractice ->
                deckPracticeComponentFactory.create(
                    componentContext = componentContext,
                    args =
                        DeckPracticeComponent.Args(
                            source = PracticeSessionSource.Deck(config.deckId),
                            focusedCardId = config.focusedCardId,
                        ),
                )

            PracticeConfig.DuePractice ->
                deckPracticeComponentFactory.create(
                    componentContext = componentContext,
                    args = DeckPracticeComponent.Args(source = PracticeSessionSource.Due),
                )

            is PracticeConfig.CardDetail ->
                cardDetailComponentFactory.create(
                    componentContext = componentContext,
                    args = CardDetailComponent.Args(cardId = config.cardId),
                )

            else -> error("Unknown screen config: $config")
        }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<PracticeSectionComponent.Factory>(),
    )
    public fun interface Factory : PracticeSectionComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            target: PracticeConfig?,
        ): DefaultPracticeSectionComponent
    }
}

/**
 * Practice's own bottom-bar policy: the deep practice surfaces (a running deck, a card's
 * detail) are immersive and hide the bar; the practice home keeps it. This lives in the
 * presentation layer because chrome visibility is a presentation concern the section owns —
 * deliberately *not* on the `@Serializable` navigation config in navigation-api.
 */
private val PracticeConfig.showsBottomBar: Boolean
    get() =
        when (this) {
            PracticeConfig.Home -> true
            is PracticeConfig.DeckPractice -> false
            PracticeConfig.DuePractice -> false
            is PracticeConfig.CardDetail -> false
        }
