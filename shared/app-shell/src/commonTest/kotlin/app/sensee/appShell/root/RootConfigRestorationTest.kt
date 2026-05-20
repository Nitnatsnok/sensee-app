package app.sensee.appShell.root

import app.sensee.appShell.primary.PrimaryShellConfig
import app.sensee.appShell.primary.PrimaryShellConfigSerializersProvider
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.navigation.ScreenConfigSerializerFactory
import app.sensee.feature.home.presentation.navigationApi.HomeConfig
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that root stack configs survive a serialize/deserialize round-trip
 * through the assembled polymorphic serializer used for process-death restore.
 */
class RootConfigRestorationTest {
    private val serializer =
        ScreenConfigSerializerFactory(setOf(PrimaryShellConfigSerializersProvider())).create()

    private fun roundTrip(config: ScreenConfig): ScreenConfig =
        Json.decodeFromString(serializer, Json.encodeToString(serializer, config))

    @Test
    fun `StartupConfig round-trips so a cold-start stack restores`() {
        assertEquals(StartupConfig, roundTrip(StartupConfig))
    }

    @Test
    fun `PrimaryShellConfig round-trips so a deep target restores`() {
        val config = PrimaryShellConfig(target = null)
        assertEquals(config, roundTrip(config))
    }

    @Test
    fun `PrimaryShellConfig target is transient so a deep root config restores without crashing`() {
        // Decompose Web Navigation serializes the root nav key through its own
        // Json module. `target` is only a bootstrap hint; the active section is
        // restored from PrimaryShell's persisted stack.
        assertEquals(
            PrimaryShellConfig(target = null),
            roundTrip(PrimaryShellConfig(target = HomeConfig.Home)),
        )
    }
}
