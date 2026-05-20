package app.sensee.feature.practice.presentation.impl.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.practice.presentation.api.DeckPracticeRatingAction
import app.sensee.feature.practice.presentation.impl.text.PracticeTextKeys
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.button.SenseeButtonColors
import app.sensee.ui.designSystem.component.button.SenseeButtonDefaults
import app.sensee.ui.designSystem.component.practiceRating.SenseePracticeRatingButton
import app.sensee.ui.designSystem.component.practiceRating.SenseePracticeRatingButtonDefaults
import app.sensee.ui.designSystem.icons.ArrowDownwardAlt24px
import app.sensee.ui.designSystem.icons.ArrowLeftAlt24px
import app.sensee.ui.designSystem.icons.ArrowRightAlt24px
import app.sensee.ui.designSystem.icons.ArrowUpwardAlt24px
import app.sensee.ui.designSystem.icons.Flip24px
import app.sensee.ui.designSystem.icons.SentimentDissatisfied24px
import app.sensee.ui.designSystem.icons.SentimentSatisfied24px
import app.sensee.ui.designSystem.icons.SentimentVeryDissatisfied24px
import app.sensee.ui.designSystem.icons.SentimentVerySatisfied24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.learningDeck.LearningSwipeDirection
import com.composeunstyled.Text

@Composable
internal fun PracticeRatingRow(
    enabled: Boolean,
    onAgain: () -> Unit,
    onHard: () -> Unit,
    onGood: () -> Unit,
    onEasy: () -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    val spacing = SenseeTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SenseePracticeRatingButton(
            label = textProvider.text(PracticeTextKeys.RatingAgain),
            icon = SentimentVeryDissatisfied24px,
            directionIcon = ArrowLeftAlt24px,
            onClick = onAgain,
            colors = SenseePracticeRatingButtonDefaults.againColors(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        SenseePracticeRatingButton(
            label = textProvider.text(PracticeTextKeys.RatingHard),
            icon = SentimentDissatisfied24px,
            directionIcon = ArrowDownwardAlt24px,
            onClick = onHard,
            colors = SenseePracticeRatingButtonDefaults.hardColors(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        SenseePracticeRatingButton(
            label = textProvider.text(PracticeTextKeys.RatingGood),
            icon = SentimentSatisfied24px,
            directionIcon = ArrowUpwardAlt24px,
            onClick = onGood,
            colors = SenseePracticeRatingButtonDefaults.goodColors(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        SenseePracticeRatingButton(
            label = textProvider.text(PracticeTextKeys.RatingEasy),
            icon = SentimentVerySatisfied24px,
            directionIcon = ArrowRightAlt24px,
            onClick = onEasy,
            colors = SenseePracticeRatingButtonDefaults.easyColors(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun RevealMeaningButton(
    onClick: () -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    val spacing = SenseeTheme.spacing
    SenseeButton(
        onClick = onClick,
        modifier = modifier,
        colors = SenseeButtonColors.tonal(),
    ) {
        SenseeIcon(
            imageVector = Flip24px,
            contentDescription = null,
            modifier = Modifier.size(SenseeButtonDefaults.IconSize),
        )
        Spacer(modifier = Modifier.width(spacing.extraSmall))
        Text(text = textProvider.text(PracticeTextKeys.RevealButton))
    }
}

/**
 * Practice-screen-specific tuning for the swipe deck dimensions. The card flips orientation
 * with the available slot so it reads like a real physical card: roughly 3:2 landscape on
 * wide windows, 2:3 portrait on tall ones. Max bounds cap the deck so a huge desktop window
 * doesn't turn the card into a billboard.
 */
internal object PracticeDeckSize {
    const val LANDSCAPE_ASPECT_RATIO: Float = 3f / 2f
    const val PORTRAIT_ASPECT_RATIO: Float = 2f / 3f
    val LandscapeMaxWidth: Dp = 560.dp
    val LandscapeMaxHeight: Dp = 400.dp
    val PortraitMaxWidth: Dp = 380.dp
    val PortraitMaxHeight: Dp = 560.dp
}

// Symmetric "severity compass": difficulty eases Start → Down → Up → End
// (Again → Hard → Good → Easy). Horizontal extremes carry the rating extremes
// (mirroring the leftmost/rightmost buttons); vertical carries the two middles
// with natural valence (up = better).
internal fun LearningSwipeDirection.toRating(): DeckPracticeRatingAction =
    when (this) {
        LearningSwipeDirection.Start -> DeckPracticeRatingAction.Again
        LearningSwipeDirection.Down -> DeckPracticeRatingAction.Hard
        LearningSwipeDirection.Up -> DeckPracticeRatingAction.Good
        LearningSwipeDirection.End -> DeckPracticeRatingAction.Easy
    }
