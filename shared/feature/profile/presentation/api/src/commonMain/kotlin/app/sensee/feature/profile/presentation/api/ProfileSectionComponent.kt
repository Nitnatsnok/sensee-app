package app.sensee.feature.profile.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.navigation.BottomBarVisibilityOwner
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value

public interface ProfileSectionComponent :
    AppComponent,
    NavigationDispatcher,
    BottomBarVisibilityOwner {
    public val stack: Value<ChildStack<ScreenConfig, AppComponent>>

    public interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            target: ProfileConfig? = null,
        ): ProfileSectionComponent
    }
}
