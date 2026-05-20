package app.sensee.feature.practice.presentation.impl.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.sensee.core.presentation.DataLoadingState
import app.sensee.core.presentation.text.CommonTextKeys
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.practice.presentation.api.CardDetailAction
import app.sensee.feature.practice.presentation.api.CardDetailCardUiState
import app.sensee.feature.practice.presentation.api.CardDetailComponent
import app.sensee.feature.practice.presentation.api.CardDetailUiState
import app.sensee.feature.practice.presentation.api.RelatedCardUiState
import app.sensee.feature.practice.presentation.impl.grammar.grammarTagBadgeColors
import app.sensee.feature.practice.presentation.impl.grammar.grammarUnitBadgeColors
import app.sensee.feature.practice.presentation.impl.text.PracticeTextKeys
import app.sensee.feature.practice.presentation.impl.text.label
import app.sensee.feature.practice.presentation.impl.text.rememberPracticeTextProvider
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.ui.designSystem.component.badge.SenseeBadge
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.deckEntryCard.SenseeDeckEntryCard
import app.sensee.ui.designSystem.component.deckEntryCard.SenseeDeckEntryCardDefaults
import app.sensee.ui.designSystem.component.layout.SenseeEmptyState
import app.sensee.ui.designSystem.component.layout.SenseeErrorState
import app.sensee.ui.designSystem.component.layout.SenseeLoadingState
import app.sensee.ui.designSystem.component.layout.SenseeSurface
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text
import kotlinx.collections.immutable.ImmutableList

@Composable
public fun CardDetailScreen(
    component: CardDetailComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberPracticeTextProvider(),
) {
    val uiState by component.uiState.collectAsState()
    CardDetailContent(
        uiState = uiState,
        onAction = component::onAction,
        modifier = modifier.fillMaxSize(),
        textProvider = textProvider,
    )
}

@Composable
internal fun CardDetailContent(
    uiState: CardDetailUiState,
    onAction: (CardDetailAction) -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    val spacing = SenseeTheme.spacing

    Box(
        modifier = modifier,
    ) {
        when (val state = uiState.loadingState) {
            DataLoadingState.Loading ->
                SenseeLoadingState(
                    modifier = Modifier.padding(spacing.large),
                    title = textProvider.text(PracticeTextKeys.CardDetailLoading),
                )
            is DataLoadingState.Error ->
                SenseeErrorState(
                    modifier = Modifier.padding(spacing.large),
                    title =
                        textProvider.errorText(
                            state.throwable,
                            PracticeTextKeys.CardDetailOpenError,
                        ),
                    actions = {
                        SenseeButton(onClick = { onAction(CardDetailAction.Retry) }) {
                            Text(text = textProvider.text(CommonTextKeys.Retry))
                        }
                    },
                )
            else ->
                CardDetailBody(
                    card = uiState.card,
                    lemmaText = uiState.lemmaText,
                    related = uiState.relatedCards,
                    onRelatedClick = { cardId -> onAction(CardDetailAction.OpenRelated(cardId)) },
                    modifier = Modifier.fillMaxSize(),
                    textProvider = textProvider,
                )
        }
    }
}

@Composable
private fun CardDetailBody(
    card: CardDetailCardUiState?,
    lemmaText: String,
    related: ImmutableList<RelatedCardUiState>,
    onRelatedClick: (String) -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    val spacing = SenseeTheme.spacing
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography

    if (card == null) {
        SenseeEmptyState(title = textProvider.text(PracticeTextKeys.CardDetailNoData))
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding =
            PaddingValues(
                start = spacing.large,
                top = spacing.large,
                end = spacing.large,
                bottom = spacing.extraLarge,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item { CardHeader(card = card, textProvider = textProvider) }
        item { CardBodySection(card = card, textProvider = textProvider) }
        if (related.isNotEmpty()) {
            item {
                Text(
                    text = textProvider.text(PracticeTextKeys.CardDetailRelatedTitle, lemmaText),
                    color = colors.textPrimary,
                    style = typography.titleMedium,
                )
            }
            items(items = related, key = { it.id }) { item ->
                RelatedCardRow(
                    item = item,
                    onClick = { onRelatedClick(item.id) },
                    textProvider = textProvider,
                )
            }
        }
    }
}

@Composable
private fun CardHeader(
    card: CardDetailCardUiState,
    textProvider: TextProvider,
) {
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing

    SenseeSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
            Text(text = card.headword, style = typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                SenseeBadge(
                    text = card.unitType.label(textProvider),
                    colors = grammarUnitBadgeColors(card.unitType),
                )
                card.grammarTags.forEach { tag ->
                    SenseeBadge(
                        text = tag.label(textProvider),
                        colors = grammarTagBadgeColors(tag),
                    )
                }
            }
            Text(text = card.translation, style = typography.titleMedium)
        }
    }
}

@Composable
private fun CardBodySection(
    card: CardDetailCardUiState,
    textProvider: TextProvider,
) {
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing

    SenseeSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            SectionLabel(text = textProvider.text(PracticeTextKeys.CardDetailSectionSense))
            Text(text = card.senseSummary, style = typography.bodyMedium)
            Spacer(modifier = Modifier.height(spacing.small))
            SectionLabel(text = textProvider.text(PracticeTextKeys.CardDetailSectionContext))
            Text(
                text = StudiedSentence.parse(card.contextSentence).plainText(),
                style = typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    Text(
        text = text,
        color = colors.textMuted,
        style = typography.labelMedium,
    )
}

@Composable
private fun RelatedCardRow(
    item: RelatedCardUiState,
    onClick: () -> Unit,
    textProvider: TextProvider,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing

    SenseeDeckEntryCard(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Text(
                    text = item.headword,
                    modifier = Modifier.weight(1f),
                    style = typography.titleMedium,
                )
                SenseeBadge(
                    text = item.unitType.label(textProvider),
                    colors = grammarUnitBadgeColors(item.unitType),
                )
            }
        },
        subtitle = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                Text(text = item.translation, style = typography.bodyMedium)
                if (item.isCurrent) {
                    Text(
                        text = textProvider.text(PracticeTextKeys.CardDetailCurrentlyOpen),
                        color = colors.accent,
                        style = typography.labelMedium,
                    )
                }
            }
        },
        meta = {
            Text(
                text = item.senseSummary,
                style = typography.bodySmall,
            )
        },
        onClick = if (item.isCurrent) null else onClick,
        enabled = !item.isCurrent,
        colors =
            SenseeDeckEntryCardDefaults.colors(
                border = if (item.isCurrent) colors.accent else Color.Unspecified,
            ),
    )
}
