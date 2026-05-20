package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val ArrowLeftAlt24px: ImageVector
    get() {
        val current = _arrowLeftAlt24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.ArrowLeftAlt24px",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 960.0f,
                viewportHeight = 960.0f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                ) {
                    moveTo(x = 314.0f, y = 520.0f)
                    lineTo(x = 428.0f, y = 634.0f)
                    quadTo(
                        x1 = 440.0f,
                        y1 = 646.0f,
                        x2 = 439.5f,
                        y2 = 662.0f,
                    )
                    quadTo(
                        x1 = 439.0f,
                        y1 = 678.0f,
                        x2 = 428.0f,
                        y2 = 690.0f,
                    )
                    quadTo(
                        x1 = 416.0f,
                        y1 = 702.0f,
                        x2 = 399.5f,
                        y2 = 702.5f,
                    )
                    quadTo(
                        x1 = 383.0f,
                        y1 = 703.0f,
                        x2 = 371.0f,
                        y2 = 691.0f,
                    )
                    lineTo(x = 188.0f, y = 508.0f)
                    quadTo(
                        x1 = 176.0f,
                        y1 = 496.0f,
                        x2 = 176.0f,
                        y2 = 480.0f,
                    )
                    quadTo(
                        x1 = 176.0f,
                        y1 = 464.0f,
                        x2 = 188.0f,
                        y2 = 452.0f,
                    )
                    lineTo(x = 371.0f, y = 269.0f)
                    quadTo(
                        x1 = 383.0f,
                        y1 = 257.0f,
                        x2 = 399.5f,
                        y2 = 257.5f,
                    )
                    quadTo(
                        x1 = 416.0f,
                        y1 = 258.0f,
                        x2 = 428.0f,
                        y2 = 270.0f,
                    )
                    quadTo(
                        x1 = 439.0f,
                        y1 = 282.0f,
                        x2 = 439.5f,
                        y2 = 298.0f,
                    )
                    quadTo(
                        x1 = 440.0f,
                        y1 = 314.0f,
                        x2 = 428.0f,
                        y2 = 326.0f,
                    )
                    lineTo(x = 314.0f, y = 440.0f)
                    lineTo(x = 760.0f, y = 440.0f)
                    quadTo(
                        x1 = 777.0f,
                        y1 = 440.0f,
                        x2 = 788.5f,
                        y2 = 451.5f,
                    )
                    quadTo(
                        x1 = 800.0f,
                        y1 = 463.0f,
                        x2 = 800.0f,
                        y2 = 480.0f,
                    )
                    quadTo(
                        x1 = 800.0f,
                        y1 = 497.0f,
                        x2 = 788.5f,
                        y2 = 508.5f,
                    )
                    quadTo(
                        x1 = 777.0f,
                        y1 = 520.0f,
                        x2 = 760.0f,
                        y2 = 520.0f,
                    )
                    lineTo(x = 314.0f, y = 520.0f)
                    close()
                }
            }.build()
            .also { _arrowLeftAlt24px = it }
    }

@Suppress("ObjectPropertyName")
private var _arrowLeftAlt24px: ImageVector? = null
