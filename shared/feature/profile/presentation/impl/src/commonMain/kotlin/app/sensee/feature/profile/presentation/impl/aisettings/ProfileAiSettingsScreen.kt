package app.sensee.feature.profile.presentation.impl.aisettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsAction
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsComponent
import app.sensee.feature.profile.presentation.api.ProfileAiSettingsUiState
import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.TtsProvider
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.selectField.SenseeSelectField
import app.sensee.ui.designSystem.component.textField.SenseeTextField
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import com.composeunstyled.Text

@Composable
public fun ProfileAiSettingsScreen(
    component: ProfileAiSettingsComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberProfileAiSettingsTextProvider(),
) {
    val uiState by component.uiState.collectAsState()
    val formState = rememberProfileAiSettingsFormState()
    val spacing = SenseeTheme.spacing
    val layoutMetrics = LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics()

    SyncProfileForm(uiState, formState, component::onAction)

    val showSaved = uiState.isSaved && !uiState.isDirty

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = layoutMetrics.screenHorizontalPadding,
                    top = layoutMetrics.screenVerticalPadding,
                    end = layoutMetrics.screenHorizontalPadding,
                    bottom = layoutMetrics.screenVerticalPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            val listContext =
                ProfileAiSettingsListContext(
                    component = component,
                    uiState = uiState,
                    formState = formState,
                    textProvider = textProvider,
                    layoutMetrics = layoutMetrics,
                )
            aiSection(listContext)
            ttsSection(listContext)
            saveItem(listContext, showSaved)
        }
    }
}

@Composable
internal fun rememberProfileAiSettingsTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultProfileAiSettingsTextProvider.withFallback(parent) }
}

