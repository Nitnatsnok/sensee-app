package app.sensee.ui.designSystem.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.composeunstyled.theme.ThemeToken

/**
 * Corner-radius scale for surfaces, cards and chips. M3-aligned step: 4 / 8 / 12 / 16 / 28.
 * Theme-independent. `CircleShape` lives on [SenseeShapes] directly because it isn't part
 * of the scale and doesn't carry a token.
 */
internal val SenseeShapeScale: Map<ThemeToken<CornerBasedShape>, CornerBasedShape> =
    mapOf(
        SenseeShapeTokens.extraSmall to RoundedCornerShape(4.dp),
        SenseeShapeTokens.small to RoundedCornerShape(8.dp),
        SenseeShapeTokens.medium to RoundedCornerShape(12.dp),
        SenseeShapeTokens.large to RoundedCornerShape(16.dp),
        SenseeShapeTokens.extraLarge to RoundedCornerShape(28.dp),
    )
