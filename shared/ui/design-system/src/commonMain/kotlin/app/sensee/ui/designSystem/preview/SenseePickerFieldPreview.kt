package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.pickerField.SenseePickerField
import app.sensee.ui.designSystem.icons.Tag
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.SenseeThemeMode

@Preview(widthDp = 360)
@Composable
private fun SenseePickerFieldLightPreview() =
    SenseePreview {
        SenseePickerFieldSamples()
    }

@Preview(widthDp = 360)
@Composable
private fun SenseePickerFieldDarkPreview() =
    SenseePreview(themeMode = SenseeThemeMode.Dark) {
        SenseePickerFieldSamples()
    }

@Composable
private fun SenseePickerFieldSamples() {
    Column(verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.medium)) {
        SenseePickerField(
            title = "Темы для примеров",
            value = "Путешествия, Еда, ещё 3",
            onClick = {},
            hint = "AI-ассистент подбирает примеры по выбранным темам там, где это естественно для значения слова.",
            leadingIcon = Tag,
        )
        SenseePickerField(
            title = "Темы для примеров",
            value = "Не выбрано",
            onClick = {},
            leadingIcon = Tag,
        )
        SenseePickerField(
            title = "Без иконки",
            value = "Какое-то значение",
            onClick = {},
        )
    }
}
