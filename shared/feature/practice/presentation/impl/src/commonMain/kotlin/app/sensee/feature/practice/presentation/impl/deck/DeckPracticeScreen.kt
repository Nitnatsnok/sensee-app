package app.sensee.feature.practice.presentation.impl.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.CommonTextKeys
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.practice.presentation.api.DeckPracticeAction
import app.sensee.feature.practice.presentation.api.DeckPracticeCardUiState
import app.sensee.feature.practice.presentation.api.DeckPracticeComponent
import app.sensee.feature.practice.presentation.api.DeckPracticeUiState
import app.sensee.feature.practice.presentation.impl.text.PracticeTextKeys
import app.sensee.feature.practice.presentation.impl.text.rememberPracticeTextProvider
import app.sensee.ui.adaptive.AppChildPanels
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.layout.SenseeErrorState
import app.sensee.ui.designSystem.component.layout.SenseeLoadingState
import app.sensee.ui.designSystem.component.learningCard.SenseeLearningCardDefaults
import app.sensee.ui.designSystem.component.learningCard.SenseeLearningCardSide
import app.sensee.ui.designSystem.component.topBar.SenseeImmersiveTopBar
import app.sensee.ui.designSystem.component.topBar.SenseeTopBarIconButton
import app.sensee.ui.designSystem.icons.Close24px
import app.sensee.ui.designSystem.icons.Help24px
import app.sensee.ui.designSystem.icons.Info24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.learningDeck.LearningDeckConfig
import app.sensee.ui.learningDeck.LearningDeckState
import app.sensee.ui.learningDeck.LearningSwipeDeck
import app.sensee.ui.learningDeck.LearningSwipeDirection
import app.sensee.ui.learningDeck.rememberLearningDeckState
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.composeunstyled.Text
import kotlinx.collections.immutable.persistentSetOf

@OptIn(ExperimentalDecomposeApi::class)
@Composable
public fun DeckPracticeScreen(
    component: DeckPracticeComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberPracticeTextProvider(),
) {
    val uiState by component.uiState.collectAsState()

    var helpVisible by remember { mutableStateOf(false) }

    val colors = SenseeTheme.colors
    val backgroundBrush =
        remember(colors) {
            Brush.verticalGradient(
                listOf(colors.surfaceContainerHighest, colors.surfaceContainer, colors.background),
            )
        }

    Box(modifier = modifier.fillMaxSize().background(backgroundBrush)) {
        AppChildPanels(
            panels = component.panels,
            modifier = Modifier.fillMaxSize(),
            mainPaneWeight = MAIN_PANE_WEIGHT,
            detailPaneWeight = DETAIL_PANE_WEIGHT,
            main = { _, _ ->
                DeckPracticePane(
                    uiState = uiState,
                    onAction = component::onAction,
                    onHelpRequest = { helpVisible = true },
                    modifier = Modifier.fillMaxSize(),
                    textProvider = textProvider,
                )
            },
            detail = { detailChild, _ ->
                DeckPracticeDetailPane(
                    component = detailChild.instance,
                    onDismiss = { component.onAction(DeckPracticeAction.DismissDetails) },
                    modifier = Modifier.fillMaxSize(),
                    textProvider = textProvider,
                )
            },
            compactDetail = {
                MainPane()
                DeckPracticeDetailSheet(
                    component = detail?.instance,
                    visible = detail != null,
                    onDismiss = { component.onAction(DeckPracticeAction.DismissDetails) },
                    textProvider = textProvider,
                )
            },
        )
    }

    DeckPracticeHelpSheet(
        visible = helpVisible,
        onDismiss = { helpVisible = false },
        labels = uiState.grammarLabels,
        studyLanguageTag = uiState.studyLanguageTag,
        nativeLanguageTag = uiState.nativeLanguageTag,
        textProvider = textProvider,
    )
}

