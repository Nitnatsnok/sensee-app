package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val ArrowBack24px: ImageVector
    get() {
        val current = _arrowBack24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.ArrowBack24px",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 960.0f,
                viewportHeight = 960.0f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                ) {
                    moveTo(x = 313.0f, y = 520.0f)
                    lineTo(x = 509.0f, y = 716.0f)
                    quadTo(
                        x1 = 521.0f,
                        y1 = 728.0f,
                        x2 = 520.5f,
                        y2 = 744.0f,
                    )
                    quadTo(
                        x1 = 520.0f,
                        y1 = 760.0f,
                        x2 = 508.0f,
                        y2 = 772.0f,
                    )
                    quadTo(
                        x1 = 496.0f,
                        y1 = 783.0f,
                        x2 = 480.0f,
                        y2 = 783.5f,
                    )
                    quadTo(
                        x1 = 464.0f,
                        y1 = 784.0f,
                        x2 = 452.0f,
                        y2 = 772.0f,
                    )
                    lineTo(x = 188.0f, y = 508.0f)
                    quadTo(
                        x1 = 182.0f,
                        y1 = 502.0f,
                        x2 = 179.5f,
                        y2 = 495.0f,
                    )
                    quadTo(
                        x1 = 177.0f,
                        y1 = 488.0f,
                        x2 = 177.0f,
                        y2 = 480.0f,
                    )
                    quadTo(
                        x1 = 177.0f,
                        y1 = 472.0f,
                        x2 = 179.5f,
                        y2 = 465.0f,
                    )
                    quadTo(
                        x1 = 182.0f,
                        y1 = 458.0f,
                        x2 = 188.0f,
                        y2 = 452.0f,
                    )
                    lineTo(x = 452.0f, y = 188.0f)
                    quadTo(
                        x1 = 463.0f,
                        y1 = 177.0f,
                        x2 = 479.5f,
                        y2 = 177.0f,
                    )
                    quadTo(
                        x1 = 496.0f,
                        y1 = 177.0f,
                        x2 = 508.0f,
                        y2 = 188.0f,
                    )
                    quadTo(
                        x1 = 520.0f,
                        y1 = 200.0f,
                        x2 = 520.0f,
                        y2 = 216.5f,
                    )
                    quadTo(
                        x1 = 520.0f,
                        y1 = 233.0f,
                        x2 = 508.0f,
                        y2 = 245.0f,
                    )
                    lineTo(x = 313.0f, y = 440.0f)
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
                    lineTo(x = 313.0f, y = 520.0f)
                    close()
                }
            }.build()
            .also { _arrowBack24px = it }
    }

@Suppress("ObjectPropertyName")
private var _arrowBack24px: ImageVector? = null
