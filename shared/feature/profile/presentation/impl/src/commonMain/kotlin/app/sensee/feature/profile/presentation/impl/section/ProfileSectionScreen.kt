package app.sensee.feature.profile.presentation.impl.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.core.decompose.AppComponent
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileLearningSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileSectionComponent
import app.sensee.feature.profile.presentation.api.ProfileSettingsPlaceholderComponent
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerAction
import app.sensee.feature.profile.presentation.api.ProfileTopicPickerComponent
import app.sensee.feature.profile.presentation.impl.aisettings.ProfileAiSettingsScreen
import app.sensee.feature.profile.presentation.impl.home.ProfileHomeScreen
import app.sensee.feature.profile.presentation.impl.home.ProfileHomeTextKeys
import app.sensee.feature.profile.presentation.impl.home.categoryTitleKey
import app.sensee.feature.profile.presentation.impl.home.rememberProfileHomeTextProvider
import app.sensee.feature.profile.presentation.impl.learningsettings.ProfileLearningSettingsScreen
import app.sensee.feature.profile.presentation.impl.learningsettings.picker.ProfileTopicPickerScreen
import app.sensee.feature.profile.presentation.impl.placeholder.ProfileSettingsPlaceholderScreen
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.ui.adaptive.AppChildPanels
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.layout.SenseeModalBottomSheet
import app.sensee.ui.designSystem.component.layout.SenseeSurface
import app.sensee.ui.designSystem.component.topBar.SenseeTopBar
import app.sensee.ui.designSystem.component.topBar.SenseeTopBarIconButton
import app.sensee.ui.designSystem.icons.ArrowBack24px
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics
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
        main = { mainChild, detailChild, _ ->
            ProfileHomeScreen(
                component = mainChild.instance,
                selectedConfig = detailChild?.configuration,
                modifier = Modifier.fillMaxSize(),
                textProvider = textProvider,
            )
        },
        detail = { detailChild, _, compact ->
            ProfileDetailPane(
                config = detailChild.configuration,
                component = detailChild.instance,
                compact = compact,
                onClose = { component.back() },
                modifier = Modifier.fillMaxSize(),
                textProvider = textProvider,
            )
        },
        extra = { extraChild, compact ->
            // Inline path renders an active child; the compact (sheet) path is
            // also invoked when the child is null so the sheet has a frame to
            // animate out in.
            val picker = extraChild?.instance as? ProfileTopicPickerComponent
            if (compact) {
                TopicPickerSheet(
                    component = picker,
                    visible = extraChild != null,
                )
            } else if (picker != null) {
                ProfileTopicPickerScreen(
                    component = picker,
                    modifier = Modifier.fillMaxSize(),
                    compact = false,
                )
            }
        },
    )
}

@Composable
private fun ProfileDetailPane(
    config: ProfileConfig.Settings,
    component: AppComponent,
    compact: Boolean,
    onClose: () -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    if (compact) {
        val title = textProvider.text(config.categoryTitleKey())
        val colors = SenseeTheme.colors
        Column(modifier = modifier) {
            SenseeTopBar(
                title = { Text(text = title) },
                navigation = {
                    SenseeTopBarIconButton(
                        onClick = onClose,
                        accessibilityLabel = textProvider.text(ProfileHomeTextKeys.Back),
                        icon = {
                            SenseeIcon(imageVector = ArrowBack24px, contentDescription = null)
                        },
                    )
                },
            )
            ProfileDetailBody(component = component, modifier = Modifier.fillMaxSize().background(colors.background))
        }
    } else {
        val spacing = SenseeTheme.spacing
        SenseeSurface(
            modifier =
                modifier
                    .statusBarsPadding()
                    .padding(top = spacing.medium, bottom = spacing.medium, end = spacing.medium),
            shape = SenseeTheme.shapes.large,
            contentPadding = PaddingValues(0.dp),
            borderWidth = 0.dp,
        ) {
            CompositionLocalProvider(
                LocalSenseeAdaptiveLayoutMetrics provides profilePaneLayoutMetrics(),
            ) {
                ProfileDetailBody(component = component, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun profilePaneLayoutMetrics(): SenseeAdaptiveLayoutMetrics {
    val layout = SenseeTheme.layout
    return SenseeAdaptiveLayoutMetrics(
        screenHorizontalPadding = layout.screenHorizontalPaddingCompact,
        screenVerticalPadding = layout.screenVerticalPaddingCompact,
        contentMaxWidth = Dp.Infinity,
        paneGap = layout.paneGap,
    )
}

@Composable
private fun ProfileDetailBody(
    component: AppComponent,
    modifier: Modifier = Modifier,
) {
    when (component) {
        is ProfileAiSettingsComponent ->
            ProfileAiSettingsScreen(component = component, modifier = modifier)

        is ProfileLearningSettingsComponent ->
            ProfileLearningSettingsScreen(component = component, modifier = modifier)

        is ProfileSettingsPlaceholderComponent ->
            ProfileSettingsPlaceholderScreen(
                config = component.config,
                modifier = modifier,
            )

        else -> error("Unknown profile detail child: ${component::class}")
    }
}

@Composable
private fun TopicPickerSheet(
    component: ProfileTopicPickerComponent?,
    visible: Boolean,
) {
    var lastComponent by remember { mutableStateOf<ProfileTopicPickerComponent?>(null) }
    if (component != null && component != lastComponent) {
        lastComponent = component
    }
    val renderTarget = component ?: lastComponent

    SenseeModalBottomSheet(
        visible = visible,
        onDismissRequest = { renderTarget?.onAction(ProfileTopicPickerAction.Close) },
        contentPadding = PaddingValues(0.dp),
    ) {
        if (renderTarget == null) return@SenseeModalBottomSheet
        ProfileTopicPickerScreen(
            component = renderTarget,
            modifier = Modifier.fillMaxWidth(),
            compact = true,
        )
    }
}
