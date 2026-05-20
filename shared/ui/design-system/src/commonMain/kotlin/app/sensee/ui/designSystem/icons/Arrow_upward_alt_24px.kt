package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val ArrowUpwardAlt24px: ImageVector
    get() {
        val current = _arrowUpwardAlt24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.ArrowUpwardAlt24px",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 960.0f,
                viewportHeight = 960.0f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                ) {
                    moveTo(x = 440.0f, y = 352.0f)
                    lineTo(x = 324.0f, y = 468.0f)
                    quadTo(
                        x1 = 313.0f,
                        y1 = 479.0f,
                        x2 = 296.0f,
                        y2 = 479.0f,
                    )
                    quadTo(
                        x1 = 279.0f,
                        y1 = 479.0f,
                        x2 = 268.0f,
                        y2 = 468.0f,
                    )
                    quadTo(
                        x1 = 257.0f,
                        y1 = 457.0f,
                        x2 = 257.0f,
                        y2 = 440.0f,
                    )
                    quadTo(
                        x1 = 257.0f,
                        y1 = 423.0f,
                        x2 = 268.0f,
                        y2 = 412.0f,
                    )
                    lineTo(x = 452.0f, y = 228.0f)
                    quadTo(
                        x1 = 464.0f,
                        y1 = 216.0f,
                        x2 = 480.0f,
                        y2 = 216.0f,
                    )
                    quadTo(
                        x1 = 496.0f,
                        y1 = 216.0f,
                        x2 = 508.0f,
                        y2 = 228.0f,
                    )
                    lineTo(x = 692.0f, y = 412.0f)
                    quadTo(
                        x1 = 703.0f,
                        y1 = 423.0f,
                        x2 = 703.0f,
                        y2 = 440.0f,
                    )
                    quadTo(
                        x1 = 703.0f,
                        y1 = 457.0f,
                        x2 = 692.0f,
                        y2 = 468.0f,
                    )
                    quadTo(
                        x1 = 681.0f,
                        y1 = 479.0f,
                        x2 = 664.0f,
                        y2 = 479.0f,
                    )
                    quadTo(
                        x1 = 647.0f,
                        y1 = 479.0f,
                        x2 = 636.0f,
                        y2 = 468.0f,
                    )
                    lineTo(x = 520.0f, y = 352.0f)
                    lineTo(x = 520.0f, y = 680.0f)
                    quadTo(
                        x1 = 520.0f,
                        y1 = 697.0f,
                        x2 = 508.5f,
                        y2 = 708.5f,
                    )
                    quadTo(
                        x1 = 497.0f,
                        y1 = 720.0f,
                        x2 = 480.0f,
                        y2 = 720.0f,
                    )
                    quadTo(
                        x1 = 463.0f,
                        y1 = 720.0f,
                        x2 = 451.5f,
                        y2 = 708.5f,
                    )
                    quadTo(
                        x1 = 440.0f,
                        y1 = 697.0f,
                        x2 = 440.0f,
                        y2 = 680.0f,
                    )
                    lineTo(x = 440.0f, y = 352.0f)
                    close()
                }
            }.build()
            .also { _arrowUpwardAlt24px = it }
    }

@Suppress("ObjectPropertyName")
private var _arrowUpwardAlt24px: ImageVector? = null
