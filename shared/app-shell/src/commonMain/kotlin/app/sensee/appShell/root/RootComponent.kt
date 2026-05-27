package app.sensee.appShell.root

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.context.AppContentPresentation
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.platform.Platform
import app.sensee.ui.designSystem.theme.SenseeThemeMode
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalDecomposeApi::class)
public interface RootComponent :
    NavigationDispatcher,
    WebNavigationOwner {
    public val stack: Value<ChildStack<ScreenConfig, AppComponent>>

    public val platform: Platform

    public val themeMode: StateFlow<SenseeThemeMode>

    public fun setContentPresentation(presentation: AppContentPresentation)

    public fun interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            deepLink: String?,
        ): RootComponent
    }
}
