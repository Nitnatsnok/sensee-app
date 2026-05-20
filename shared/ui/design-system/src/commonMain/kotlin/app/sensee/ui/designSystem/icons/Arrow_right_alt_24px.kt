package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val ArrowRightAlt24px: ImageVector
    get() {
        val current = _arrowRightAlt24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.ArrowRightAlt24px",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 960.0f,
                viewportHeight = 960.0f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                ) {
                    moveTo(x = 646.0f, y = 520.0f)
                    lineTo(x = 200.0f, y = 520.0f)
                    quadTo(
                        x1 = 183.0f,
                        y1 = 520.0f,
                        x2 = 171.5f,
                        y2 = 508.5f,
                    )
                    quadTo(
                        x1 = 160.0f,
                        y1 = 497.0f,
                        x2 = 160.0f,
                        y2 = 480.0f,
                    )
                    quadTo(
                        x1 = 160.0f,
                        y1 = 463.0f,
                        x2 = 171.5f,
                        y2 = 451.5f,
                    )
                    quadTo(
                        x1 = 183.0f,
                        y1 = 440.0f,
                        x2 = 200.0f,
                        y2 = 440.0f,
                    )
                    lineTo(x = 646.0f, y = 440.0f)
                    lineTo(x = 532.0f, y = 326.0f)
                    quadTo(
                        x1 = 520.0f,
                        y1 = 314.0f,
                        x2 = 520.5f,
                        y2 = 298.0f,
                    )
                    quadTo(
                        x1 = 521.0f,
                        y1 = 282.0f,
                        x2 = 532.0f,
                        y2 = 270.0f,
                    )
                    quadTo(
                        x1 = 544.0f,
                        y1 = 258.0f,
                        x2 = 560.5f,
                        y2 = 257.5f,
                    )
                    quadTo(
                        x1 = 577.0f,
                        y1 = 257.0f,
                        x2 = 589.0f,
                        y2 = 269.0f,
                    )
                    lineTo(x = 772.0f, y = 452.0f)
                    quadTo(
                        x1 = 778.0f,
                        y1 = 458.0f,
                        x2 = 780.5f,
                        y2 = 465.0f,
                    )
                    quadTo(
                        x1 = 783.0f,
                        y1 = 472.0f,
                        x2 = 783.0f,
                        y2 = 480.0f,
                    )
                    quadTo(
                        x1 = 783.0f,
                        y1 = 488.0f,
                        x2 = 780.5f,
                        y2 = 495.0f,
                    )
                    quadTo(
                        x1 = 778.0f,
                        y1 = 502.0f,
                        x2 = 772.0f,
                        y2 = 508.0f,
                    )
                    lineTo(x = 589.0f, y = 691.0f)
                    quadTo(
                        x1 = 577.0f,
                        y1 = 703.0f,
                        x2 = 560.5f,
                        y2 = 702.5f,
                    )
                    quadTo(
                        x1 = 544.0f,
                        y1 = 702.0f,
                        x2 = 532.0f,
                        y2 = 690.0f,
                    )
                    quadTo(
                        x1 = 521.0f,
                        y1 = 678.0f,
                        x2 = 520.5f,
                        y2 = 662.0f,
                    )
                    quadTo(
                        x1 = 520.0f,
                        y1 = 646.0f,
                        x2 = 532.0f,
                        y2 = 634.0f,
                    )
                    lineTo(x = 646.0f, y = 520.0f)
                    close()
                }
            }.build()
            .also { _arrowRightAlt24px = it }
    }

@Suppress("ObjectPropertyName")
private var _arrowRightAlt24px: ImageVector? = null
