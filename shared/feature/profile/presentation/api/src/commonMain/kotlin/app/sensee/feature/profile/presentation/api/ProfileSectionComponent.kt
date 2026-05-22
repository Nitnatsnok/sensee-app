package app.sensee.feature.profile.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.navigation.BottomBarVisibilityOwner
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.value.Value

/**
 * Profile section panels: the [ProfileConfig.Home] menu as the main panel and a
 * settings-category screen as the optional detail panel. On wide layouts both
 * panels render side by side; on compact layouts one at a time.
 */
@OptIn(ExperimentalDecomposeApi::class)
public typealias ProfileChildPanels =
    ChildPanels<
        ProfileConfig.Home,
        ProfileHomeComponent,
        ProfileConfig,
        AppComponent,
        Nothing,
        Nothing,
    >

@OptIn(ExperimentalDecomposeApi::class)
public interface ProfileSectionComponent :
    AppComponent,
    NavigationDispatcher,
    BottomBarVisibilityOwner {
    public val panels: Value<ProfileChildPanels>

    public interface Factory {
        public fun create(
            componentContext: AppComponentContext,
            target: ProfileConfig? = null,
        ): ProfileSectionComponent
    }
}
