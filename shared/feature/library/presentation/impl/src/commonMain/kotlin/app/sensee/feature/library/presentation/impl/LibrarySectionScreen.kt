package app.sensee.feature.library.presentation.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.CommonTextKeys
import app.sensee.core.presentation.text.TextKey
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback
import app.sensee.feature.library.presentation.api.LibraryDeckUiState
import app.sensee.feature.library.presentation.api.LibraryHomeAction
import app.sensee.feature.library.presentation.api.LibraryHomeComponent
import app.sensee.feature.library.presentation.api.LibraryHomeUiState
import app.sensee.feature.library.presentation.api.LibrarySectionComponent
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.button.SenseeIconButton
import app.sensee.ui.designSystem.component.button.SenseeIconButtonDefaults
import app.sensee.ui.designSystem.component.deckEntryCard.SenseeDeckEntryCard
import app.sensee.ui.designSystem.component.layout.SenseeErrorState
import app.sensee.ui.designSystem.component.layout.SenseeLoadingState
import app.sensee.ui.designSystem.component.layout.SenseeScreenContent
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.icons.Add24px
import app.sensee.ui.designSystem.icons.Delete24px
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.composeunstyled.Text
import kotlinx.collections.immutable.PersistentList

@Composable
public fun LibrarySectionScreen(
    component: LibrarySectionComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberLibraryTextProvider(),
) {
    CompositionLocalProvider(LocalTextProvider provides textProvider) {
        Children(
            stack = component.stack,
            modifier = modifier,
        ) { child ->
            when (val instance = child.instance) {
                is LibraryHomeComponent ->
                    LibraryHomeScreen(
                        component = instance,
                        modifier = Modifier,
                        textProvider = textProvider,
                    )

                else -> error("Unknown library child: ${instance::class}")
            }
        }
    }
}

@Composable
public fun LibraryHomeScreen(
    component: LibraryHomeComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberLibraryTextProvider(),
) {
    val uiState by component.uiState.collectAsState()

    LibraryHomeContent(
        uiState = uiState,
        onAction = component::onAction,
        modifier = modifier.fillMaxSize(),
        textProvider = textProvider,
    )
}

@Composable
internal fun LibraryHomeContent(
    uiState: LibraryHomeUiState,
    onAction: (LibraryHomeAction) -> Unit,
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
                    SenseeLoadingState(title = textProvider.text(LibraryTextKeys.HomeLoading))
                }
            is DataLoadingState.Error ->
                SenseeScreenContent {
                    SenseeErrorState(
                        title =
                            textProvider.errorText(
                                loadingState.throwable,
                                LibraryTextKeys.HomeLoadError,
                            ),
                        actions = {
                            SenseeButton(onClick = { onAction(LibraryHomeAction.Retry) }) {
                                Text(text = textProvider.text(CommonTextKeys.Retry))
                            }
                        },
                    )
                }
            else ->
                LibraryHomeList(
                    uiState = uiState,
                    onAction = onAction,
                    textProvider = textProvider,
                )
        }
    }
}

@Composable
private fun rememberLibraryTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultLibraryTextProvider.withFallback(parent) }
}

@Composable
private fun LibraryHomeList(
    uiState: LibraryHomeUiState,
    onAction: (LibraryHomeAction) -> Unit,
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
        libraryHomeHeader(layoutMetrics = layoutMetrics, textProvider = textProvider)
        libraryOwnedSection(
            decks = uiState.owned,
            layoutMetrics = layoutMetrics,
            textProvider = textProvider,
            onUnAdopt = { id -> onAction(LibraryHomeAction.UnAdopt(id)) },
        )
        librarySuggestedSection(
            decks = uiState.suggested,
            layoutMetrics = layoutMetrics,
            textProvider = textProvider,
            onAdopt = { id -> onAction(LibraryHomeAction.Adopt(id)) },
        )
    }
}

