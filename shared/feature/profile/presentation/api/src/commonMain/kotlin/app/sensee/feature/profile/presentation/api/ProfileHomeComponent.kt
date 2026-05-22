package app.sensee.feature.profile.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import kotlinx.collections.immutable.ImmutableList

/**
 * Profile section landing — the settings-category menu. [items] are the
 * settings-category configs shown under the "Settings" group; selecting one
 * opens its screen in the section's detail panel.
 */
public interface ProfileHomeComponent : AppComponent {
    public val items: ImmutableList<ProfileConfig>

    public fun onItemSelected(config: ProfileConfig)

    public fun interface Factory {
        public fun create(componentContext: AppComponentContext): ProfileHomeComponent
    }
}
