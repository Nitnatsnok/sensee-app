package app.sensee

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.sensee.appShell.App
import app.sensee.appShell.DesktopPlatformEnvironment
import app.sensee.appShell.desktop.SenseeDesktopWindowFrame
import app.sensee.appShell.root.RootComponent
import app.sensee.appShell.root.createAppRoot
import app.sensee.database.DatabaseConfig
import app.sensee.desktop.installWindowsWindowDecoration
import app.sensee.desktop.isWindows
import app.sensee.desktop.minimizeWindow
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import java.awt.Dimension

// Keep the desktop window above the smallest size supported by the shared
// adaptive layout; below this, navigation and practice surfaces collapse.
private const val MIN_WINDOW_WIDTH_DP = 360
private const val MIN_WINDOW_HEIGHT_DP = 640

private const val WINDOW_TITLE = "Sensee"

private fun createDesktopRoot(lifecycleRegistry: LifecycleRegistry): RootComponent =
    runOnUiThread {
        createAppRoot(
            componentContext = DefaultComponentContext(lifecycle = lifecycleRegistry),
            platformEnvironment = DesktopPlatformEnvironment(),
            // Destructive: wipes the local DB on schema drift. Dev-only.
            databaseConfig = DatabaseConfig(resetOnSchemaMigration = true),
        )
    }

fun main() {
    val lifecycleRegistry = LifecycleRegistry()
    val rootComponent = createDesktopRoot(lifecycleRegistry)

    application {
        val windowState = rememberWindowState()

        LifecycleController(
            lifecycleRegistry = lifecycleRegistry,
            windowState = windowState,
        )

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = WINDOW_TITLE,
            icon = classpathPainter("icon.png"),
            // Custom chrome only on Windows; macOS and Linux keep their native
            // window decorations — the reliable, idiomatic look there.
            undecorated = isWindows,
            transparent = false,
            resizable = true,
        ) {
            SenseeWindowContent(
                windowState = windowState,
                rootComponent = rootComponent,
                onClose = ::exitApplication,
            )
        }
    }
}

@Composable
private fun FrameWindowScope.SenseeWindowContent(
    windowState: WindowState,
    rootComponent: RootComponent,
    onClose: () -> Unit,
) {
    LaunchedEffect(Unit) {
        window.minimumSize = Dimension(MIN_WINDOW_WIDTH_DP, MIN_WINDOW_HEIGHT_DP)
    }
    if (isWindows) {
        remember(window) { installWindowsWindowDecoration(window) }
        App(
            rootComponent = rootComponent,
            contentFrame = { content ->
                SenseeDesktopWindowFrame(
                    windowState = windowState,
                    title = WINDOW_TITLE,
                    onMinimize = { minimizeWindow(window, windowState) },
                    onClose = onClose,
                    content = content,
                )
            },
        )
    } else {
        // macOS / Linux: native window decorations, no custom chrome.
        App(rootComponent = rootComponent)
    }
}

// Compose Resources is wired only in shared/ui/design-system for fonts, so a single
// classpath PNG is loaded directly via Skia — the migration target suggested by
// https://github.com/JetBrains/compose-multiplatform-core/pull/1457.
private object ResourceLoader

@Composable
private fun classpathPainter(path: String): Painter =
    remember(path) {
        val bytes =
            checkNotNull(ResourceLoader.javaClass.classLoader.getResourceAsStream(path)) {
                "Resource '$path' not found on classpath"
            }.use { it.readBytes() }
        BitmapPainter(bytes.decodeToImageBitmap())
    }