private fun LazyListScope.libraryHomeHeader(
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
    textProvider: TextProvider,
) {
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            val typography = SenseeTheme.typography
            val colors = SenseeTheme.colors
            Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.extraSmall)) {
                Text(
                    text = textProvider.text(LibraryTextKeys.Title),
                    color = colors.textPrimary,
                    style = typography.titleLarge,
                )
                Text(
                    text = textProvider.text(LibraryTextKeys.Subtitle),
                    color = colors.textMuted,
                    style = typography.bodyMedium,
                )
            }
        }
    }
}

private fun LazyListScope.libraryOwnedSection(
    decks: PersistentList<LibraryDeckUiState>,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
    textProvider: TextProvider,
    onUnAdopt: (deckId: String) -> Unit,
) {
    librarySection(
        spec = LibrarySectionSpec(LibraryTextKeys.OwnedSection, LibraryTextKeys.OwnedEmpty),
        decks = decks,
        layoutMetrics = layoutMetrics,
        textProvider = textProvider,
    ) { deck ->
        if (deck.canUnAdopt) {
            {
                SenseeIconButton(
                    onClick = { onUnAdopt(deck.id) },
                    icon = {
                        SenseeIcon(
                            imageVector = Delete24px,
                            contentDescription = textProvider.text(LibraryTextKeys.UnAdopt),
                        )
                    },
                )
            }
        } else {
            null
        }
    }
}

private fun LazyListScope.librarySuggestedSection(
    decks: PersistentList<LibraryDeckUiState>,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
    textProvider: TextProvider,
    onAdopt: (deckId: String) -> Unit,
) {
    librarySection(
        spec = LibrarySectionSpec(LibraryTextKeys.SuggestedSection, LibraryTextKeys.SuggestedEmpty),
        decks = decks,
        layoutMetrics = layoutMetrics,
        textProvider = textProvider,
    ) { deck ->
        {
            SenseeIconButton(
                onClick = { onAdopt(deck.id) },
                icon = {
                    SenseeIcon(
                        imageVector = Add24px,
                        contentDescription = textProvider.text(LibraryTextKeys.Adopt),
                    )
                },
                colors = SenseeIconButtonDefaults.filledColors(),
            )
        }
    }
}

private fun LazyListScope.librarySection(
    spec: LibrarySectionSpec,
    decks: PersistentList<LibraryDeckUiState>,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
    textProvider: TextProvider,
    deckAction: (LibraryDeckUiState) -> (@Composable () -> Unit)?,
) {
    item {
        LibrarySectionHeading(text = textProvider.text(spec.heading), layoutMetrics = layoutMetrics)
    }
    if (decks.isEmpty()) {
        item {
            LibraryEmptyMessage(text = textProvider.text(spec.empty), layoutMetrics = layoutMetrics)
        }
    }
    items(items = decks, key = { "${spec.heading.value}-${it.id}" }) { deck ->
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            LibraryDeckCard(deck = deck, textProvider = textProvider, action = deckAction(deck))
        }
    }
}

private data class LibrarySectionSpec(
    val heading: TextKey,
    val empty: TextKey,
)

@Composable
private fun LibrarySectionHeading(
    text: String,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography

    SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
        Text(
            text = text,
            color = colors.textPrimary,
            style = typography.titleMedium,
        )
    }
}

@Composable
private fun LibraryEmptyMessage(
    text: String,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography

    SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
        Text(
            text = text,
            color = colors.textMuted,
            style = typography.bodyMedium,
        )
    }
}

@Composable
private fun LibraryDeckCard(
    deck: LibraryDeckUiState,
    textProvider: TextProvider,
    action: (@Composable () -> Unit)?,
) {
    val typography = SenseeTheme.typography

    SenseeDeckEntryCard(
        title = {
            Text(text = deck.title, style = typography.titleMedium)
        },
        subtitle = {
            Text(text = deck.description, style = typography.bodyMedium)
        },
        meta = {
            Text(
                text = textProvider.quantity(LibraryTextKeys.CardCount, deck.cardCount),
                style = typography.labelMedium,
            )
        },
        trailing = action?.let { { it() } },
    )
}
