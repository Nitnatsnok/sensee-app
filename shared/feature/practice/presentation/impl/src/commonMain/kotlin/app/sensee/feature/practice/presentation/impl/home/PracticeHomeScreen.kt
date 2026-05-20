package app.sensee.feature.practice.presentation.impl.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.CommonTextKeys
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.practice.presentation.api.DeckSummaryUiState
import app.sensee.feature.practice.presentation.api.PracticeHomeAction
import app.sensee.feature.practice.presentation.api.PracticeHomeComponent
import app.sensee.feature.practice.presentation.api.PracticeHomeUiState
import app.sensee.feature.practice.presentation.impl.text.PracticeTextKeys
import app.sensee.feature.practice.presentation.impl.text.rememberPracticeTextProvider
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.deckEntryCard.SenseeDeckEntryCard
import app.sensee.ui.designSystem.component.layout.SenseeEmptyState
import app.sensee.ui.designSystem.component.layout.SenseeErrorState
import app.sensee.ui.designSystem.component.layout.SenseeLoadingState
import app.sensee.ui.designSystem.component.layout.SenseeScreenContent
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import com.composeunstyled.Text
import kotlinx.collections.immutable.ImmutableList

@Composable
public fun PracticeHomeScreen(
    component: PracticeHomeComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberPracticeTextProvider(),
) {
    val uiState by component.uiState.collectAsState()

    PracticeHomeContent(
        uiState = uiState,
        onAction = component::onAction,
        modifier = modifier.fillMaxSize(),
        textProvider = textProvider,
    )
}

@Composable
internal fun PracticeHomeContent(
    uiState: PracticeHomeUiState,
    onAction: (PracticeHomeAction) -> Unit,
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
                    SenseeLoadingState(title = textProvider.text(PracticeTextKeys.HomeLoading))
                }
            is DataLoadingState.Error ->
                SenseeScreenContent {
                    SenseeErrorState(
                        title =
                            textProvider.errorText(
                                loadingState.throwable,
                                PracticeTextKeys.HomeLoadError,
                            ),
                        actions = {
                            SenseeButton(onClick = { onAction(PracticeHomeAction.Retry) }) {
                                Text(text = textProvider.text(CommonTextKeys.Retry))
                            }
                        },
                    )
                }
            else ->
                PracticeHomeList(
                    decks = uiState.decks,
                    onDeckClick = { deckId -> onAction(PracticeHomeAction.OpenDeck(deckId)) },
                    textProvider = textProvider,
                )
        }
    }
}

@Composable
private fun PracticeHomeList(
    decks: ImmutableList<DeckSummaryUiState>,
    onDeckClick: (String) -> Unit,
    textProvider: TextProvider,
) {
    val typography = SenseeTheme.typography
    val colors = SenseeTheme.colors
    val spacing = SenseeTheme.spacing
    val layoutMetrics = LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics()

    if (decks.isEmpty()) {
        SenseeScreenContent {
            SenseeEmptyState(title = textProvider.text(PracticeTextKeys.HomeEmpty))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = layoutMetrics.screenHorizontalPadding,
                top = layoutMetrics.screenVerticalPadding,
                end = layoutMetrics.screenHorizontalPadding,
                bottom = layoutMetrics.screenVerticalPadding + spacing.extraLarge,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                    Text(
                        text = textProvider.text(PracticeTextKeys.HomeTitle),
                        color = colors.textPrimary,
                        style = typography.titleLarge,
                    )
                    Text(
                        text = textProvider.text(PracticeTextKeys.HomeSubtitle),
                        color = colors.textMuted,
                        style = typography.bodyMedium,
                    )
                }
            }
        }
        items(items = decks, key = { it.id }) { deck ->
            SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
                SenseeDeckEntryCard(
                    title = {
                        Text(text = deck.title, style = typography.titleMedium)
                    },
                    subtitle = {
                        Text(text = deck.description, style = typography.bodyMedium)
                    },
                    meta = {
                        Text(
                            text =
                                textProvider.quantity(
                                    PracticeTextKeys.HomeCardCount,
                                    deck.cardCount,
                                ),
                            style = typography.labelMedium,
                        )
                    },
                    onClick = { onDeckClick(deck.id) },
                )
            }
        }
    }
}
