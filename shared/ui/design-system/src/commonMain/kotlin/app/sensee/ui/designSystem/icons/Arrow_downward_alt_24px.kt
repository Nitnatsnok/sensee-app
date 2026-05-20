package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val ArrowDownwardAlt24px: ImageVector
    get() {
        val current = _arrowDownwardAlt24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.ArrowDownwardAlt24px",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 960.0f,
                viewportHeight = 960.0f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                ) {
                    moveTo(x = 440.0f, y = 568.0f)
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
                    lineTo(x = 520.0f, y = 568.0f)
                    lineTo(x = 636.0f, y = 452.0f)
                    quadTo(
                        x1 = 647.0f,
                        y1 = 441.0f,
                        x2 = 664.0f,
                        y2 = 441.0f,
                    )
                    quadTo(
                        x1 = 681.0f,
                        y1 = 441.0f,
                        x2 = 692.0f,
                        y2 = 452.0f,
                    )
                    quadTo(
                        x1 = 703.0f,
                        y1 = 463.0f,
                        x2 = 703.0f,
                        y2 = 480.0f,
                    )
                    quadTo(
                        x1 = 703.0f,
                        y1 = 497.0f,
                        x2 = 692.0f,
                        y2 = 508.0f,
                    )
                    lineTo(x = 508.0f, y = 692.0f)
                    quadTo(
                        x1 = 496.0f,
                        y1 = 704.0f,
                        x2 = 480.0f,
                        y2 = 704.0f,
                    )
                    quadTo(
                        x1 = 464.0f,
                        y1 = 704.0f,
                        x2 = 452.0f,
                        y2 = 692.0f,
                    )
                    lineTo(x = 268.0f, y = 508.0f)
                    quadTo(
                        x1 = 257.0f,
                        y1 = 497.0f,
                        x2 = 257.0f,
                        y2 = 480.0f,
                    )
                    quadTo(
                        x1 = 257.0f,
                        y1 = 463.0f,
                        x2 = 268.0f,
                        y2 = 452.0f,
                    )
                    quadTo(
                        x1 = 279.0f,
                        y1 = 441.0f,
                        x2 = 296.0f,
                        y2 = 441.0f,
                    )
                    quadTo(
                        x1 = 313.0f,
                        y1 = 441.0f,
                        x2 = 324.0f,
                        y2 = 452.0f,
                    )
                    lineTo(x = 440.0f, y = 568.0f)
                    close()
                }
            }.build()
            .also { _arrowDownwardAlt24px = it }
    }

@Suppress("ObjectPropertyName")
private var _arrowDownwardAlt24px: ImageVector? = null
