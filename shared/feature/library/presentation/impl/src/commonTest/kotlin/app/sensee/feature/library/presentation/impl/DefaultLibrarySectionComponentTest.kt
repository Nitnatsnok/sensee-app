package app.sensee.feature.library.presentation.impl

import app.sensee.core.decompose.context.AppContentPresentation
import app.sensee.core.decompose.context.RootComponentContext
import app.sensee.core.decompose.navigation.NavigationRequestStatus
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.feature.library.presentation.api.LibraryDeckDetailAction
import app.sensee.feature.library.presentation.api.LibraryDeckDetailComponent
import app.sensee.feature.library.presentation.api.LibraryDeckDetailUiState
import app.sensee.feature.library.presentation.api.LibraryHomeAction
import app.sensee.feature.library.presentation.api.LibraryHomeComponent
import app.sensee.feature.library.presentation.api.LibraryHomeUiState
import app.sensee.feature.library.presentation.navigationApi.LibraryConfig
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.PolymorphicSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalDecomposeApi::class)
class DefaultLibrarySectionComponentTest {
    @Test
    fun `section opens on the deck list with no detail`() {
        val component = buildComponent(target = null)

        assertNull(component.panels.value.details)
        assertEquals(ChildPanelsMode.SINGLE, component.panels.value.mode)
    }

    @Test
    fun `opening a deck shows it in the detail panel`() {
        val component = buildComponent(target = null)

        val status = component.open(LibraryConfig.DeckDetail("d1")) {}

        val detail = component.panels.value.details
        assertEquals(NavigationRequestStatus.Handled, status)
        assertEquals(LibraryConfig.DeckDetail("d1"), detail?.configuration)
        assertIs<LibraryDeckDetailComponent>(detail?.instance)
    }

    @Test
    fun `a deck deep-link target opens straight into the detail panel`() {
        val component = buildComponent(target = LibraryConfig.DeckDetail("d1"))

        assertEquals(
            LibraryConfig.DeckDetail("d1"),
            component.panels.value.details
                ?.configuration,
        )
    }

    @Test
    fun `a wide layout drives the dual panels mode`() {
        val component = buildComponent(target = null, contentPresentation = AppContentPresentation.ListDetail)

        assertEquals(ChildPanelsMode.DUAL, component.panels.value.mode)
    }

    @Test
    fun `a supporting-pane layout drives the triple panels mode`() {
        val component = buildComponent(target = null, contentPresentation = AppContentPresentation.SupportingPane)

        assertEquals(ChildPanelsMode.TRIPLE, component.panels.value.mode)
    }

    @Test
    fun `single-pane back closes the open deck and reports handled`() {
        val component = buildComponent(target = null)
        component.open(LibraryConfig.DeckDetail("d1")) {}

        var result: NavigationRequestStatus? = null
        component.back { result = it }

        assertEquals(NavigationRequestStatus.Handled, result)
        assertNull(component.panels.value.details)
    }

    @Test
    fun `back with no open deck reports unhandled`() {
        val component = buildComponent(target = null)

        var result: NavigationRequestStatus? = null
        component.back { result = it }

        assertEquals(NavigationRequestStatus.Unhandled, result)
    }

    @Test
    fun `a wide layout keeps the open deck on back so the press falls through`() {
        val component = buildComponent(target = null, contentPresentation = AppContentPresentation.ListDetail)
        component.open(LibraryConfig.DeckDetail("d1")) {}

        var result: NavigationRequestStatus? = null
        component.back { result = it }

        assertEquals(
            NavigationRequestStatus.Unhandled,
            result,
            "the list stays visible, so back is the shell's to handle",
        )
        assertEquals(
            LibraryConfig.DeckDetail("d1"),
            component.panels.value.details
                ?.configuration,
        )
    }

    @Test
    fun `a wide detail close action closes the open deck`() {
        val component = buildComponent(target = null, contentPresentation = AppContentPresentation.ListDetail)
        component.open(LibraryConfig.DeckDetail("d1")) {}

        component.panels.value.details
            ?.instance
            ?.onAction(LibraryDeckDetailAction.Close)

        assertNull(component.panels.value.details)
    }

    @Test
    fun `re-selecting the section returns to the list on a compact layout`() {
        val component = buildComponent(target = LibraryConfig.DeckDetail("d1"))

        component.open(LibraryConfig.Home) {}

        assertNull(component.panels.value.details, "opening Home closes the deck in single-pane")
    }

