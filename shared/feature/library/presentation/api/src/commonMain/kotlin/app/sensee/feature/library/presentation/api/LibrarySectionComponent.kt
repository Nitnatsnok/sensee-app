package app.sensee.feature.library.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.navigation.BottomBarVisibilityOwner
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.feature.library.presentation.navigationApi.LibraryConfig
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.value.Value

/**
 * Library section panels: the deck list ([LibraryConfig.Home]) as the main panel and a deck's
 * read-only card browse ([LibraryConfig.DeckDetail]) as the optional detail panel. On a wide
 * layout the two render side by side (list-detail); on a compact layout they are presented one
 * at a time. The third (extra) panel is unused for now — a card-detail pane is a planned
 * follow-up, hence the [Nothing] extra slots.
 */
@OptIn(ExperimentalDecomposeApi::class)
public typealias LibraryChildPanels =
    ChildPanels<
        LibraryConfig.Home,
        LibraryHomeComponent,
        LibraryConfig.DeckDetail,
        LibraryDeckDetailComponent,
        Nothing,
        Nothing,
    >

@OptIn(ExperimentalDecomposeApi::class)
public interface LibrarySectionComponent :
    AppComponent,
    NavigationDispatcher,
    BottomBarVisibilityOwner {
    public val panels: Value<LibraryChildPanels>

    public interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            target: LibraryConfig? = null,
        ): LibrarySectionComponent
    }
}
