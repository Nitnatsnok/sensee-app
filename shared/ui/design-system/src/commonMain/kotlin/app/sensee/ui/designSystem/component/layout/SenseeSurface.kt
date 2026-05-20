package app.sensee.ui.designSystem.component.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.LocalContentColor

/**
 * Bordered surface card — the generic "panel" primitive. Use it for information panels,
 * grouped sections, or anywhere the M3 idea of a filled outlined surface fits.
 *
 * Falls back to `surfaceContainerLow` fill, `divider` border, and `shapes.medium`. Provides
 * `LocalContentColor` from `colors.content` to children so nested `Text`/`Icon` pick up the
 * right tone without manual color = .
 *
 * Distinct from `SenseeDeckEntryCard` (which is a list-row card with title/subtitle/meta
 * slots) and from `SenseeLearningCard` (the practice flip card). Use those when you want
 * those specific slot APIs; reach for `SenseeSurface` when you just need a container.
 */
@Composable
public fun SenseeSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colors: SenseeSurfaceColors = SenseeSurfaceDefaults.colors(),
    shape: Shape = SenseeSurfaceDefaults.shape(),
    contentPadding: PaddingValues = SenseeSurfaceDefaults.contentPadding(),
    borderWidth: Dp = SenseeSurfaceDefaults.BorderWidth,
    content: @Composable BoxScope.() -> Unit,
) {
    val clickableModifier =
        if (onClick != null) {
            Modifier.clickable(role = Role.Button, onClick = onClick)
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(colors.container)
                .border(width = borderWidth, color = colors.border, shape = shape)
                .then(clickableModifier)
                .padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            content()
        }
    }
}

@Immutable
public data class SenseeSurfaceColors(
    val container: Color,
    val content: Color,
    val border: Color,
)

public object SenseeSurfaceDefaults {
    public val BorderWidth: Dp = 1.dp

    @Composable
    public fun shape(): Shape = SenseeTheme.shapes.medium

    @Composable
    public fun contentPadding(): PaddingValues {
        val spacing = SenseeTheme.spacing
        return PaddingValues(spacing.large)
    }

    @Composable
    public fun colors(
        container: Color = Color.Unspecified,
        content: Color = Color.Unspecified,
        border: Color = Color.Unspecified,
    ): SenseeSurfaceColors {
        val colors = SenseeTheme.colors
        return SenseeSurfaceColors(
            container = container.takeOrElse { colors.surfaceContainerLow },
            content = content.takeOrElse { colors.textPrimary },
            border = border.takeOrElse { colors.border },
        )
    }
}
