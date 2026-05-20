package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.practiceRating.SenseePracticeRatingButton
import app.sensee.ui.designSystem.component.practiceRating.SenseePracticeRatingButtonDefaults
import app.sensee.ui.designSystem.icons.SentimentDissatisfied24px
import app.sensee.ui.designSystem.icons.SentimentSatisfied24px
import app.sensee.ui.designSystem.icons.SentimentVeryDissatisfied24px
import app.sensee.ui.designSystem.icons.SentimentVerySatisfied24px
import app.sensee.ui.designSystem.icons.SwipeDown24px
import app.sensee.ui.designSystem.icons.SwipeLeft24px
import app.sensee.ui.designSystem.icons.SwipeRight24px
import app.sensee.ui.designSystem.icons.SwipeUp24px
import app.sensee.ui.designSystem.theme.SenseeTheme

@Preview
@Composable
private fun SenseePracticeRatingButtonPreview() =
    SenseePreview {
        Row(horizontalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.small)) {
            SenseePracticeRatingButton(
                label = "Again",
                icon = SentimentVeryDissatisfied24px,
                directionIcon = SwipeLeft24px,
                colors = SenseePracticeRatingButtonDefaults.againColors(),
                onClick = {},
            )
            SenseePracticeRatingButton(
                label = "Hard",
                icon = SentimentDissatisfied24px,
                directionIcon = SwipeDown24px,
                colors = SenseePracticeRatingButtonDefaults.hardColors(),
                onClick = {},
            )
            SenseePracticeRatingButton(
                label = "Good",
                icon = SentimentSatisfied24px,
                directionIcon = SwipeUp24px,
                colors = SenseePracticeRatingButtonDefaults.goodColors(),
                onClick = {},
            )
            SenseePracticeRatingButton(
                label = "Easy",
                icon = SentimentVerySatisfied24px,
                directionIcon = SwipeRight24px,
                colors = SenseePracticeRatingButtonDefaults.easyColors(),
                onClick = {},
            )
        }
    }
