package app.sensee.feature.vocabularyEditor.presentation.impl.screen

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics

/**
 * Recycling buckets for the capture list: items sharing a value have the same
 * composition shape, so Compose may reuse a scrolled-away slot for a new item.
 */
internal enum class VocabularyCaptureListContentType {
    SectionHeader,
    Text,
    TextField,
    SelectField,
    Button,
    LoadStatus,
    Status,
    SenseCard,
    ManualSense,
}

/**
 * Stable lazy-list identity for each fixed capture-screen item, paired with its
 * recycling [contentType]. Stable keys keep scroll position and per-item remembered
 * UI state across list mutations; the candidate and manual-sense lists key by sense
 * identity instead.
 */
internal enum class VocabularyCaptureListItem(
    val contentType: VocabularyCaptureListContentType,
) {
    Intro(VocabularyCaptureListContentType.SectionHeader),
    ConfirmedTerm(VocabularyCaptureListContentType.Text),
    CaptureAnother(VocabularyCaptureListContentType.Button),
    TermInput(VocabularyCaptureListContentType.TextField),
    SuggestButton(VocabularyCaptureListContentType.Button),
    GrammarLabelsLoadStatus(VocabularyCaptureListContentType.LoadStatus),
    StatusNote(VocabularyCaptureListContentType.Status),
    SuggestError(VocabularyCaptureListContentType.Status),
    MissedSense(VocabularyCaptureListContentType.SectionHeader),
    ManualTranslation(VocabularyCaptureListContentType.TextField),
    ManualSurfaceForm(VocabularyCaptureListContentType.TextField),
    ManualExample(VocabularyCaptureListContentType.TextField),
    ManualUnitType(VocabularyCaptureListContentType.SelectField),
    ManualAddButton(VocabularyCaptureListContentType.Button),
    ConfirmButton(VocabularyCaptureListContentType.Button),
}

internal fun LazyListScope.captureFrameItem(
    listItem: VocabularyCaptureListItem,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
    content: @Composable () -> Unit,
) {
    item(key = listItem, contentType = listItem.contentType) {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            content()
        }
    }
}
