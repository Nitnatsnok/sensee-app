package app.sensee.feature.startup.presentation.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback
import app.sensee.feature.startup.presentation.api.StartupComponent
import app.sensee.feature.startup.presentation.api.StartupState
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

@Composable
public fun StartupScreen(
    component: StartupComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberStartupTextProvider(),
) {
    val state by component.state.collectAsState()

    LaunchedEffect(component, state) {
        if (state is StartupState.Loaded) component.onFinished()
    }

    val colors = SenseeTheme.colors
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            StartupState.Loading,
            StartupState.Loaded,
            -> LoadingPanel(textProvider = textProvider)
            is StartupState.Failed ->
                ErrorPanel(
                    textProvider = textProvider,
                    onRetry = component::retry,
                )
        }
    }
}

@Composable
private fun LoadingPanel(textProvider: TextProvider) {
    val colors = SenseeTheme.colors
    val textStyles = SenseeTheme.typography
    val spacing = SenseeTheme.spacing
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(
            text = textProvider.text(StartupTextKeys.AppName),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            style = textStyles.titleLarge,
        )
        Text(
            text = textProvider.text(StartupTextKeys.StatusStarting),
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            style = textStyles.bodyMedium,
        )
    }
}

@Composable
private fun ErrorPanel(
    textProvider: TextProvider,
    onRetry: () -> Unit,
) {
    val colors = SenseeTheme.colors
    val textStyles = SenseeTheme.typography
    val spacing = SenseeTheme.spacing
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        Text(
            text = textProvider.text(StartupTextKeys.AppName),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            style = textStyles.titleLarge,
        )
        Text(
            text = textProvider.text(StartupTextKeys.ErrorTitle),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            style = textStyles.titleSmall,
        )
        Text(
            text = textProvider.text(StartupTextKeys.ErrorDescription),
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            style = textStyles.bodyMedium,
        )
        SenseeButton(onClick = onRetry) {
            Text(text = textProvider.text(StartupTextKeys.Retry))
        }
    }
}

@Composable
private fun rememberStartupTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultStartupTextProvider.withFallback(parent) }
}
