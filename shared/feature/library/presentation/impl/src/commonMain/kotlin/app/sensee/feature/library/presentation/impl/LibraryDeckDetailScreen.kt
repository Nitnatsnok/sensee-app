package app.sensee.feature.library.presentation.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.CommonTextKeys
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.library.presentation.api.LibraryCardUiState
import app.sensee.feature.library.presentation.api.LibraryDeckDetailAction
import app.sensee.feature.library.presentation.api.LibraryDeckDetailComponent
import app.sensee.feature.library.presentation.api.LibraryDeckDetailUiState
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.layout.SenseeErrorState
import app.sensee.ui.designSystem.component.layout.SenseeLoadingState
import app.sensee.ui.designSystem.component.layout.SenseePaneHeader
import app.sensee.ui.designSystem.component.layout.SenseeScreenContent
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.component.layout.SenseeSurface
import app.sensee.ui.designSystem.component.topBar.SenseeTopBar
import app.sensee.ui.designSystem.component.topBar.SenseeTopBarIconButton
import app.sensee.ui.designSystem.icons.ArrowBack24px
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import app.sensee.ui.senseCard.SenseCard
import com.composeunstyled.Text

@Composable
public fun LibraryDeckDetailScreen(
    component: LibraryDeckDetailComponent,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    textProvider: TextProvider = rememberLibraryTextProvider(),
) {
    val uiState by component.uiState.collectAsState()

    LibraryDeckDetailContent(
        uiState = uiState,
        onAction = component::onAction,
        modifier = modifier.fillMaxSize(),
        compact = compact,
        textProvider = textProvider,
    )
}

@Composable
internal fun LibraryDeckDetailContent(
    uiState: LibraryDeckDetailUiState,
    onAction: (LibraryDeckDetailAction) -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    val colors = SenseeTheme.colors
    val spacing = SenseeTheme.spacing
    val typography = SenseeTheme.typography
    val title = uiState.title.ifBlank { textProvider.text(LibraryTextKeys.Title) }

    if (compact) {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(colors.background),
        ) {
            SenseeTopBar(
                title = {
                    Text(
                        text = title,
                        style = typography.titleMedium,
                    )
                },
                navigation = {
                    SenseeTopBarIconButton(
                        onClick = { onAction(LibraryDeckDetailAction.Back) },
                        icon = {
                            SenseeIcon(
                                imageVector = ArrowBack24px,
                                contentDescription = textProvider.text(LibraryTextKeys.DeckBack),
                            )
                        },
                    )
                },
            )
            LibraryDeckDetailBody(uiState = uiState, onAction = onAction, textProvider = textProvider)
        }
    } else {
        SenseeSurface(
            modifier =
                modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = spacing.medium, bottom = spacing.medium, end = spacing.medium),
            shape = SenseeTheme.shapes.large,
            contentPadding = PaddingValues(0.dp),
            borderWidth = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SenseePaneHeader(
                    onClose = { onAction(LibraryDeckDetailAction.Close) },
                    closeAccessibilityLabel = textProvider.text(LibraryTextKeys.DeckClose),
                    contentPadding =
                        PaddingValues(start = spacing.large, top = spacing.small, end = spacing.small),
                ) {
                    Text(text = title, style = typography.headlineSmall)
                }
                LibraryDeckDetailBody(uiState = uiState, onAction = onAction, textProvider = textProvider)
            }
        }
    }
}

@Composable
private fun LibraryDeckDetailBody(
    uiState: LibraryDeckDetailUiState,
    onAction: (LibraryDeckDetailAction) -> Unit,
    textProvider: TextProvider,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState.loadingState) {
            DataLoadingState.Loading ->
                SenseeScreenContent {
                    SenseeLoadingState(title = textProvider.text(LibraryTextKeys.DeckLoading))
                }
            is DataLoadingState.Error ->
                SenseeScreenContent {
                    SenseeErrorState(
                        title = textProvider.errorText(state.throwable, LibraryTextKeys.DeckLoadError),
                        actions = {
                            SenseeButton(onClick = { onAction(LibraryDeckDetailAction.Retry) }) {
                                Text(text = textProvider.text(CommonTextKeys.Retry))
                            }
                        },
                    )
                }
            else ->
                LibraryDeckDetailList(uiState = uiState, onAction = onAction, textProvider = textProvider)
        }
    }
}

@Composable
private fun LibraryDeckDetailList(
    uiState: LibraryDeckDetailUiState,
    onAction: (LibraryDeckDetailAction) -> Unit,
    textProvider: TextProvider,
) {
    val spacing = SenseeTheme.spacing
    val layoutMetrics = LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics()

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
                LibraryDeckDetailHeader(uiState = uiState, onAction = onAction, textProvider = textProvider)
            }
        }
        if (uiState.cards.isEmpty()) {
            item {
                LibraryEmptyMessage(
                    text = textProvider.text(LibraryTextKeys.DeckEmpty),
                    layoutMetrics = layoutMetrics,
                )
            }
        }
        items(items = uiState.cards, key = { it.id }) { card ->
            SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
                LibraryDeckDetailCard(
                    card = card,
                    labels = uiState.grammarLabels,
                    studyLanguageTag = uiState.studyLanguageTag,
                )
            }
        }
    }
}

@Composable
private fun LibraryDeckDetailHeader(
    uiState: LibraryDeckDetailUiState,
    onAction: (LibraryDeckDetailAction) -> Unit,
    textProvider: TextProvider,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography

    Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
        if (uiState.description.isNotBlank()) {
            Text(text = uiState.description, color = colors.textMuted, style = typography.bodyMedium)
        }
        if (uiState.isService) {
            SenseeButton(
                onClick = { onAction(LibraryDeckDetailAction.AdoptDeck) },
                enabled = !uiState.adopting,
            ) {
                Text(text = textProvider.text(LibraryTextKeys.DeckAdopt))
            }
            uiState.adoptError?.let { throwable ->
                Text(
                    text = textProvider.errorText(throwable, LibraryTextKeys.DeckAdoptError),
                    color = colors.danger,
                    style = typography.labelMedium,
                )
            }
        }
    }
}

// Read-only browse: no onClick, so the shared sense card renders static (no selection toggle),
// but with the full rich detail — identical to how a sense looks during capture.
@Composable
private fun LibraryDeckDetailCard(
    card: LibraryCardUiState,
    labels: GrammarLabels,
    studyLanguageTag: String,
) {
    SenseCard(
        sense = card.sense,
        labels = labels,
        studyLanguageTag = studyLanguageTag,
    )
}
