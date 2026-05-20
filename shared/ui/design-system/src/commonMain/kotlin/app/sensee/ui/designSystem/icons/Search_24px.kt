package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Search24px: ImageVector
    get() {
        val current = _search24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.Search24px",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 960.0f,
                viewportHeight = 960.0f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                ) {
                    moveTo(x = 380.0f, y = 640.0f)
                    quadTo(
                        x1 = 271.0f,
                        y1 = 640.0f,
                        x2 = 195.5f,
                        y2 = 564.5f,
                    )
                    quadTo(
                        x1 = 120.0f,
                        y1 = 489.0f,
                        x2 = 120.0f,
                        y2 = 380.0f,
                    )
                    quadTo(
                        x1 = 120.0f,
                        y1 = 271.0f,
                        x2 = 195.5f,
                        y2 = 195.5f,
                    )
                    quadTo(
                        x1 = 271.0f,
                        y1 = 120.0f,
                        x2 = 380.0f,
                        y2 = 120.0f,
                    )
                    quadTo(
                        x1 = 489.0f,
                        y1 = 120.0f,
                        x2 = 564.5f,
                        y2 = 195.5f,
                    )
                    quadTo(
                        x1 = 640.0f,
                        y1 = 271.0f,
                        x2 = 640.0f,
                        y2 = 380.0f,
                    )
                    quadTo(
                        x1 = 640.0f,
                        y1 = 424.0f,
                        x2 = 626.0f,
                        y2 = 463.0f,
                    )
                    quadTo(
                        x1 = 612.0f,
                        y1 = 502.0f,
                        x2 = 588.0f,
                        y2 = 532.0f,
                    )
                    lineTo(x = 812.0f, y = 756.0f)
                    quadTo(
                        x1 = 823.0f,
                        y1 = 767.0f,
                        x2 = 823.0f,
                        y2 = 784.0f,
                    )
                    quadTo(
                        x1 = 823.0f,
                        y1 = 801.0f,
                        x2 = 812.0f,
                        y2 = 812.0f,
                    )
                    quadTo(
                        x1 = 801.0f,
                        y1 = 823.0f,
                        x2 = 784.0f,
                        y2 = 823.0f,
                    )
                    quadTo(
                        x1 = 767.0f,
                        y1 = 823.0f,
                        x2 = 756.0f,
                        y2 = 812.0f,
                    )
                    lineTo(x = 532.0f, y = 588.0f)
                    quadTo(
                        x1 = 502.0f,
                        y1 = 612.0f,
                        x2 = 463.0f,
                        y2 = 626.0f,
                    )
                    quadTo(
                        x1 = 424.0f,
                        y1 = 640.0f,
                        x2 = 380.0f,
                        y2 = 640.0f,
                    )
                    close()
                    moveTo(x = 380.0f, y = 560.0f)
                    quadTo(
                        x1 = 455.0f,
                        y1 = 560.0f,
                        x2 = 507.5f,
                        y2 = 507.5f,
                    )
                    quadTo(
                        x1 = 560.0f,
                        y1 = 455.0f,
                        x2 = 560.0f,
                        y2 = 380.0f,
                    )
                    quadTo(
                        x1 = 560.0f,
                        y1 = 305.0f,
                        x2 = 507.5f,
                        y2 = 252.5f,
                    )
                    quadTo(
                        x1 = 455.0f,
                        y1 = 200.0f,
                        x2 = 380.0f,
                        y2 = 200.0f,
                    )
                    quadTo(
                        x1 = 305.0f,
                        y1 = 200.0f,
                        x2 = 252.5f,
                        y2 = 252.5f,
                    )
                    quadTo(
                        x1 = 200.0f,
                        y1 = 305.0f,
                        x2 = 200.0f,
                        y2 = 380.0f,
                    )
                    quadTo(
                        x1 = 200.0f,
                        y1 = 455.0f,
                        x2 = 252.5f,
                        y2 = 507.5f,
                    )
                    quadTo(
                        x1 = 305.0f,
                        y1 = 560.0f,
                        x2 = 380.0f,
                        y2 = 560.0f,
                    )
                    close()
                }
            }.build()
            .also { _search24px = it }
    }

@Suppress("ObjectPropertyName")
private var _search24px: ImageVector? = null
