package app.sensee.appShell.primary

import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.feature.home.presentation.navigationApi.HomeConfig
import app.sensee.feature.library.presentation.navigationApi.LibraryConfig
import app.sensee.feature.practice.presentation.navigationApi.PracticeConfig
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.feature.vocabularyEditor.presentation.navigationApi.VocabularyEditorConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrimarySectionConfigMappingTest {
    @Test
    fun `each config family maps to its own section carrying the target`() {
        val deck = PracticeConfig.DeckPractice(deckId = "d1")

        assertEquals(PrimarySectionConfig.HomeSection(HomeConfig.Home), HomeConfig.Home.toPrimarySectionConfig())
        assertEquals(PrimarySectionConfig.PracticeSection(deck), deck.toPrimarySectionConfig())
        assertEquals(
            PrimarySectionConfig.LibrarySection(LibraryConfig.Home),
            LibraryConfig.Home.toPrimarySectionConfig(),
        )
        assertEquals(
            PrimarySectionConfig.ProfileSection(ProfileConfig.Home),
            ProfileConfig.Home.toPrimarySectionConfig(),
        )
    }

    @Test
    fun `a non-Home vocabulary config maps to its section and not silently to Home`() {
        assertEquals(
            PrimarySectionConfig.VocabularyEditor(VocabularyEditorConfig.QuickCapture),
            VocabularyEditorConfig.QuickCapture.toPrimarySectionConfig(),
        )
    }

    @Test
    fun `no root target bootstraps to Home`() {
        assertEquals(PrimarySectionConfig.HomeSection(), (null as ScreenConfig?).toPrimarySectionConfig())
    }

    @Test
    fun `an unknown config is a wiring error rather than a silent Home redirect`() {
        val unknown = object : ScreenConfig {}
        assertFailsWith<IllegalStateException> { unknown.toPrimarySectionConfig() }
    }

    @Test
    fun `deep link path resolves to the section landing`() {
        assertEquals(HomeConfig.Home, WebSectionRoute.landingForPath(emptyList(), emptyMap()))
        assertEquals(HomeConfig.Home, WebSectionRoute.landingForPath(listOf("bogus"), emptyMap()))
        assertEquals(LibraryConfig.Home, WebSectionRoute.landingForPath(listOf("library"), emptyMap()))
        assertEquals(PracticeConfig.Home, WebSectionRoute.landingForPath(listOf("practice"), emptyMap()))
    }

    @Test
    fun `deep link into a Practice screen resolves the nested config`() {
        assertEquals(
            PracticeConfig.DeckPractice(deckId = "d1"),
            WebSectionRoute.landingForPath(listOf("practice", "deck", "d1"), emptyMap()),
        )
        assertEquals(
            PracticeConfig.DeckPractice(deckId = "d1", focusedCardId = "c9"),
            WebSectionRoute.landingForPath(listOf("practice", "deck", "d1"), mapOf("focus" to "c9")),
        )
        assertEquals(
            PracticeConfig.CardDetail(cardId = "c2"),
            WebSectionRoute.landingForPath(listOf("practice", "card", "c2"), emptyMap()),
        )
    }
}