private const val MAIN_PANE_WEIGHT = 0.55f
private const val DETAIL_PANE_WEIGHT = 0.45f

@Composable
internal fun DeckPracticePane(
    uiState: DeckPracticeUiState,
    onAction: (DeckPracticeAction) -> Unit,
    onHelpRequest: () -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    // Deck state is hoisted to the pane level so the top bar — rendered once below — can
    // expose the focused card via `onDetails`, while the body branches just render content
    // for the current `loadingState`.
    val deckState = rememberLearningDeckState<String>()
    val flippedCards = remember { mutableStateMapOf<String, Boolean>() }
    // Key on `loadingState` (stable for Success), not `cards` — keying on cards would snap to
    // index 0 on every SRS reinjection.
    LaunchedEffect(uiState.loadingState) { deckState.snapToIndex(0) }

    // `derivedStateOf` confines per-swipe/flip recompositions to the readers below instead of
    // the whole pane (top bar included).
    val currentCard by remember(uiState) {
        derivedStateOf { uiState.cards.getOrNull(deckState.currentIndex) }
    }
    val canSwipe by remember(uiState) {
        derivedStateOf {
            val card = uiState.cards.getOrNull(deckState.currentIndex)
            card != null && flippedCards[card.presentationKey] == true
        }
    }

    // Hoisted so PracticeTopBar stays skippable: the lambda identity only changes when the
    // focused card id or load state changes, not on every swipe/flip.
    val onDetails: (() -> Unit)? =
        remember(uiState.loadingState, currentCard?.id) {
            val card = currentCard
            if (uiState.loadingState == DataLoadingState.Success && card != null) {
                { onAction(DeckPracticeAction.FocusCard(card.id)) }
            } else {
                null
            }
        }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PracticeTopBar(
            onClose = { onAction(DeckPracticeAction.Close) },
            onHelp = onHelpRequest,
            onDetails = onDetails,
            textProvider = textProvider,
        )
        when (val loadingState = uiState.loadingState) {
            DataLoadingState.Loading ->
                SenseeLoadingState(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    title = textProvider.text(PracticeTextKeys.LoadingDeck),
                )

            is DataLoadingState.Error ->
                SenseeErrorState(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    title =
                        textProvider.errorText(
                            loadingState.throwable,
                            PracticeTextKeys.DeckOpenError,
                        ),
                    actions = {
                        SenseeButton(onClick = { onAction(DeckPracticeAction.Retry) }) {
                            Text(text = textProvider.text(CommonTextKeys.Retry))
                        }
                    },
                )

            else ->
                DeckPracticeBody(
                    state =
                        DeckPracticeBodyState(
                            uiState = uiState,
                            deckState = deckState,
                            flippedCards = flippedCards,
                            canSwipe = canSwipe,
                        ),
                    onAction = onAction,
                    textProvider = textProvider,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
        }
    }
}

@Composable
private fun PracticeTopBar(
    onClose: () -> Unit,
    onHelp: () -> Unit,
    onDetails: (() -> Unit)?,
    textProvider: TextProvider,
) {
    SenseeImmersiveTopBar(
        navigation = {
            SenseeTopBarIconButton(
                onClick = onClose,
                accessibilityLabel = textProvider.text(PracticeTextKeys.ActionClose),
                icon = {
                    SenseeIcon(imageVector = Close24px, contentDescription = null)
                },
            )
        },
        actions = {
            SenseeTopBarIconButton(
                onClick = onHelp,
                accessibilityLabel = textProvider.text(PracticeTextKeys.ActionHelp),
                icon = {
                    SenseeIcon(imageVector = Help24px, contentDescription = null)
                },
            )
            SenseeTopBarIconButton(
                onClick = onDetails ?: {},
                enabled = onDetails != null,
                accessibilityLabel = textProvider.text(PracticeTextKeys.ActionDetails),
                icon = {
                    SenseeIcon(imageVector = Info24px, contentDescription = null)
                },
            )
        },
    )
}

