package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Add24px: ImageVector
    get() {
        val current = _add24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.Add24px",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 960.0f,
                viewportHeight = 960.0f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                ) {
                    moveTo(x = 440.0f, y = 520.0f)
                    lineTo(x = 240.0f, y = 520.0f)
                    quadTo(
                        x1 = 223.0f,
                        y1 = 520.0f,
                        x2 = 211.5f,
                        y2 = 508.5f,
                    )
                    quadTo(
                        x1 = 200.0f,
                        y1 = 497.0f,
                        x2 = 200.0f,
                        y2 = 480.0f,
                    )
                    quadTo(
                        x1 = 200.0f,
                        y1 = 463.0f,
                        x2 = 211.5f,
                        y2 = 451.5f,
                    )
                    quadTo(
                        x1 = 223.0f,
                        y1 = 440.0f,
                        x2 = 240.0f,
                        y2 = 440.0f,
                    )
                    lineTo(x = 440.0f, y = 440.0f)
                    lineTo(x = 440.0f, y = 240.0f)
                    quadTo(
                        x1 = 440.0f,
                        y1 = 223.0f,
                        x2 = 451.5f,
                        y2 = 211.5f,
                    )
                    quadTo(
                        x1 = 463.0f,
                        y1 = 200.0f,
                        x2 = 480.0f,
                        y2 = 200.0f,
                    )
                    quadTo(
                        x1 = 497.0f,
                        y1 = 200.0f,
                        x2 = 508.5f,
                        y2 = 211.5f,
                    )
                    quadTo(
                        x1 = 520.0f,
                        y1 = 223.0f,
                        x2 = 520.0f,
                        y2 = 240.0f,
                    )
                    lineTo(x = 520.0f, y = 440.0f)
                    lineTo(x = 720.0f, y = 440.0f)
                    quadTo(
                        x1 = 737.0f,
                        y1 = 440.0f,
                        x2 = 748.5f,
                        y2 = 451.5f,
                    )
                    quadTo(
                        x1 = 760.0f,
                        y1 = 463.0f,
                        x2 = 760.0f,
                        y2 = 480.0f,
                    )
                    quadTo(
                        x1 = 760.0f,
                        y1 = 497.0f,
                        x2 = 748.5f,
                        y2 = 508.5f,
                    )
                    quadTo(
                        x1 = 737.0f,
                        y1 = 520.0f,
                        x2 = 720.0f,
                        y2 = 520.0f,
                    )
                    lineTo(x = 520.0f, y = 520.0f)
                    lineTo(x = 520.0f, y = 720.0f)
                    quadTo(
                        x1 = 520.0f,
                        y1 = 737.0f,
                        x2 = 508.5f,
                        y2 = 748.5f,
                    )
                    quadTo(
                        x1 = 497.0f,
                        y1 = 760.0f,
                        x2 = 480.0f,
                        y2 = 760.0f,
                    )
                    quadTo(
                        x1 = 463.0f,
                        y1 = 760.0f,
                        x2 = 451.5f,
                        y2 = 748.5f,
                    )
                    quadTo(
                        x1 = 440.0f,
                        y1 = 737.0f,
                        x2 = 440.0f,
                        y2 = 720.0f,
                    )
                    lineTo(x = 440.0f, y = 520.0f)
                    close()
                }
            }.build()
            .also { _add24px = it }
    }

@Suppress("ObjectPropertyName")
private var _add24px: ImageVector? = null
