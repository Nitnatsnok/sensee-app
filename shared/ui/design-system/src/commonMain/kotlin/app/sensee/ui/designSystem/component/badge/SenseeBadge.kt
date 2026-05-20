package app.sensee.ui.designSystem.component.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

@Composable
public fun SenseeBadge(
    text: String,
    modifier: Modifier = Modifier,
    colors: SenseeBadgeColors = SenseeBadgeDefaults.neutralColors(),
    shape: Shape = SenseeBadgeDefaults.shape(),
    contentPadding: PaddingValues = SenseeBadgeDefaults.ContentPadding,
    borderWidth: Dp = SenseeBadgeDefaults.BorderWidth,
) {
    val typography = SenseeTheme.typography
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(colors.container)
                .border(width = borderWidth, color = colors.border, shape = shape)
                .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = colors.content,
            style = typography.labelMedium,
        )
    }
}
