package app.sensee.ui.designSystem.component.deckEntryCard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

@Composable
internal fun SenseeDeckEntryProgressBar(
    progress: Float,
    trackColor: Color,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
) {
    val normalizedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(SenseeDeckEntryCardDefaults.ProgressHeight)
                .clip(CircleShape)
                .background(trackColor),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(normalizedProgress)
                    .height(SenseeDeckEntryCardDefaults.ProgressHeight)
                    .clip(CircleShape)
                    .background(indicatorColor),
        )
    }
}
