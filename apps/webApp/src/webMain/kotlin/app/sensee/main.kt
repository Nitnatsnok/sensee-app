package app.sensee

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import app.sensee.appShell.App
import app.sensee.appShell.WebPlatformEnvironment
import app.sensee.appShell.root.RootComponent
import app.sensee.appShell.root.createAppRoot
import app.sensee.core.platform.PlatformEnvironment
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.webhistory.withWebHistory
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.resume

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()

    val rootComponent =
        createWebRoot(
            lifecycle = lifecycle,
            platformEnvironment = createEnvironment(),
        )

    lifecycle.create()
    lifecycle.resume()

    ComposeViewport(configure = { isA11YEnabled = IS_WEB_ACCESSIBILITY_ENABLED }) {
        App(rootComponent)
    }
}

private const val IS_WEB_ACCESSIBILITY_ENABLED = false

internal fun createEnvironment(): PlatformEnvironment = WebPlatformEnvironment()

/**
 * Builds the root component wrapped in Decompose Web Navigation. `withWebHistory`
 * supplies the browser-history `StateKeeper` and the cold-start deep link.
 */
@OptIn(ExperimentalDecomposeApi::class)
internal fun createWebRoot(
    lifecycle: LifecycleRegistry,
    platformEnvironment: PlatformEnvironment,
): RootComponent =
    withWebHistory { stateKeeper, deepLink ->
        createAppRoot(
            componentContext =
                DefaultComponentContext(
                    lifecycle = lifecycle,
                    stateKeeper = stateKeeper,
                ),
            platformEnvironment = platformEnvironment,
            deepLink = deepLink,
        )
    }
