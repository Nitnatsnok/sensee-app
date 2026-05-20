package app.sensee.feature.practice.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.navigation.BottomBarVisibilityOwner
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.feature.practice.presentation.navigationApi.PracticeConfig
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.webhistory.WebNavigationOwner
import com.arkivanov.decompose.value.Value

@OptIn(ExperimentalDecomposeApi::class)
public interface PracticeSectionComponent :
    AppComponent,
    NavigationDispatcher,
    BottomBarVisibilityOwner,
    WebNavigationOwner {
    public val stack: Value<ChildStack<ScreenConfig, AppComponent>>

    public interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            target: PracticeConfig? = null,
        ): PracticeSectionComponent
    }
}
