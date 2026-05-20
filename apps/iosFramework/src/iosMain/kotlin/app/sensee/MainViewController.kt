package app.sensee

import androidx.compose.ui.window.ComposeUIViewController
import app.sensee.appShell.App
import app.sensee.appShell.IosRootHolder
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused")
fun MainViewController(rootHolder: IosRootHolder): UIViewController =
    ComposeUIViewController {
        App(rootComponent = rootHolder.rootComponent)
    }
