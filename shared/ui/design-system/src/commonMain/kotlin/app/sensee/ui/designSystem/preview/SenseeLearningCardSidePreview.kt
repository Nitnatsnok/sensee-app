package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.component.learningCard.SenseeLearningCardDefaults
import app.sensee.ui.designSystem.component.learningCard.SenseeLearningCardSide
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

@Preview
@Composable
private fun SenseeLearningCardSidePreview() =
    SenseePreview {
        val colors = SenseeLearningCardDefaults.colors()
        Box(modifier = Modifier.size(width = 280.dp, height = 360.dp)) {
            SenseeLearningCardSide(
                containerColor = colors.frontContainer,
                contentColor = colors.frontContent,
                borderColor = colors.border,
                borderWidth = SenseeLearningCardDefaults.BorderWidth,
                shape = SenseeLearningCardDefaults.shape(),
                contentPadding = SenseeLearningCardDefaults.contentPadding(),
            ) {
                Text(
                    text = "to run",
                    style = SenseeTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
