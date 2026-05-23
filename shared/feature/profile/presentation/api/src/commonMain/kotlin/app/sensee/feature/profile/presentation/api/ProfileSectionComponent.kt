package app.sensee.feature.profile.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.decompose.navigation.BottomBarVisibilityOwner
import app.sensee.core.decompose.navigation.NavigationDispatcher
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.feature.profile.presentation.navigationApi.ProfileExtraConfig
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.ChildPanels
import com.arkivanov.decompose.value.Value

/**
 * Profile section panels: the [ProfileConfig.Home] menu as the main panel, a
 * settings-category screen as the optional detail panel, and an optional extra
 * panel for supporting content (currently the topic picker). On the largest
 * layout all three panels render side by side; on list-detail layouts the
 * extra panel falls back to a modal sheet; on compact layouts panels are
 * presented one at a time.
 */
@OptIn(ExperimentalDecomposeApi::class)
public typealias ProfileChildPanels =
    ChildPanels<
        ProfileConfig.Home,
        ProfileHomeComponent,
        ProfileConfig.Settings,
        AppComponent,
        ProfileExtraConfig,
        AppComponent,
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
