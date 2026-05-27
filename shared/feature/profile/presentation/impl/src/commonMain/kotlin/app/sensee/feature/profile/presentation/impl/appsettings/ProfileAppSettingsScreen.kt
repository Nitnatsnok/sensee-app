package app.sensee.feature.profile.presentation.impl.appsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback
import app.sensee.feature.profile.presentation.api.ProfileAppSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileAppSettingsComponent
import app.sensee.settings.domain.AppThemeMode
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.component.selectField.SenseeSelectField
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import com.composeunstyled.Text
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun ProfileAppSettingsScreen(
    component: ProfileAppSettingsComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberProfileAppSettingsTextProvider(),
) {
    val uiState by component.uiState.collectAsState()
    val spacing = SenseeTheme.spacing
    val layoutMetrics = LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = layoutMetrics.screenHorizontalPadding,
                        top = layoutMetrics.screenVerticalPadding,
                        end = layoutMetrics.screenHorizontalPadding,
                        bottom = layoutMetrics.screenVerticalPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
                Text(
                    text = textProvider.text(ProfileAppSettingsTextKeys.SectionAppearance),
                    color = SenseeTheme.colors.textPrimary,
                    style = SenseeTheme.typography.titleLarge,
                )
            }
            SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
                SenseeSelectField(
                    label = textProvider.text(ProfileAppSettingsTextKeys.ThemeMode),
                    selected = uiState.themeMode,
                    options = ThemeModeOptions,
                    optionLabel = { textProvider.text(it.labelKey()) },
                    onSelect = { themeMode ->
                        component.onAction(ProfileAppSettingsAction.SetThemeMode(themeMode))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun rememberProfileAppSettingsTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultProfileAppSettingsTextProvider.withFallback(parent) }
}

private val ThemeModeOptions: ImmutableList<AppThemeMode> =
    persistentListOf(
        AppThemeMode.System,
        AppThemeMode.Light,
        AppThemeMode.Dark,
    )
