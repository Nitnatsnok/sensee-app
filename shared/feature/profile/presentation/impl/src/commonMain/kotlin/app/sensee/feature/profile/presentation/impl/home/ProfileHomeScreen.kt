package app.sensee.feature.profile.presentation.impl.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.component.layout.SenseeSurface
import app.sensee.ui.designSystem.component.layout.SenseeSurfaceDefaults
import app.sensee.ui.designSystem.icons.Exercise24px
import app.sensee.ui.designSystem.icons.Handyman24px
import app.sensee.ui.designSystem.icons.Language24px
import app.sensee.ui.designSystem.icons.SettingsApplications24px
import app.sensee.ui.designSystem.icons.WandStars24px
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import com.composeunstyled.Text

@Composable
public fun ProfileHomeScreen(
    component: ProfileHomeComponent,
    selectedConfig: ProfileConfig?,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberProfileHomeTextProvider(),
) {
    val colors = SenseeTheme.colors
    val spacing = SenseeTheme.spacing
    val layoutMetrics = LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = layoutMetrics.screenHorizontalPadding,
                    top = layoutMetrics.screenVerticalPadding,
                    end = layoutMetrics.screenHorizontalPadding,
                    bottom = layoutMetrics.screenVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            item {
                SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
                    Text(
                        text = textProvider.text(ProfileHomeTextKeys.GroupSettings),
                        color = colors.textPrimary,
                        style = SenseeTheme.typography.titleLarge,
                    )
                }
            }
            items(
                items = component.items,
                key = { it.categoryTitleKey().value },
            ) { config ->
                SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
                    ProfileMenuRow(
                        title = textProvider.text(config.categoryTitleKey()),
                        icon = config.categoryIcon(),
                        selected = config == selectedConfig,
                        onClick = { component.onItemSelected(config) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun rememberProfileHomeTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultProfileHomeTextProvider.withFallback(parent) }
}

@Composable
private fun ProfileMenuRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = SenseeTheme.colors
    val spacing = SenseeTheme.spacing
    SenseeSurface(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MenuRowMinHeight),
        onClick = onClick,
        colors =
            SenseeSurfaceDefaults.colors(
                container =
                    if (selected) colors.surfaceContainerHighest else colors.surfaceContainerLow,
            ),
        contentPadding = PaddingValues(horizontal = spacing.medium, vertical = spacing.small),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            SenseeIcon(imageVector = icon, contentDescription = null)
            Text(text = title, style = SenseeTheme.typography.titleMedium)
        }
    }
}

private val MenuRowMinHeight = 48.dp

private fun ProfileConfig.Settings.categoryIcon(): ImageVector =
    when (this) {
        ProfileConfig.Settings.App -> SettingsApplications24px
        ProfileConfig.Settings.Learning -> Language24px
        ProfileConfig.Settings.Practice -> Exercise24px
        ProfileConfig.Settings.Ai -> WandStars24px
        ProfileConfig.Settings.Experimental -> Handyman24px
    }
