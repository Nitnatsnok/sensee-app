package app.sensee.appShell.primary

import kotlin.test.Test
import kotlin.test.assertEquals

class PrimaryShellBackPolicyTest {
    @Test
    fun `multi-item stack pops the top section so back walks tab history`() {
        val destinations =
            listOf(
                PrimarySectionConfig.HomeSection(),
                PrimarySectionConfig.LibrarySection(),
            )

        assertEquals(PrimaryShellBackAction.Pop, primaryShellBackAction(destinations))
    }

    @Test
    fun `single non-Home item routes to Home so the user does not exit from a deep landing`() {
        val destinations = listOf(PrimarySectionConfig.LibrarySection())

        assertEquals(PrimaryShellBackAction.ReplaceWithHomeRoot, primaryShellBackAction(destinations))
    }

    @Test
    fun `single Home item is unhandled so the system back can exit the app`() {
        val destinations = listOf(PrimarySectionConfig.HomeSection())

        assertEquals(PrimaryShellBackAction.Unhandled, primaryShellBackAction(destinations))
    }

    @Test
    fun `LRU rotation leaves Home above non-Home and still pops first`() {
        val destinations =
            listOf(
                PrimarySectionConfig.LibrarySection(),
                PrimarySectionConfig.HomeSection(),
            )

        assertEquals(PrimaryShellBackAction.Pop, primaryShellBackAction(destinations))
    }
}