@Composable
private fun DeckPracticeBody(
    state: DeckPracticeBodyState,
    onAction: (DeckPracticeAction) -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    val spacing = SenseeTheme.spacing
    val deckConfig = rememberPracticeDeckConfig(state.canSwipe)

    fun submitCurrent(direction: LearningSwipeDirection) {
        if (state.canSwipe) state.deckState.requestSwipe(direction)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        GrammarLabelsLoadStatus(
            state = state.uiState.grammarLabelsState,
            onRetry = { onAction(DeckPracticeAction.RetryGrammarLabels) },
            textProvider = textProvider,
        )
        DeckPracticeCardDeck(
            state = state,
            deckConfig = deckConfig,
            onAction = onAction,
            textProvider = textProvider,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = spacing.extraLarge,
                        vertical = spacing.doubleExtraLarge,
                    ),
        )

        PracticeRatingRow(
            enabled = state.canSwipe,
            onAgain = { submitCurrent(LearningSwipeDirection.Start) },
            onHard = { submitCurrent(LearningSwipeDirection.Down) },
            onGood = { submitCurrent(LearningSwipeDirection.Up) },
            onEasy = { submitCurrent(LearningSwipeDirection.End) },
            modifier =
                Modifier
                    // Cap the rating row to the deck's landscape footprint so the buttons
                    // stay grouped under the card on wide windows instead of spreading
                    // edge-to-edge across the pane. The Column centres it horizontally.
                    .widthIn(max = PracticeDeckSize.LandscapeMaxWidth)
                    .padding(horizontal = spacing.large)
                    // Top gap absorbs the back-card stack peek (`stackOffset`) that visually
                    // extends below the top card's layout bounds, so the rating row never
                    // looks fused to the deck.
                    .padding(top = spacing.large, bottom = spacing.large),
            textProvider = textProvider,
        )
    }
}

@Composable
private fun rememberPracticeDeckConfig(canSwipe: Boolean): LearningDeckConfig =
    remember(canSwipe) {
        LearningDeckConfig(
            visibleCards = 3,
            stackOffset = 22.dp,
            stackScaleStep = 0.03f,
            stackAlphaStep = 0.03f,
            // Drag/keyboard input is gated by the card being flipped: a swipe must
            // commit a rating, so the unflipped card stays put.
            gesturesEnabled = canSwipe,
            keyboardEnabled = canSwipe,
            allowedDirections =
                if (canSwipe) {
                    persistentSetOf(
                        LearningSwipeDirection.Start,
                        LearningSwipeDirection.End,
                        LearningSwipeDirection.Up,
                        LearningSwipeDirection.Down,
                    )
                } else {
                    persistentSetOf()
                },
        )
    }

@Composable
private fun DeckPracticeCardDeck(
    state: DeckPracticeBodyState,
    deckConfig: LearningDeckConfig,
    onAction: (DeckPracticeAction) -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    val spacing = SenseeTheme.spacing

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val deckSizeModifier = practiceDeckSizeModifier(landscape = maxWidth > maxHeight)
        LearningSwipeDeck(
            items = state.uiState.cards,
            itemKey = { it.presentationKey },
            state = state.deckState,
            modifier = deckSizeModifier,
            config = deckConfig,
            onSwipe = { card, direction ->
                state.flippedCards.remove(card.presentationKey)
                onAction(DeckPracticeAction.SubmitReview(card.id, direction.toRating()))
            },
            emptyContent = {
                PracticeEmptyCard(
                    label =
                        if (state.uiState.cards.isEmpty()) {
                            textProvider.text(PracticeTextKeys.DeckFinished)
                        } else {
                            null
                        },
                )
            },
        ) { card ->
            DeckPracticeLearningCard(
                state = state,
                card = card,
                onAction = onAction,
                textProvider = textProvider,
                bottomPadding = spacing.small,
                isTopCard = isTopCard,
            )
        }
    }
}

