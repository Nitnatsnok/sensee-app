package app.sensee.feature.profile.presentation.impl.placeholder

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.feature.profile.presentation.api.ProfileSettingsPlaceholderComponent
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig

internal class DefaultProfileSettingsPlaceholderComponent(
    componentContext: AppComponentContext,
    override val config: ProfileConfig,
) : ProfileSettingsPlaceholderComponent,
    AppComponentContext by componentContext
