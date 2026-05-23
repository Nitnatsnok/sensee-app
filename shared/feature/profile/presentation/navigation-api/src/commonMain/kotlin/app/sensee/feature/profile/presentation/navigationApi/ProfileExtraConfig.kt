package app.sensee.feature.profile.presentation.navigationApi

import app.sensee.core.decompose.navigation.ScreenConfig
import kotlinx.serialization.Serializable

/**
 * Configs for the Profile section's third (extra) panel. The extra panel hosts
 * supporting-content like pickers that an active detail screen wants surfaced
 * alongside itself.
 */
@Serializable
public sealed interface ProfileExtraConfig : ScreenConfig {
    @Serializable
    public object TopicPicker : ProfileExtraConfig
}
