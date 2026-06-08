package app.sensee.ui.senseCard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.MapTextProvider
import app.sensee.core.presentation.text.TextKey
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback

/**
 * Structural labels owned by [SenseCard] itself — the expand toggle and the section captions
 * for the secondary grammar detail. They belong to the card (not to any one feature), so the
 * card resolves them from its own [DefaultSenseCardTextProvider]; the ambient
 * [LocalTextProvider] stays the fallback for anything the card does not define.
 */
public object SenseCardTextKeys {
    public val ToggleShow: TextKey = TextKey("sense_card.detail_toggle_show")
    public val ToggleHide: TextKey = TextKey("sense_card.detail_toggle_hide")
    public val Prepositions: TextKey = TextKey("sense_card.detail_prepositions")
    public val Complementation: TextKey = TextKey("sense_card.detail_complementation")
    public val Grammar: TextKey = TextKey("sense_card.detail_grammar")
    public val Usage: TextKey = TextKey("sense_card.detail_usage")
    public val Forms: TextKey = TextKey("sense_card.detail_forms")
    public val Note: TextKey = TextKey("sense_card.detail_note")
}

internal val DefaultSenseCardTextProvider: TextProvider =
    MapTextProvider(
        mapOf(
            SenseCardTextKeys.ToggleShow to "Подробнее",
            SenseCardTextKeys.ToggleHide to "Свернуть",
            SenseCardTextKeys.Prepositions to "Предлоги",
            SenseCardTextKeys.Complementation to "Дополнение",
            SenseCardTextKeys.Grammar to "Грамматика",
            SenseCardTextKeys.Usage to "Употребление",
            SenseCardTextKeys.Forms to "Формы",
            SenseCardTextKeys.Note to "Примечание",
        ),
    )

@Composable
internal fun rememberSenseCardTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultSenseCardTextProvider.withFallback(parent) }
}
