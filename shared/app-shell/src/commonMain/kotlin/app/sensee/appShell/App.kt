package app.sensee.appShell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.appShell.root.RootComponent
import app.sensee.appShell.root.RootScreen

@Composable
public fun App(
    rootComponent: RootComponent,
    modifier: Modifier = Modifier,
) {
    AppComposeEnvironment {
        RootScreen(
            component = rootComponent,
            modifier = modifier.fillMaxSize(),
        )
    }
}
