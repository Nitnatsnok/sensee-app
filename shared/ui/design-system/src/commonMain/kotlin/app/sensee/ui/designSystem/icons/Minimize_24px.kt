package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Minimize24px: ImageVector
    get() {
        val current = _minimize24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.Minimize24px",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                ) {
                    moveTo(x = 6.0f, y = 21.0f)
                    verticalLineTo(y = 19.0f)
                    horizontalLineTo(x = 18.0f)
                    verticalLineToRelative(dy = 2.0f)
                    horizontalLineTo(x = 6.0f)
                    close()
                }
            }.build()
            .also { _minimize24px = it }
    }

@Suppress("ObjectPropertyName")
private var _minimize24px: ImageVector? = null