private fun practiceDeckSizeModifier(landscape: Boolean): Modifier =
    if (landscape) {
        Modifier
            .sizeIn(
                maxWidth = PracticeDeckSize.LandscapeMaxWidth,
                maxHeight = PracticeDeckSize.LandscapeMaxHeight,
            ).aspectRatio(PracticeDeckSize.LANDSCAPE_ASPECT_RATIO)
    } else {
        Modifier
            .sizeIn(
                maxWidth = PracticeDeckSize.PortraitMaxWidth,
                maxHeight = PracticeDeckSize.PortraitMaxHeight,
            ).aspectRatio(PracticeDeckSize.PORTRAIT_ASPECT_RATIO)
    }

@Composable
private fun DeckPracticeLearningCard(
    state: DeckPracticeBodyState,
    card: DeckPracticeCardUiState,
    onAction: (DeckPracticeAction) -> Unit,
    textProvider: TextProvider,
    bottomPadding: androidx.compose.ui.unit.Dp,
    isTopCard: Boolean,
) {
    val entering = isTopCard && card.animateEntrance
    val entrance =
        remember(card.presentationKey) {
            Animatable(if (entering) 0f else 1f)
        }
    LaunchedEffect(card.presentationKey) {
        if (entering) {
            entrance.animateTo(1f, animationSpec = tween(durationMillis = 340))
        }
    }
    val density = LocalDensity.current
    val entranceSlidePx = remember(density) { with(density) { 160.dp.toPx() } }
    val isFlipped = state.flippedCards[card.presentationKey] == true

    SenseeLearningCard(
        modifier =
            Modifier.graphicsLayer {
                val progress = entrance.value
                alpha = progress
                translationX = (1f - progress) * entranceSlidePx
            },
        isBackVisible = isFlipped,
        onClick =
            if (isTopCard && state.uiState.tapToFlipEnabled) {
                { state.flippedCards[card.presentationKey] = !isFlipped }
            } else {
                null
            },
        front = {
            PracticeCardFrontContent(
                card = card,
                labels = state.uiState.grammarLabels,
                studyLanguageTag = state.uiState.studyLanguageTag,
                onSpeak = { text -> onAction(DeckPracticeAction.SpeakText(text)) },
            )
            if (isTopCard && !state.uiState.tapToFlipEnabled && !isFlipped) {
                RevealMeaningButton(
                    onClick = { state.flippedCards[card.presentationKey] = true },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = bottomPadding),
                    textProvider = textProvider,
                )
            }
        },
        back = {
            PracticeCardBackContent(
                card = card,
                labels = state.uiState.grammarLabels,
                studyLanguageTag = state.uiState.studyLanguageTag,
                onSpeak = { text -> onAction(DeckPracticeAction.SpeakText(text)) },
            )
        },
    )
}

private data class DeckPracticeBodyState(
    val uiState: DeckPracticeUiState,
    val deckState: LearningDeckState<String>,
    val flippedCards: SnapshotStateMap<String, Boolean>,
    val canSwipe: Boolean,
)

/**
 * A card-shaped empty slot reusing the learning-card surface so it lines up exactly with a
 * real card. Rendered while a review is in flight (blank) and, with [label], as the terminal
 * "deck finished" state.
 */
@Composable
private fun PracticeEmptyCard(label: String?) {
    val colors = SenseeLearningCardDefaults.colors()
    SenseeLearningCardSide(
        containerColor = colors.frontContainer,
        contentColor = colors.frontContent,
        borderColor = colors.border,
        borderWidth = SenseeLearningCardDefaults.BorderWidth,
        shape = SenseeLearningCardDefaults.shape(),
        contentPadding = SenseeLearningCardDefaults.contentPadding(),
    ) {
        if (label != null) {
            Text(
                text = label,
                style = SenseeTheme.typography.titleMedium,
                color = SenseeTheme.colors.textSecondary,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
