package app.sensee

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.sensee.appShell.App
import app.sensee.appShell.DesktopPlatformEnvironment
import app.sensee.appShell.root.createAppRoot
import app.sensee.database.DatabaseConfig
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import java.awt.Dimension

// Keep the desktop window above the smallest size supported by the shared
// adaptive layout; below this, navigation and practice surfaces collapse.
private const val MIN_WINDOW_WIDTH_DP = 360
private const val MIN_WINDOW_HEIGHT_DP = 640

fun main() {
    val lifecycleRegistry = LifecycleRegistry()

    val rootComponent =
        runOnUiThread {
            createAppRoot(
                componentContext =
                    DefaultComponentContext(
                        lifecycle = lifecycleRegistry,
                    ),
                platformEnvironment = DesktopPlatformEnvironment(),
                // Destructive: wipes the local DB on schema drift. Dev-only.
                databaseConfig = DatabaseConfig(resetOnSchemaMigration = true),
            )
        }

    application {
        val windowState = rememberWindowState()

        LifecycleController(
            lifecycleRegistry = lifecycleRegistry,
            windowState = windowState,
        )

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Sensee",
        ) {
            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(MIN_WINDOW_WIDTH_DP, MIN_WINDOW_HEIGHT_DP)
            }
            App(rootComponent)
        }
    }
}