private fun LazyListScope.aiSection(context: ProfileAiSettingsListContext) {
    val component = context.component
    val uiState = context.uiState
    val formState = context.formState
    val textProvider = context.textProvider
    val layoutMetrics = context.layoutMetrics

    frameItem(ProfileAiSettingsListItem.AiSection, layoutMetrics) {
        SectionHeader(
            title = textProvider.text(ProfileAiSettingsTextKeys.SectionAi),
            hint = textProvider.text(ProfileAiSettingsTextKeys.AiHint),
        )
    }
    frameItem(ProfileAiSettingsListItem.AiProvider, layoutMetrics) {
        SenseeSelectField(
            label = textProvider.text(ProfileAiSettingsTextKeys.AiProvider),
            selected = uiState.draftSnapshot.aiProvider,
            options = uiState.providers,
            optionLabel = { it.displayName },
            onSelect = { component.onAction(ProfileAiSettingsAction.SetAiProvider(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    frameItem(ProfileAiSettingsListItem.AiApiKey, layoutMetrics) {
        SenseeTextField(
            state = formState.aiApiKey,
            accessibilityLabel = textProvider.text(ProfileAiSettingsTextKeys.AiApiKey),
            label = { Text(textProvider.text(ProfileAiSettingsTextKeys.AiApiKey)) },
            secure = true,
        )
    }
    frameItem(ProfileAiSettingsListItem.AiVerify, layoutMetrics) {
        VerifyRow(
            status = uiState.effectiveAiKeyCheck(),
            textProvider = textProvider,
            onVerify = { component.onAction(ProfileAiSettingsAction.VerifyAiKey) },
        )
    }
    aiResultItems(context)
}

private fun LazyListScope.aiResultItems(context: ProfileAiSettingsListContext) {
    val uiState = context.uiState
    val formState = context.formState
    val textProvider = context.textProvider
    val layoutMetrics = context.layoutMetrics

    val aiCheck = uiState.effectiveAiKeyCheck()
    if (aiCheck.hasResult()) {
        frameItem(ProfileAiSettingsListItem.AiKeyCheckMessage, layoutMetrics) {
            KeyCheckMessage(aiCheck, textProvider)
        }
        frameItem(ProfileAiSettingsListItem.AiModel, layoutMetrics) {
            ModelField(
                status = aiCheck,
                label = textProvider.text(ProfileAiSettingsTextKeys.AiModel),
                options = uiState.availableAiModels,
                state = formState.aiModel,
            )
        }
    }
}

private fun LazyListScope.ttsSection(context: ProfileAiSettingsListContext) {
    val component = context.component
    val uiState = context.uiState
    val formState = context.formState
    val textProvider = context.textProvider
    val layoutMetrics = context.layoutMetrics

    frameItem(ProfileAiSettingsListItem.TtsSection, layoutMetrics) {
        SectionHeader(title = textProvider.text(ProfileAiSettingsTextKeys.SectionTts))
    }
    frameItem(ProfileAiSettingsListItem.TtsProvider, layoutMetrics) {
        SenseeSelectField(
            label = textProvider.text(ProfileAiSettingsTextKeys.TtsProvider),
            selected = uiState.draftSnapshot.ttsProvider,
            options = uiState.ttsProviders,
            optionLabel = { it.displayName },
            onSelect = { component.onAction(ProfileAiSettingsAction.SetTtsProvider(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (uiState.draftSnapshot.ttsInheritsAiKey) {
        ttsInheritedKeyBlock(component, uiState, textProvider, layoutMetrics)
    } else {
        ttsSeparateKeyBlock(component, uiState, formState, textProvider, layoutMetrics)
    }

    ttsResultItems(context)
}

private fun LazyListScope.ttsResultItems(context: ProfileAiSettingsListContext) {
    val uiState = context.uiState
    val formState = context.formState
    val textProvider = context.textProvider
    val layoutMetrics = context.layoutMetrics

    val ttsCheck = uiState.effectiveTtsKeyCheck()
    if (ttsCheck.hasResult()) {
        frameItem(ProfileAiSettingsListItem.TtsModel, layoutMetrics) {
            ModelField(
                status = ttsCheck,
                label = textProvider.text(ProfileAiSettingsTextKeys.TtsModel),
                options = uiState.effectiveAvailableTtsModels(),
                state = formState.ttsModel,
            )
        }
        frameItem(ProfileAiSettingsListItem.TtsVoice, layoutMetrics) {
            ModelField(
                status = ttsCheck,
                label = textProvider.text(ProfileAiSettingsTextKeys.TtsVoice),
                options = uiState.effectiveAvailableTtsVoices(),
                state = formState.ttsVoiceId,
            )
        }
    }
}

internal data class ProfileAiSettingsListContext(
    val component: ProfileAiSettingsComponent,
    val uiState: ProfileAiSettingsUiState,
    val formState: ProfileAiSettingsFormState,
    val textProvider: TextProvider,
    val layoutMetrics: SenseeAdaptiveLayoutMetrics,
)

private fun LazyListScope.ttsInheritedKeyBlock(
    component: ProfileAiSettingsComponent,
    uiState: ProfileAiSettingsUiState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    frameItem(ProfileAiSettingsListItem.TtsInheritedKeyHint, layoutMetrics) {
        BodyMutedText(textProvider.text(ProfileAiSettingsTextKeys.TtsUsesAiKey))
    }
    val aiCheck = uiState.effectiveAiKeyCheck()
    if (aiCheck.hasResult()) {
        frameItem(ProfileAiSettingsListItem.TtsInheritedAiKeyCheckMessage, layoutMetrics) {
            KeyCheckMessage(aiCheck, textProvider)
        }
    }
    frameItem(ProfileAiSettingsListItem.TtsUseSeparateKey, layoutMetrics) {
        SenseeButton(
            onClick = { component.onAction(ProfileAiSettingsAction.SetTtsSeparateKey(true)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(textProvider.text(ProfileAiSettingsTextKeys.TtsUseSeparateKey))
        }
    }
}

private fun LazyListScope.ttsSeparateKeyBlock(
    component: ProfileAiSettingsComponent,
    uiState: ProfileAiSettingsUiState,
    formState: ProfileAiSettingsFormState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    frameItem(ProfileAiSettingsListItem.TtsApiKey, layoutMetrics) {
        SenseeTextField(
            state = formState.ttsApiKey,
            accessibilityLabel = textProvider.text(ProfileAiSettingsTextKeys.TtsApiKey),
            label = { Text(textProvider.text(ProfileAiSettingsTextKeys.TtsApiKey)) },
            secure = true,
        )
    }
    frameItem(ProfileAiSettingsListItem.TtsVerify, layoutMetrics) {
        VerifyRow(
            status = uiState.effectiveTtsKeyCheck(),
            textProvider = textProvider,
            onVerify = { component.onAction(ProfileAiSettingsAction.VerifyTtsKey) },
        )
    }
    val ttsCheck = uiState.effectiveTtsKeyCheck()
    if (ttsCheck.hasResult()) {
        frameItem(ProfileAiSettingsListItem.TtsKeyCheckMessage, layoutMetrics) {
            KeyCheckMessage(ttsCheck, textProvider)
        }
    }
    val draft = uiState.draftSnapshot
    if (draft.ttsProvider == TtsProvider.OpenAi && draft.aiProvider == AiProvider.OpenAi) {
        frameItem(ProfileAiSettingsListItem.TtsUseAiKey, layoutMetrics) {
            SenseeButton(
                onClick = { component.onAction(ProfileAiSettingsAction.SetTtsSeparateKey(false)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(textProvider.text(ProfileAiSettingsTextKeys.TtsUseAiKey))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    hint: String? = null,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Text(text = title, color = colors.textPrimary, style = typography.titleLarge)
        if (hint != null) {
            Text(text = hint, color = colors.textMuted, style = typography.bodyMedium)
        }
    }
}

@Composable
private fun BodyMutedText(text: String) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    Text(text = text, color = colors.textSecondary, style = typography.bodyMedium)
}
