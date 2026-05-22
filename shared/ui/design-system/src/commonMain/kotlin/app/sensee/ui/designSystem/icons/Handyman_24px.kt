package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Handyman24px: ImageVector
    get() {
        val current = _handyman24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.Handyman24px",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Bevel,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(18.48f, 21.91f)
                    quadTo(18.3f, 21.85f, 18.15f, 21.7f)
                    lineToRelative(-5.1f, -5.1f)
                    quadTo(12.9f, 16.45f, 12.84f, 16.27f)
                    reflectiveQuadTo(12.78f, 15.9f)
                    reflectiveQuadToRelative(0.06f, -0.38f)
                    reflectiveQuadTo(13.05f, 15.2f)
                    lineToRelative(2.13f, -2.13f)
                    quadToRelative(0.15f, -0.15f, 0.32f, -0.21f)
                    reflectiveQuadTo(15.88f, 12.8f)
                    reflectiveQuadToRelative(0.38f, 0.06f)
                    reflectiveQuadToRelative(0.32f, 0.21f)
                    lineToRelative(5.1f, 5.1f)
                    quadToRelative(0.15f, 0.15f, 0.21f, 0.32f)
                    reflectiveQuadToRelative(0.06f, 0.38f)
                    reflectiveQuadToRelative(-0.06f, 0.38f)
                    reflectiveQuadToRelative(-0.21f, 0.32f)
                    lineTo(19.55f, 21.7f)
                    quadToRelative(-0.15f, 0.15f, -0.32f, 0.21f)
                    reflectiveQuadToRelative(-0.38f, 0.06f)
                    reflectiveQuadTo(18.48f, 21.91f)
                    close()
                    moveTo(18.85f, 19.6f)
                    lineToRelative(0.73f, -0.73f)
                    lineTo(15.9f, 15.2f)
                    lineToRelative(-0.72f, 0.72f)
                    lineToRelative(3.68f, 3.68f)
                    close()
                    moveTo(4.74f, 21.93f)
                    quadTo(4.55f, 21.85f, 4.4f, 21.7f)
                    lineTo(2.3f, 19.6f)
                    quadTo(2.15f, 19.45f, 2.08f, 19.26f)
                    reflectiveQuadTo(2f, 18.88f)
                    reflectiveQuadTo(2.08f, 18.5f)
                    reflectiveQuadTo(2.3f, 18.18f)
                    lineToRelative(5.3f, -5.3f)
                    horizontalLineTo(9.73f)
                    lineToRelative(0.85f, -0.85f)
                    lineTo(6.45f, 7.9f)
                    horizontalLineTo(5.03f)
                    lineTo(2f, 4.88f)
                    lineTo(4.83f, 2.05f)
                    lineTo(7.85f, 5.07f)
                    verticalLineTo(6.5f)
                    lineToRelative(4.13f, 4.13f)
                    lineToRelative(2.9f, -2.9f)
                    lineTo(13.8f, 6.65f)
                    lineToRelative(1.4f, -1.4f)
                    horizontalLineTo(12.38f)
                    lineToRelative(-0.7f, -0.7f)
                    lineTo(15.23f, 1f)
                    lineToRelative(0.7f, 0.7f)
                    verticalLineTo(4.52f)
                    lineToRelative(1.4f, -1.4f)
                    lineToRelative(3.55f, 3.55f)
                    quadTo(21.3f, 7.1f, 21.53f, 7.64f)
                    quadToRelative(0.22f, 0.54f, 0.22f, 1.14f)
                    quadToRelative(0f, 0.6f, -0.22f, 1.15f)
                    quadTo(21.3f, 10.48f, 20.88f, 10.9f)
                    lineTo(18.75f, 8.77f)
                    lineToRelative(-1.4f, 1.4f)
                    lineTo(16.3f, 9.13f)
                    lineTo(11.13f, 14.3f)
                    verticalLineToRelative(2.1f)
                    lineToRelative(-5.3f, 5.3f)
                    quadTo(5.68f, 21.85f, 5.5f, 21.93f)
                    quadTo(5.33f, 22f, 5.13f, 22f)
                    reflectiveQuadTo(4.74f, 21.93f)
                    close()
                    moveTo(5.13f, 19.6f)
                    lineTo(9.38f, 15.35f)
                    verticalLineTo(14.63f)
                    horizontalLineTo(8.65f)
                    lineTo(4.4f, 18.88f)
                    lineTo(5.13f, 19.6f)
                    close()
                    moveToRelative(0f, 0f)
                    lineTo(4.4f, 18.88f)
                    lineToRelative(0.38f, 0.35f)
                    lineTo(5.13f, 19.6f)
                    close()
                    moveToRelative(13.73f, 0f)
                    lineToRelative(0.73f, -0.73f)
                    lineTo(18.85f, 19.6f)
                    close()
                }
            }.build()
            .also { _handyman24px = it }
    }

@Suppress("ObjectPropertyName")
private var _handyman24px: ImageVector? = null
