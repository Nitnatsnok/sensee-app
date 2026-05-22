package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val CollapseContent24px: ImageVector
    get() {
        val current = _collapseContent24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.CollapseContent24px",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                ) {
                    moveTo(x = 11.0f, y = 13.0f)
                    verticalLineToRelative(dy = 6.0f)
                    horizontalLineTo(x = 9.0f)
                    verticalLineTo(y = 15.0f)
                    horizontalLineTo(x = 5.0f)
                    verticalLineTo(y = 13.0f)
                    horizontalLineToRelative(dx = 6.0f)
                    close()
                    moveTo(x = 15.0f, y = 5.0f)
                    verticalLineTo(y = 9.0f)
                    horizontalLineToRelative(dx = 4.0f)
                    verticalLineToRelative(dy = 2.0f)
                    horizontalLineTo(x = 13.0f)
                    verticalLineTo(y = 5.0f)
                    horizontalLineToRelative(dx = 2.0f)
                    close()
                }
            }.build()
            .also { _collapseContent24px = it }
    }

@Suppress("ObjectPropertyName")
private var _collapseContent24px: ImageVector? = null
