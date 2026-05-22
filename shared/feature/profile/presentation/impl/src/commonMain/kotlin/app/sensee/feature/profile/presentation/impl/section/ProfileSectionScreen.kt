package app.sensee.feature.profile.presentation.impl.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.sensee.core.decompose.AppComponent
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileSectionComponent
import app.sensee.feature.profile.presentation.api.ProfileSettingsPlaceholderComponent
import app.sensee.feature.profile.presentation.impl.aisettings.ProfileAiSettingsScreen
import app.sensee.feature.profile.presentation.impl.home.ProfileHomeScreen
import app.sensee.feature.profile.presentation.impl.home.ProfileHomeTextKeys
import app.sensee.feature.profile.presentation.impl.home.categoryTitleKey
import app.sensee.feature.profile.presentation.impl.home.rememberProfileHomeTextProvider
import app.sensee.feature.profile.presentation.impl.placeholder.ProfileSettingsPlaceholderScreen
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.ui.adaptive.AppChildPanels
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.topBar.SenseeTopBar
import app.sensee.ui.designSystem.component.topBar.SenseeTopBarIconButton
import app.sensee.ui.designSystem.icons.ArrowBack24px
import app.sensee.ui.designSystem.icons.Close24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.composeunstyled.Text

@OptIn(ExperimentalDecomposeApi::class)
@Composable
public fun ProfileSectionScreen(
    component: ProfileSectionComponent,
    modifier: Modifier = Modifier,
) {
    val textProvider = rememberProfileHomeTextProvider()

    AppChildPanels(
        panels = component.panels,
        modifier = modifier,
        main = { mainChild, detailChild ->
            ProfileHomeScreen(
                component = mainChild.instance,
                selectedConfig = detailChild?.configuration,
                modifier = Modifier.fillMaxSize(),
                textProvider = textProvider,
            )
        },
        detail = { detailChild, compact ->
            ProfileDetailPane(
                config = detailChild.configuration,
                component = detailChild.instance,
                compact = compact,
                onClose = { component.back() },
                modifier = Modifier.fillMaxSize(),
                textProvider = textProvider,
            )
        },
    )
}

@Composable
private fun ProfileDetailPane(
    config: ProfileConfig,
    component: AppComponent,
    compact: Boolean,
    onClose: () -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(SenseeTheme.colors.background)) {
        SenseeTopBar(
            title = { Text(text = textProvider.text(config.categoryTitleKey())) },
            navigation = {
                SenseeTopBarIconButton(
                    onClick = onClose,
                    accessibilityLabel =
                        textProvider.text(
                            if (compact) ProfileHomeTextKeys.Back else ProfileHomeTextKeys.Close,
                        ),
                    icon = {
                        SenseeIcon(
                            imageVector = if (compact) ArrowBack24px else Close24px,
                            contentDescription = null,
                        )
                    },
                )
            },
            showDivider = false,
        )
        when (component) {
            is ProfileAiSettingsComponent ->
                ProfileAiSettingsScreen(component = component, modifier = Modifier.fillMaxSize())

            is ProfileSettingsPlaceholderComponent ->
                ProfileSettingsPlaceholderScreen(
                    config = component.config,
                    modifier = Modifier.fillMaxSize(),
                )

            else -> error("Unknown profile detail child: ${component::class}")
        }
    }
}
