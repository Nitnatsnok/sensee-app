package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Close24px: ImageVector
    get() {
        val current = _close24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.Close24px",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 960.0f,
                viewportHeight = 960.0f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                ) {
                    moveTo(x = 480.0f, y = 536.0f)
                    lineTo(x = 284.0f, y = 732.0f)
                    quadTo(
                        x1 = 273.0f,
                        y1 = 743.0f,
                        x2 = 256.0f,
                        y2 = 743.0f,
                    )
                    quadTo(
                        x1 = 239.0f,
                        y1 = 743.0f,
                        x2 = 228.0f,
                        y2 = 732.0f,
                    )
                    quadTo(
                        x1 = 217.0f,
                        y1 = 721.0f,
                        x2 = 217.0f,
                        y2 = 704.0f,
                    )
                    quadTo(
                        x1 = 217.0f,
                        y1 = 687.0f,
                        x2 = 228.0f,
                        y2 = 676.0f,
                    )
                    lineTo(x = 424.0f, y = 480.0f)
                    lineTo(x = 228.0f, y = 284.0f)
                    quadTo(
                        x1 = 217.0f,
                        y1 = 273.0f,
                        x2 = 217.0f,
                        y2 = 256.0f,
                    )
                    quadTo(
                        x1 = 217.0f,
                        y1 = 239.0f,
                        x2 = 228.0f,
                        y2 = 228.0f,
                    )
                    quadTo(
                        x1 = 239.0f,
                        y1 = 217.0f,
                        x2 = 256.0f,
                        y2 = 217.0f,
                    )
                    quadTo(
                        x1 = 273.0f,
                        y1 = 217.0f,
                        x2 = 284.0f,
                        y2 = 228.0f,
                    )
                    lineTo(x = 480.0f, y = 424.0f)
                    lineTo(x = 676.0f, y = 228.0f)
                    quadTo(
                        x1 = 687.0f,
                        y1 = 217.0f,
                        x2 = 704.0f,
                        y2 = 217.0f,
                    )
                    quadTo(
                        x1 = 721.0f,
                        y1 = 217.0f,
                        x2 = 732.0f,
                        y2 = 228.0f,
                    )
                    quadTo(
                        x1 = 743.0f,
                        y1 = 239.0f,
                        x2 = 743.0f,
                        y2 = 256.0f,
                    )
                    quadTo(
                        x1 = 743.0f,
                        y1 = 273.0f,
                        x2 = 732.0f,
                        y2 = 284.0f,
                    )
                    lineTo(x = 536.0f, y = 480.0f)
                    lineTo(x = 732.0f, y = 676.0f)
                    quadTo(
                        x1 = 743.0f,
                        y1 = 687.0f,
                        x2 = 743.0f,
                        y2 = 704.0f,
                    )
                    quadTo(
                        x1 = 743.0f,
                        y1 = 721.0f,
                        x2 = 732.0f,
                        y2 = 732.0f,
                    )
                    quadTo(
                        x1 = 721.0f,
                        y1 = 743.0f,
                        x2 = 704.0f,
                        y2 = 743.0f,
                    )
                    quadTo(
                        x1 = 687.0f,
                        y1 = 743.0f,
                        x2 = 676.0f,
                        y2 = 732.0f,
                    )
                    lineTo(x = 480.0f, y = 536.0f)
                    close()
                }
            }.build()
            .also { _close24px = it }
    }

@Suppress("ObjectPropertyName")
private var _close24px: ImageVector? = null
