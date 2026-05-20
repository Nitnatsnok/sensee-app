package app.sensee.appShell.primary

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.navigation.BottomBarVisibilityOwner
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.navigation.NodeConfig
import app.sensee.core.decompose.navigation.ScreenConfig
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalDecomposeApi::class)
public interface PrimaryShellComponent :
    AppComponent,
    NavigationDispatcher,
    BottomBarVisibilityOwner,
    WebNavigationOwner {
    public val stack: Value<ChildStack<NodeConfig<ScreenConfig>, AppComponent>>
    public val selectedSection: StateFlow<PrimarySection>

    public fun selectSection(section: PrimarySection)

    public interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            target: ScreenConfig? = null,
        ): PrimaryShellComponent
    }
}
