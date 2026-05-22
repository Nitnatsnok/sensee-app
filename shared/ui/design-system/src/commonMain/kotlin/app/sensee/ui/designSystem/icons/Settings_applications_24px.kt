package app.sensee.ui.designSystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val SettingsApplications24px: ImageVector
    get() {
        val current = _settingsApplications24px
        if (current != null) return current

        return ImageVector
            .Builder(
                name = "app.sensee.ui.designSystem.theme.SenseeTheme.SettingsApplications24px",
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
                    moveTo(11f, 17f)
                    horizontalLineToRelative(2f)
                    lineToRelative(0.3f, -1.5f)
                    quadToRelative(0.3f, -0.13f, 0.56f, -0.26f)
                    reflectiveQuadTo(14.4f, 14.9f)
                    lineToRelative(1.45f, 0.45f)
                    lineToRelative(1f, -1.7f)
                    lineToRelative(-1.15f, -1f)
                    quadTo(15.75f, 12.3f, 15.75f, 12f)
                    reflectiveQuadTo(15.7f, 11.35f)
                    lineToRelative(1.15f, -1f)
                    lineToRelative(-1f, -1.7f)
                    lineTo(14.4f, 9.1f)
                    quadTo(14.13f, 8.9f, 13.86f, 8.76f)
                    reflectiveQuadTo(13.3f, 8.5f)
                    lineTo(13f, 7f)
                    horizontalLineTo(11f)
                    lineTo(10.7f, 8.5f)
                    quadTo(10.4f, 8.63f, 10.14f, 8.76f)
                    reflectiveQuadTo(9.6f, 9.1f)
                    lineTo(8.15f, 8.65f)
                    lineToRelative(-1f, 1.7f)
                    lineToRelative(1.15f, 1f)
                    quadTo(8.25f, 11.7f, 8.25f, 12f)
                    reflectiveQuadTo(8.3f, 12.65f)
                    lineToRelative(-1.15f, 1f)
                    lineToRelative(1f, 1.7f)
                    lineTo(9.6f, 14.9f)
                    quadToRelative(0.27f, 0.2f, 0.54f, 0.34f)
                    quadToRelative(0.26f, 0.14f, 0.56f, 0.26f)
                    lineTo(11f, 17f)
                    close()
                    moveTo(10.59f, 13.41f)
                    quadTo(10f, 12.83f, 10f, 12f)
                    reflectiveQuadToRelative(0.59f, -1.41f)
                    reflectiveQuadTo(12f, 10f)
                    reflectiveQuadToRelative(1.41f, 0.59f)
                    quadTo(14f, 11.18f, 14f, 12f)
                    reflectiveQuadToRelative(-0.59f, 1.41f)
                    reflectiveQuadTo(12f, 14f)
                    reflectiveQuadTo(10.59f, 13.41f)
                    close()
                    moveTo(5f, 21f)
                    quadTo(4.18f, 21f, 3.59f, 20.41f)
                    reflectiveQuadTo(3f, 19f)
                    verticalLineTo(5f)
                    quadTo(3f, 4.17f, 3.59f, 3.59f)
                    reflectiveQuadTo(5f, 3f)
                    horizontalLineTo(19f)
                    quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                    reflectiveQuadTo(21f, 5f)
                    verticalLineTo(19f)
                    quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                    reflectiveQuadTo(19f, 21f)
                    horizontalLineTo(5f)
                    close()
                    moveTo(5f, 19f)
                    horizontalLineTo(19f)
                    verticalLineTo(5f)
                    horizontalLineTo(5f)
                    verticalLineTo(19f)
                    close()
                    moveTo(5f, 5f)
                    verticalLineTo(19f)
                    verticalLineTo(5f)
                    close()
                }
            }.build()
            .also { _settingsApplications24px = it }
    }

@Suppress("ObjectPropertyName")
private var _settingsApplications24px: ImageVector? = null
