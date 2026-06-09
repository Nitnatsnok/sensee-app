package app.sensee.feature.profile.presentation.impl.aisettings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics

/**
 * Recycling buckets for the AI-settings list: items sharing a value have the same
 * composition shape, so Compose may reuse a scrolled-away slot for a new item.
 */
internal enum class ProfileAiSettingsListContentType {
    SectionHeader,
    SelectField,
    TextField,
    ModelField,
    Button,
    Status,
}

/**
 * Stable lazy-list identity for each AI-settings item, paired with its recycling
 * [contentType]. Stable keys keep scroll position and per-item remembered UI state
 * as result rows appear and disappear when key checks resolve.
 */
internal enum class ProfileAiSettingsListItem(
    val contentType: ProfileAiSettingsListContentType,
) {
    AiSection(ProfileAiSettingsListContentType.SectionHeader),
    AiProvider(ProfileAiSettingsListContentType.SelectField),
    AiApiKey(ProfileAiSettingsListContentType.TextField),
    AiVerify(ProfileAiSettingsListContentType.Button),
    AiKeyCheckMessage(ProfileAiSettingsListContentType.Status),
    AiModel(ProfileAiSettingsListContentType.ModelField),
    TtsSection(ProfileAiSettingsListContentType.SectionHeader),
    TtsProvider(ProfileAiSettingsListContentType.SelectField),
    TtsInheritedKeyHint(ProfileAiSettingsListContentType.Status),
    TtsInheritedAiKeyCheckMessage(ProfileAiSettingsListContentType.Status),
    TtsUseSeparateKey(ProfileAiSettingsListContentType.Button),
    TtsApiKey(ProfileAiSettingsListContentType.TextField),
    TtsVerify(ProfileAiSettingsListContentType.Button),
    TtsKeyCheckMessage(ProfileAiSettingsListContentType.Status),
    TtsUseAiKey(ProfileAiSettingsListContentType.Button),
    TtsModel(ProfileAiSettingsListContentType.ModelField),
    TtsVoice(ProfileAiSettingsListContentType.ModelField),
    Save(ProfileAiSettingsListContentType.Button),
}

internal fun LazyListScope.frameItem(
    listItem: ProfileAiSettingsListItem,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
    content: @Composable () -> Unit,
) = item(key = listItem, contentType = listItem.contentType) {
    SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
        content()
    }
}
