package app.sensee.feature.profile.presentation.impl.home

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@AssistedInject
public class DefaultProfileHomeComponent(
    @Assisted componentContext: AppComponentContext,
) : ProfileHomeComponent,
    AppComponentContext by componentContext {
    override val items: ImmutableList<ProfileConfig.Settings> = SettingsCategoryConfigs

    override fun onItemSelected(config: ProfileConfig.Settings) {
        navigation.open(config)
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<ProfileHomeComponent.Factory>(),
    )
    public fun interface Factory : ProfileHomeComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultProfileHomeComponent
    }
}

private val SettingsCategoryConfigs: ImmutableList<ProfileConfig.Settings> =
    persistentListOf(
        ProfileConfig.Settings.App,
        ProfileConfig.Settings.Learning,
        ProfileConfig.Settings.Practice,
        ProfileConfig.Settings.Ai,
        ProfileConfig.Settings.Experimental,
    )
