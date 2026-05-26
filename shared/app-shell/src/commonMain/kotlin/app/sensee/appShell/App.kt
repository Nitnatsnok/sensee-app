package app.sensee.appShell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import app.sensee.appShell.root.RootComponent
import app.sensee.appShell.root.RootScreen
import app.sensee.core.decompose.context.AppContentPresentation
import app.sensee.ui.adaptive.ContentLayoutType
import app.sensee.ui.adaptive.LocalAdaptiveInfo

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
    AppComposeEnvironment(platform = rootComponent.platform) {
        val contentPresentation = LocalAdaptiveInfo.current.contentLayoutType.toContentPresentation()
        SideEffect {
            rootComponent.setContentPresentation(contentPresentation)
        }
        contentFrame {
            RootScreen(
                component = rootComponent,
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}

private fun ContentLayoutType.toContentPresentation(): AppContentPresentation =
    when (this) {
        ContentLayoutType.SinglePane -> AppContentPresentation.SinglePane
        ContentLayoutType.ListDetail -> AppContentPresentation.ListDetail
        ContentLayoutType.SupportingPane -> AppContentPresentation.SupportingPane
    }
