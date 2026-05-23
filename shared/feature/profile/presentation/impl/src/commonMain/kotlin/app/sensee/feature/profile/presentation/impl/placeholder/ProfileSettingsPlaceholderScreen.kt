package app.sensee.feature.profile.presentation.impl.placeholder

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.profile.presentation.impl.home.ProfileHomeTextKeys
import app.sensee.feature.profile.presentation.impl.home.categoryTitleKey
import app.sensee.feature.profile.presentation.impl.home.rememberProfileHomeTextProvider
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.ui.designSystem.component.UnimplementedScreen

@Composable
internal fun ProfileSettingsPlaceholderScreen(
    config: ProfileConfig.Settings,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberProfileHomeTextProvider(),
) {
    UnimplementedScreen(
        title = textProvider.text(config.categoryTitleKey()),
        description = textProvider.text(ProfileHomeTextKeys.PlaceholderDescription),
        modifier = modifier,
    )
}
