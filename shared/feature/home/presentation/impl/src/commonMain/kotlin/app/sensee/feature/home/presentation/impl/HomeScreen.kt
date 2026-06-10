package app.sensee.feature.home.presentation.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.CommonTextKeys
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.home.presentation.api.HomeAction
import app.sensee.feature.home.presentation.api.HomeComponent
import app.sensee.feature.home.presentation.api.HomeUiState
import app.sensee.feature.home.presentation.impl.text.HomeTextKeys
import app.sensee.feature.home.presentation.impl.text.rememberHomeTextProvider
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.layout.SenseeErrorState
import app.sensee.ui.designSystem.component.layout.SenseeLoadingState
import app.sensee.ui.designSystem.component.layout.SenseeScreenContent
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.component.layout.SenseeSurface
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import com.composeunstyled.Text

@Composable
public fun HomeScreen(
    component: HomeComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberHomeTextProvider(),
) {
    val uiState by component.uiState.collectAsState()

    HomeContent(
        uiState = uiState,
        onAction = component::onAction,
        modifier = modifier.fillMaxSize(),
        textProvider = textProvider,
    )
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    val colors = SenseeTheme.colors

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
    ) {
        when (val loadingState = uiState.loadingState) {
            DataLoadingState.Loading ->
                SenseeScreenContent {
                    SenseeLoadingState(title = textProvider.text(HomeTextKeys.Loading))
                }

            is DataLoadingState.Error ->
                SenseeScreenContent {
                    SenseeErrorState(
                        title = textProvider.errorText(loadingState.throwable, HomeTextKeys.LoadError),
                        actions = {
                            SenseeButton(onClick = { onAction(HomeAction.Retry) }) {
                                Text(text = textProvider.text(CommonTextKeys.Retry))
                            }
                        },
                    )
                }

            else ->
                HomeDashboard(
                    uiState = uiState,
                    onAction = onAction,
                    textProvider = textProvider,
                )
        }
    }
}

@Composable
private fun HomeDashboard(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    textProvider: TextProvider,
) {
    val spacing = SenseeTheme.spacing
    val layoutMetrics = LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = layoutMetrics.screenHorizontalPadding,
                    vertical = layoutMetrics.screenVerticalPadding,
                ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            ReviewWidget(
                uiState = uiState,
                onStartDueSession = { onAction(HomeAction.StartDueSession) },
                textProvider = textProvider,
            )
        }
    }
}

@Composable
private fun ReviewWidget(
    uiState: HomeUiState,
    onStartDueSession: () -> Unit,
    textProvider: TextProvider,
) {
    val typography = SenseeTheme.typography
    val colors = SenseeTheme.colors
    val spacing = SenseeTheme.spacing

    SenseeSurface {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text(
                text = textProvider.text(HomeTextKeys.ReviewTitle),
                color = colors.textPrimary,
                style = typography.titleMedium,
            )
            Text(
                text =
                    when {
                        uiState.dueCount <= 0 -> textProvider.text(HomeTextKeys.ReviewNothingDue)
                        uiState.dueExceedsSessionLimit ->
                            textProvider.text(HomeTextKeys.ReviewDueCountCapped, uiState.dueCount)
                        else -> textProvider.quantity(HomeTextKeys.ReviewDueCount, uiState.dueCount)
                    },
                color = colors.textPrimary,
                style = typography.headlineSmall,
            )
            Text(
                text = textProvider.quantity(HomeTextKeys.ReviewGoal, uiState.dailyGoal),
                color = colors.textMuted,
                style = typography.bodyMedium,
            )
            SenseeButton(
                onClick = onStartDueSession,
                enabled = uiState.isReviewActionable,
            ) {
                Text(text = textProvider.text(HomeTextKeys.ReviewCta))
            }
        }
    }
}