    @Test
    fun `re-selecting the section keeps the open deck on a wide layout`() {
        val component = buildComponent(target = null, contentPresentation = AppContentPresentation.ListDetail)
        component.open(LibraryConfig.DeckDetail("d1")) {}

        component.open(LibraryConfig.Home) {}

        assertEquals(
            LibraryConfig.DeckDetail("d1"),
            component.panels.value.details
                ?.configuration,
            "the list is already visible beside the deck, so Home is a no-op",
        )
    }

    @Test
    fun `collapsing to a compact layout lets back close the open deck`() {
        val fixture = buildFixture(target = null, contentPresentation = AppContentPresentation.ListDetail)
        val component = fixture.component
        component.open(LibraryConfig.DeckDetail("d1")) {}

        // Wide: back leaves the deck open (the list is visible).
        var wide: NavigationRequestStatus? = null
        component.back { wide = it }
        assertEquals(NavigationRequestStatus.Unhandled, wide)
        assertEquals(
            LibraryConfig.DeckDetail("d1"),
            component.panels.value.details
                ?.configuration,
        )

        fixture.componentContext.setContentPresentation(AppContentPresentation.SinglePane)

        assertEquals(ChildPanelsMode.SINGLE, component.panels.value.mode, "resize drives setMode on a live component")
        var compact: NavigationRequestStatus? = null
        component.back { compact = it }
        assertEquals(NavigationRequestStatus.Handled, compact, "the deck now covers the list, so back closes it")
        assertNull(component.panels.value.details)
    }

    @Test
    fun `expanding to a wide layout keeps the open deck and drives the dual mode`() {
        val fixture =
            buildFixture(
                target = LibraryConfig.DeckDetail("d1"),
                contentPresentation = AppContentPresentation.SinglePane,
            )
        val component = fixture.component
        assertEquals(ChildPanelsMode.SINGLE, component.panels.value.mode)

        fixture.componentContext.setContentPresentation(AppContentPresentation.ListDetail)

        assertEquals(ChildPanelsMode.DUAL, component.panels.value.mode, "resize drives setMode on a live component")
        assertEquals(
            LibraryConfig.DeckDetail("d1"),
            component.panels.value.details
                ?.configuration,
            "the open deck survives the resize",
        )
        var result: NavigationRequestStatus? = null
        component.back { result = it }
        assertEquals(NavigationRequestStatus.Unhandled, result, "the list is visible again, so back falls through")
        assertEquals(
            LibraryConfig.DeckDetail("d1"),
            component.panels.value.details
                ?.configuration,
        )
    }

    private fun buildComponent(
        target: LibraryConfig?,
        contentPresentation: AppContentPresentation = AppContentPresentation.SinglePane,
    ): DefaultLibrarySectionComponent =
        buildFixture(target = target, contentPresentation = contentPresentation).component

    private fun buildFixture(
        target: LibraryConfig?,
        contentPresentation: AppContentPresentation = AppContentPresentation.SinglePane,
    ): LibrarySectionFixture {
        val lifecycle = LifecycleRegistry()
        val componentContext =
            RootComponentContext(
                delegate = DefaultComponentContext(lifecycle),
                // LibraryConfig panels carry their own serializers; the context one is unused here.
                screenConfigSerializer = PolymorphicSerializer(ScreenConfig::class),
            )
        lifecycle.resume()
        val component =
            DefaultLibrarySectionComponent(
                componentContext = componentContext,
                target = target,
                libraryHomeComponentFactory = { FakeLibraryHomeComponent() },
                libraryDeckDetailComponentFactory = { _, args -> FakeLibraryDeckDetailComponent(args) },
            )
        componentContext.setContentPresentation(contentPresentation)
        return LibrarySectionFixture(component, componentContext)
    }

    private data class LibrarySectionFixture(
        val component: DefaultLibrarySectionComponent,
        val componentContext: RootComponentContext,
    )

    private class FakeLibraryHomeComponent : LibraryHomeComponent {
        override val uiState: StateFlow<LibraryHomeUiState> = MutableStateFlow(LibraryHomeUiState())

        override fun onAction(action: LibraryHomeAction) = Unit
    }

    private class FakeLibraryDeckDetailComponent(
        private val args: LibraryDeckDetailComponent.Args,
    ) : LibraryDeckDetailComponent {
        override val uiState: StateFlow<LibraryDeckDetailUiState> = MutableStateFlow(LibraryDeckDetailUiState())

        override fun onAction(action: LibraryDeckDetailAction) {
            if (action == LibraryDeckDetailAction.Close) {
                args.onClose()
            }
        }
    }
}
