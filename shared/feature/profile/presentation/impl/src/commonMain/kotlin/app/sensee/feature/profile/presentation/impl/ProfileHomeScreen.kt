package app.sensee.feature.profile.presentation.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.profile.presentation.api.ProfileHomeAction
import app.sensee.feature.profile.presentation.api.ProfileHomeComponent
import app.sensee.feature.profile.presentation.api.ProfileHomeUiState
import app.sensee.settings.domain.AiProvider
import app.sensee.settings.domain.TtsProvider
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.component.selectField.SenseeSelectField
import app.sensee.ui.designSystem.component.textField.SenseeTextField
import app.sensee.ui.designSystem.theme.LocalSenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.senseeCompactLayoutMetrics
import com.composeunstyled.Text
import kotlinx.coroutines.flow.drop

@Composable
public fun ProfileHomeScreen(
    component: ProfileHomeComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberProfileHomeTextProvider(),
) {
    val uiState by component.uiState.collectAsState()
    val formState = rememberProfileFormState()
    val colors = SenseeTheme.colors
    val spacing = SenseeTheme.spacing
    val layoutMetrics = LocalSenseeAdaptiveLayoutMetrics.current ?: senseeCompactLayoutMetrics()

    SyncProfileForm(uiState, formState, component::onAction)

    val showSaved = uiState.isSaved && !uiState.isDirty

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
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
                ProfileHomeListContext(
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

/**
 * TextFieldState mirrors for the five text fields the design-system requires us
 * to drive with [TextFieldState] ([app.sensee.ui.designSystem.component.textField.SenseeTextField]
 * has no value/onValueChange overload). All draft values live in
 * [ProfileHomeUiState.draftSnapshot] — this class is the UI-side handle only,
 * not the source of truth.
 */
@Stable
internal class ProfileFormState(
    val aiApiKey: TextFieldState,
    val aiModel: TextFieldState,
    val ttsApiKey: TextFieldState,
    val ttsModel: TextFieldState,
    val ttsVoiceId: TextFieldState,
)

@Composable
internal fun rememberProfileFormState(): ProfileFormState {
    val aiApiKey = rememberTextFieldState()
    val aiModel = rememberTextFieldState()
    val ttsApiKey = rememberTextFieldState()
    val ttsModel = rememberTextFieldState()
    val ttsVoiceId = rememberTextFieldState()
    return remember { ProfileFormState(aiApiKey, aiModel, ttsApiKey, ttsModel, ttsVoiceId) }
}

@Composable
private fun SyncProfileForm(
    uiState: ProfileHomeUiState,
    formState: ProfileFormState,
    onAction: (ProfileHomeAction) -> Unit,
) {
    formState.aiApiKey.bindToDraft(uiState.draftSnapshot.aiApiKey) {
        onAction(ProfileHomeAction.SetAiApiKey(it))
    }
    formState.aiModel.bindToDraft(uiState.draftSnapshot.aiModel) {
        onAction(ProfileHomeAction.SetAiModel(it))
    }
    formState.ttsApiKey.bindToDraft(uiState.draftSnapshot.ttsApiKey) {
        onAction(ProfileHomeAction.SetTtsApiKey(it))
    }
    formState.ttsModel.bindToDraft(uiState.draftSnapshot.ttsModel) {
        onAction(ProfileHomeAction.SetTtsModel(it))
    }
    formState.ttsVoiceId.bindToDraft(uiState.draftSnapshot.ttsVoiceId) {
        onAction(ProfileHomeAction.SetTtsVoiceId(it))
    }
}

/**
 * Two-way binding between a [TextFieldState] (UI-owned, required by the
 * design-system) and a draft string in [ProfileHomeUiState]. Downstream: any
 * `draftValue` change (load, save, Logic reset) is applied to [TextFieldState];
 * `setTextIfDifferent` keeps echoes from re-triggering the upstream. Upstream:
 * every keystroke becomes an action; `drop(1)` skips the first snapshot which
 * is the value the downstream just placed, not a user edit.
 *
 * `rememberUpdatedState` is required because the upstream effect is keyed on
 * the stable receiver and never restarts; without it the captured callback
 * would hold the first composition's reference.
 */
@Composable
private fun TextFieldState.bindToDraft(
    draftValue: String,
    onChange: (String) -> Unit,
) {
    val currentOnChange by rememberUpdatedState(onChange)

    LaunchedEffect(this, draftValue) {
        setTextIfDifferent(draftValue)
    }
    LaunchedEffect(this) {
        snapshotFlow { text.toString() }
            .drop(1)
            .collect { currentOnChange(it) }
    }
}

private fun TextFieldState.setTextIfDifferent(value: String) {
    if (text.toString() != value) {
        setTextAndPlaceCursorAtEnd(value)
    }
}

private fun LazyListScope.aiSection(context: ProfileHomeListContext) {
    val component = context.component
    val uiState = context.uiState
    val formState = context.formState
    val textProvider = context.textProvider
    val layoutMetrics = context.layoutMetrics

    frameItem(layoutMetrics) {
        SectionHeader(
            title = textProvider.text(ProfileHomeTextKeys.SectionAi),
            hint = textProvider.text(ProfileHomeTextKeys.AiHint),
        )
    }
    frameItem(layoutMetrics) {
        SenseeSelectField(
            label = textProvider.text(ProfileHomeTextKeys.AiProvider),
            selected = uiState.draftSnapshot.aiProvider,
            options = uiState.providers,
            optionLabel = { it.displayName },
            onSelect = { component.onAction(ProfileHomeAction.SetAiProvider(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    frameItem(layoutMetrics) {
        SenseeTextField(
            state = formState.aiApiKey,
            accessibilityLabel = textProvider.text(ProfileHomeTextKeys.AiApiKey),
            label = { Text(textProvider.text(ProfileHomeTextKeys.AiApiKey)) },
            secure = true,
        )
    }
    frameItem(layoutMetrics) {
        VerifyRow(
            status = uiState.effectiveAiKeyCheck(),
            textProvider = textProvider,
            onVerify = { component.onAction(ProfileHomeAction.VerifyAiKey) },
        )
    }
    aiResultItems(context)
}

private fun LazyListScope.aiResultItems(context: ProfileHomeListContext) {
    val uiState = context.uiState
    val formState = context.formState
    val textProvider = context.textProvider
    val layoutMetrics = context.layoutMetrics

    val aiCheck = uiState.effectiveAiKeyCheck()
    if (aiCheck.hasResult()) {
        frameItem(layoutMetrics) {
            KeyCheckMessage(aiCheck, textProvider)
        }
        frameItem(layoutMetrics) {
            ModelField(
                status = aiCheck,
                label = textProvider.text(ProfileHomeTextKeys.AiModel),
                options = uiState.availableAiModels,
                state = formState.aiModel,
            )
        }
    }
}

private fun LazyListScope.ttsSection(context: ProfileHomeListContext) {
    val component = context.component
    val uiState = context.uiState
    val formState = context.formState
    val textProvider = context.textProvider
    val layoutMetrics = context.layoutMetrics

    frameItem(layoutMetrics) {
        SectionHeader(title = textProvider.text(ProfileHomeTextKeys.SectionTts))
    }
    frameItem(layoutMetrics) {
        SenseeSelectField(
            label = textProvider.text(ProfileHomeTextKeys.TtsProvider),
            selected = uiState.draftSnapshot.ttsProvider,
            options = uiState.ttsProviders,
            optionLabel = { it.displayName },
            onSelect = { component.onAction(ProfileHomeAction.SetTtsProvider(it)) },
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

private fun LazyListScope.ttsResultItems(context: ProfileHomeListContext) {
    val uiState = context.uiState
    val formState = context.formState
    val textProvider = context.textProvider
    val layoutMetrics = context.layoutMetrics

    val ttsCheck = uiState.effectiveTtsKeyCheck()
    if (ttsCheck.hasResult()) {
        frameItem(layoutMetrics) {
            ModelField(
                status = ttsCheck,
                label = textProvider.text(ProfileHomeTextKeys.TtsModel),
                options = uiState.effectiveAvailableTtsModels(),
                state = formState.ttsModel,
            )
        }
        frameItem(layoutMetrics) {
            ModelField(
                status = ttsCheck,
                label = textProvider.text(ProfileHomeTextKeys.TtsVoice),
                options = uiState.effectiveAvailableTtsVoices(),
                state = formState.ttsVoiceId,
            )
        }
    }
}

internal data class ProfileHomeListContext(
    val component: ProfileHomeComponent,
    val uiState: ProfileHomeUiState,
    val formState: ProfileFormState,
    val textProvider: TextProvider,
    val layoutMetrics: SenseeAdaptiveLayoutMetrics,
)

private fun LazyListScope.ttsInheritedKeyBlock(
    component: ProfileHomeComponent,
    uiState: ProfileHomeUiState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    frameItem(layoutMetrics) {
        BodyMutedText(textProvider.text(ProfileHomeTextKeys.TtsUsesAiKey))
    }
    val aiCheck = uiState.effectiveAiKeyCheck()
    if (aiCheck.hasResult()) {
        frameItem(layoutMetrics) {
            KeyCheckMessage(aiCheck, textProvider)
        }
    }
    frameItem(layoutMetrics) {
        SenseeButton(
            onClick = { component.onAction(ProfileHomeAction.SetTtsSeparateKey(true)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(textProvider.text(ProfileHomeTextKeys.TtsUseSeparateKey))
        }
    }
}

private fun LazyListScope.ttsSeparateKeyBlock(
    component: ProfileHomeComponent,
    uiState: ProfileHomeUiState,
    formState: ProfileFormState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    frameItem(layoutMetrics) {
        SenseeTextField(
            state = formState.ttsApiKey,
            accessibilityLabel = textProvider.text(ProfileHomeTextKeys.TtsApiKey),
            label = { Text(textProvider.text(ProfileHomeTextKeys.TtsApiKey)) },
            secure = true,
        )
    }
    frameItem(layoutMetrics) {
        VerifyRow(
            status = uiState.effectiveTtsKeyCheck(),
            textProvider = textProvider,
            onVerify = { component.onAction(ProfileHomeAction.VerifyTtsKey) },
        )
    }
    val ttsCheck = uiState.effectiveTtsKeyCheck()
    if (ttsCheck.hasResult()) {
        frameItem(layoutMetrics) {
            KeyCheckMessage(ttsCheck, textProvider)
        }
    }
    val draft = uiState.draftSnapshot
    if (draft.ttsProvider == TtsProvider.OpenAi && draft.aiProvider == AiProvider.OpenAi) {
        frameItem(layoutMetrics) {
            SenseeButton(
                onClick = { component.onAction(ProfileHomeAction.SetTtsSeparateKey(false)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(textProvider.text(ProfileHomeTextKeys.TtsUseAiKey))
            }
        }
    }
}

private fun LazyListScope.frameItem(
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
    content: @Composable () -> Unit,
) = item {
    SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
        content()
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
