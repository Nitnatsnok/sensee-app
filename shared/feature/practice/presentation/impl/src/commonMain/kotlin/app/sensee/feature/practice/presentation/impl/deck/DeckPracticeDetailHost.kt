package app.sensee.feature.practice.presentation.impl.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.practice.presentation.api.CardDetailCardUiState
import app.sensee.feature.practice.presentation.api.CardDetailComponent
import app.sensee.feature.practice.presentation.impl.detail.CardDetailScreen
import app.sensee.feature.practice.presentation.impl.grammar.grammarTagBadgeColors
import app.sensee.feature.practice.presentation.impl.grammar.grammarUnitBadgeColors
import app.sensee.feature.practice.presentation.impl.text.PracticeTextKeys
import app.sensee.feature.practice.presentation.impl.text.rememberPracticeTextProvider
import app.sensee.feature.practice.presentation.impl.text.shortLabel
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.ui.designSystem.component.badge.SenseeBadge
import app.sensee.ui.designSystem.component.layout.SenseeModalBottomSheet
import app.sensee.ui.designSystem.component.layout.SenseePaneHeader
import app.sensee.ui.designSystem.component.layout.SenseeSheetHeader
import app.sensee.ui.designSystem.component.layout.SenseeSurface
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

@Composable
internal fun DeckPracticeDetailPane(
    component: CardDetailComponent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberPracticeTextProvider(),
) {
    val spacing = SenseeTheme.spacing
    val typography = SenseeTheme.typography
    val state by component.uiState.collectAsState()
    val headword = state.card?.headword.orEmpty()
    val displayTitle =
        headword.ifBlank { textProvider.text(PracticeTextKeys.CardDetailSheetTitle) }

    SenseeSurface(
        modifier =
            modifier
                .statusBarsPadding()
                .padding(top = spacing.medium, bottom = spacing.medium, end = spacing.medium),
        shape = SenseeTheme.shapes.large,
        contentPadding = PaddingValues(0.dp),
        borderWidth = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SenseePaneHeader(
                onClose = onDismiss,
                closeAccessibilityLabel = textProvider.text(PracticeTextKeys.ActionClose),
                contentPadding =
                    PaddingValues(start = spacing.large, top = spacing.small, end = spacing.small),
            ) {
                Text(text = displayTitle, style = typography.headlineSmall)
            }
            val card = state.card
            if (card != null) {
                DetailBadges(
                    card = card,
                    labels = state.grammarLabels,
                    studyLanguageTag = state.studyLanguageTag,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = spacing.large,
                                top = spacing.extraSmall,
                                end = spacing.large,
                            ),
                )
            }
            CardDetailScreen(
                component = component,
                modifier = Modifier.fillMaxSize(),
                textProvider = textProvider,
                showCardSummary = false,
            )
        }
    }
}

@Composable
internal fun DeckPracticeDetailSheet(
    component: CardDetailComponent?,
    visible: Boolean,
    onDismiss: () -> Unit,
    textProvider: TextProvider = rememberPracticeTextProvider(),
) {
    // Keep the last non-null component around so the exit animation has content to render
    // after `panels.details` is cleared by decompose. Updated synchronously in composition
    // (a feed-forward cache of the latest input) rather than through a LaunchedEffect, which
    // would add a recomposition pass and a frame where renderTarget is briefly stale.
    var lastComponent by remember { mutableStateOf<CardDetailComponent?>(null) }
    if (component != null && component != lastComponent) {
        lastComponent = component
    }

    SenseeModalBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        contentPadding = PaddingValues(0.dp),
    ) {
        val spacing = SenseeTheme.spacing
        val renderTarget = component ?: lastComponent
        val card: CardDetailCardUiState?
        val labels: GrammarLabels
        val studyLanguageTag: String
        if (renderTarget != null) {
            val state by renderTarget.uiState.collectAsState()
            card = state.card
            labels = state.grammarLabels
            studyLanguageTag = state.studyLanguageTag
        } else {
            card = null
            labels = GrammarLabels.EMPTY
            studyLanguageTag = ""
        }
        val headword = card?.headword.orEmpty()

        val displayTitle =
            headword.ifBlank { textProvider.text(PracticeTextKeys.CardDetailSheetTitle) }
        SenseeSheetHeader(
            onClose = onDismiss,
            closeAccessibilityLabel = textProvider.text(PracticeTextKeys.ActionClose),
        ) {
            Text(text = displayTitle, style = SenseeTheme.typography.titleMedium)
        }
        if (card != null) {
            DetailBadges(
                card = card,
                labels = labels,
                studyLanguageTag = studyLanguageTag,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.large, vertical = spacing.extraSmall),
            )
        }
        Spacer(modifier = Modifier.height(spacing.small))
        if (renderTarget != null) {
            CardDetailScreen(
                component = renderTarget,
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                textProvider = textProvider,
                showCardSummary = false,
            )
        }
    }
}

@Composable
private fun DetailBadges(
    card: CardDetailCardUiState,
    labels: GrammarLabels,
    studyLanguageTag: String,
    modifier: Modifier = Modifier,
) {
    val spacing = SenseeTheme.spacing
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        SenseeBadge(
            text = card.unitType.shortLabel(labels, studyLanguageTag),
            colors = grammarUnitBadgeColors(card.unitType),
        )
        card.grammarTags.forEach { tag ->
            SenseeBadge(
                text = tag.shortLabel(labels, studyLanguageTag),
                colors = grammarTagBadgeColors(tag),
            )
        }
    }
}
