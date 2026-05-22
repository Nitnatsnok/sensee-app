package app.sensee.appShell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.appShell.root.RootComponent
import app.sensee.appShell.root.RootScreen

/**
 * Decorator slot rendered between [AppComposeEnvironment] and [RootScreen].
 *
 * It runs inside the themed environment, so a frame may read `SenseeTheme` tokens.
 * The default is identity (renders [content] verbatim) — only desktop supplies a
 * real implementation, for the custom window chrome.
 */
public typealias AppContentFrame = @Composable (content: @Composable () -> Unit) -> Unit

@Composable
public fun App(
    rootComponent: RootComponent,
    modifier: Modifier = Modifier,
    contentFrame: AppContentFrame = { content -> content() },
) {
    AppComposeEnvironment {
        contentFrame {
            RootScreen(
                component = rootComponent,
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}
