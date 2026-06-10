package app.sensee.feature.library.presentation.impl

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.context.AppContentPresentation
import app.sensee.core.decompose.context.appChildPanels
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.feature.library.presentation.api.LibraryChildPanels
import app.sensee.feature.library.presentation.api.LibraryDeckDetailComponent
import app.sensee.feature.library.presentation.api.LibraryHomeComponent
import app.sensee.feature.library.presentation.api.LibrarySectionComponent
import app.sensee.feature.library.presentation.navigationApi.LibraryConfig
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.decompose.router.panels.Panels
import com.arkivanov.decompose.router.panels.PanelsNavigation
import com.arkivanov.decompose.router.panels.navigate
import com.arkivanov.decompose.router.panels.setMode
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.subscribe
import com.arkivanov.essenty.backhandler.BackCallback
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalDecomposeApi::class)
@AssistedInject
public class DefaultLibrarySectionComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted target: LibraryConfig?,
    private val libraryHomeComponentFactory: LibraryHomeComponent.Factory,
    private val libraryDeckDetailComponentFactory: LibraryDeckDetailComponent.Factory,
) : LibrarySectionComponent,
    AppComponentContext by componentContext {
    private val panelsNavigation = PanelsNavigation<LibraryConfig.Home, LibraryConfig.DeckDetail, Nothing>()

    override val panels: Value<LibraryChildPanels> =
        appChildPanels(
            source = panelsNavigation,
            serializers =
                Pair(
                    LibraryConfig.Home.serializer(),
                    LibraryConfig.DeckDetail.serializer(),
                ),
            initialPanels = {
                Panels(
                    main = LibraryConfig.Home,
                    details = target as? LibraryConfig.DeckDetail,
                    mode = ChildPanelsMode.SINGLE,
                )
            },
            handleBackButton = false,
            navigation = this,
            mainFactory = { _, childContext ->
                libraryHomeComponentFactory.create(childContext)
            },
            detailsFactory = { config, childContext ->
                libraryDeckDetailComponentFactory.create(
                    componentContext = childContext,
                    args = LibraryDeckDetailComponent.Args(deckId = config.deckId, onClose = ::closeDeck),
                )
            },
        )

    override val showBottomBar: StateFlow<Boolean>
        field = MutableStateFlow(true)

    // System back closes the open deck only in single-pane (where the detail covers the list).
    // In list-detail the list stays visible, so the press falls through to the shell.
    // appChildPanels' own handleBackButton stays off so this is the single source.
    private val panelsBackCallback =
        BackCallback(isEnabled = false) {
            if (isDeckClosable()) {
                closeDeck()
            }
        }

    init {
        backHandler.register(panelsBackCallback)
        // Unlike Profile, Library keeps no "last detail" to re-open on a wide layout: a deck
        // selection is always explicit, so an empty detail pane is a valid list-detail state.
        // This subscription therefore only refreshes the back-callback, not any stored config.
        panels.subscribe(lifecycle) { updatePanelsBackCallback() }
        contentPresentation.subscribe(lifecycle) { presentation ->
            applyContentPresentation(presentation)
        }
    }

    override fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus =
        when (target) {
            LibraryConfig.Home -> {
                // Re-selecting the section returns to the list on a compact layout; on a wide
                // layout the list is always visible, so there is nothing to do.
                if (contentPresentation.value == AppContentPresentation.SinglePane) {
                    closeDeck()
                }
                onComplete(true)
                NavigationRequestStatus.Handled
            }
            is LibraryConfig.DeckDetail -> {
                openDeck(target)
                onComplete(true)
                NavigationRequestStatus.Handled
            }
            else -> NavigationRequestStatus.Unhandled
        }

    override fun back(onResult: (NavigationRequestStatus) -> Unit) {
        if (isDeckClosable()) {
            closeDeck()
            onResult(NavigationRequestStatus.Handled)
        } else {
            onResult(NavigationRequestStatus.Unhandled)
        }
    }

    private fun isDeckClosable(): Boolean =
        panels.value.details != null && contentPresentation.value == AppContentPresentation.SinglePane

    // The named-arg navigate(details = ...) convenience is unavailable when the extra
    // panel type is Nothing (the 2-pane shape), so drive the transformer directly.
    private fun openDeck(config: LibraryConfig.DeckDetail) {
        panelsNavigation.navigate { panels -> panels.copy(details = config) }
    }

    private fun closeDeck() {
        panelsNavigation.navigate { panels -> panels.copy(details = null) }
    }

    private fun applyContentPresentation(presentation: AppContentPresentation) {
        val mode = presentation.toChildPanelsMode()
        if (panels.value.mode != mode) {
            panelsNavigation.setMode(mode)
        }
        updatePanelsBackCallback()
    }

    private fun updatePanelsBackCallback() {
        panelsBackCallback.isEnabled = isDeckClosable()
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<LibrarySectionComponent.Factory>(),
    )
    public fun interface Factory : LibrarySectionComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            target: LibraryConfig?,
        ): DefaultLibrarySectionComponent
    }
}

@OptIn(ExperimentalDecomposeApi::class)
private fun AppContentPresentation.toChildPanelsMode(): ChildPanelsMode =
    when (this) {
        AppContentPresentation.SinglePane -> ChildPanelsMode.SINGLE
        AppContentPresentation.ListDetail -> ChildPanelsMode.DUAL
        AppContentPresentation.SupportingPane -> ChildPanelsMode.TRIPLE
    }
