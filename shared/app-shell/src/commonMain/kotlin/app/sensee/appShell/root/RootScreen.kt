package app.sensee.appShell.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.appShell.primary.PrimaryShellComponent
import app.sensee.appShell.primary.PrimaryShellScreen
import app.sensee.feature.startup.presentation.api.StartupComponent
import app.sensee.feature.startup.presentation.impl.StartupScreen
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation

@Composable
public fun RootScreen(
    component: RootComponent,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = component.stack,
        modifier = modifier,
        animation = stackAnimation(fade()),
    ) { child ->
        when (val instance = child.instance) {
            is StartupComponent -> {
                StartupScreen(
                    component = instance,
                )
            }

            is PrimaryShellComponent -> {
                PrimaryShellScreen(
                    component = instance,
                )
            }

            else -> {
                error("Unknown root child: ${instance::class}")
            }
        }
    }
}
