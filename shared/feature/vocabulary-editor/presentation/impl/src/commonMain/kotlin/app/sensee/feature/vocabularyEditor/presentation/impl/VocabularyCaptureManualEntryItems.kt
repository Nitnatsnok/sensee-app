package app.sensee.feature.vocabularyEditor.presentation.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureAction
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureComponent
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyCaptureUiState
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.button.SenseeButtonColors
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import app.sensee.ui.designSystem.component.selectField.SenseeSelectField
import app.sensee.ui.designSystem.component.textField.SenseeTextField
import app.sensee.ui.designSystem.theme.SenseeAdaptiveLayoutMetrics
import com.composeunstyled.Text
import kotlinx.collections.immutable.toPersistentList

private val ManualUnitTypeOptions = GrammarUnitType.entries.toPersistentList()

internal fun LazyListScope.manualTranslationItem(
    formState: VocabularyCaptureFormState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SenseeTextField(
                state = formState.manualTranslation,
                accessibilityLabel = textProvider.text(VocabularyCaptureTextKeys.ManualMeaningLabel),
                label = { Text(textProvider.text(VocabularyCaptureTextKeys.ManualMeaningLabel)) },
                placeholder = {
                    Text(textProvider.text(VocabularyCaptureTextKeys.ManualMeaningPlaceholder))
                },
            )
        }
    }
}

internal fun LazyListScope.manualSurfaceFormItem(
    formState: VocabularyCaptureFormState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SenseeTextField(
                state = formState.manualSurfaceForm,
                accessibilityLabel = textProvider.text(VocabularyCaptureTextKeys.SurfaceFormLabel),
                label = { Text(textProvider.text(VocabularyCaptureTextKeys.SurfaceFormLabel)) },
                placeholder = {
                    Text(textProvider.text(VocabularyCaptureTextKeys.SurfaceFormPlaceholder))
                },
            )
        }
    }
}

internal fun LazyListScope.manualUnitTypeItem(
    uiState: VocabularyCaptureUiState,
    formState: VocabularyCaptureFormState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SenseeSelectField(
                label = textProvider.text(VocabularyCaptureTextKeys.PartOfSpeechLabel),
                selected = formState.manualUnitType,
                options = ManualUnitTypeOptions,
                optionLabel = { uiState.grammarLabels.unitType(it) ?: it.name },
                onSelect = { formState.manualUnitType = it },
                placeholder = textProvider.text(VocabularyCaptureTextKeys.PartOfSpeechUnset),
            )
        }
    }
}

internal fun LazyListScope.manualAddButtonItem(
    component: VocabularyCaptureComponent,
    formState: VocabularyCaptureFormState,
    textProvider: TextProvider,
    layoutMetrics: SenseeAdaptiveLayoutMetrics,
) {
    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SenseeButton(
                onClick = {
                    component.onAction(
                        VocabularyCaptureAction.AddManual(
                            translation = formState.manualTranslation.text.toString(),
                            surfaceForm = formState.manualSurfaceForm.text.toString(),
                            unitType = formState.manualUnitType,
                        ),
                    )
                    formState.resetManual()
                },
                colors = SenseeButtonColors.tonal(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(textProvider.text(VocabularyCaptureTextKeys.AddOwnVariant))
            }
        }
    }
}
