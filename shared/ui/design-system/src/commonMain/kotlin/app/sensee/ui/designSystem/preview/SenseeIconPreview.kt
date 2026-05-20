package app.sensee.ui.designSystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.icons.Add24px
import app.sensee.ui.designSystem.icons.AddCircle24px
import app.sensee.ui.designSystem.icons.ArrowBack24px
import app.sensee.ui.designSystem.icons.ArrowDownwardAlt24px
import app.sensee.ui.designSystem.icons.ArrowLeftAlt24px
import app.sensee.ui.designSystem.icons.ArrowRightAlt24px
import app.sensee.ui.designSystem.icons.ArrowUpwardAlt24px
import app.sensee.ui.designSystem.icons.Check
import app.sensee.ui.designSystem.icons.CheckBox
import app.sensee.ui.designSystem.icons.CheckBoxOutlineBlank
import app.sensee.ui.designSystem.icons.CheckCircle24px
import app.sensee.ui.designSystem.icons.Close24px
import app.sensee.ui.designSystem.icons.Delete24px
import app.sensee.ui.designSystem.icons.Flip24px
import app.sensee.ui.designSystem.icons.Help24px
import app.sensee.ui.designSystem.icons.Home24px
import app.sensee.ui.designSystem.icons.Info24px
import app.sensee.ui.designSystem.icons.Language24px
import app.sensee.ui.designSystem.icons.LibraryBooks24px
import app.sensee.ui.designSystem.icons.Person24px
import app.sensee.ui.designSystem.icons.Psychology24px
import app.sensee.ui.designSystem.icons.Search24px
import app.sensee.ui.designSystem.icons.SentimentDissatisfied24px
import app.sensee.ui.designSystem.icons.SentimentSatisfied24px
import app.sensee.ui.designSystem.icons.SentimentVeryDissatisfied24px
import app.sensee.ui.designSystem.icons.SentimentVerySatisfied24px
import app.sensee.ui.designSystem.icons.Settings24px
import app.sensee.ui.designSystem.icons.SoundSampler24px
import app.sensee.ui.designSystem.icons.Star24px
import app.sensee.ui.designSystem.icons.StarFilled24px
import app.sensee.ui.designSystem.icons.SwipeDown24px
import app.sensee.ui.designSystem.icons.SwipeLeft24px
import app.sensee.ui.designSystem.icons.SwipeRight24px
import app.sensee.ui.designSystem.icons.SwipeUp24px
import app.sensee.ui.designSystem.icons.Undo24px
import app.sensee.ui.designSystem.icons.WandStars24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import app.sensee.ui.designSystem.theme.SenseeThemeMode
import com.composeunstyled.Text

@Preview(widthDp = 360)
@Composable
private fun SenseeIconCatalogLightPreview() =
    SenseePreview {
        SenseeIconCatalog()
    }

@Preview(widthDp = 360)
@Composable
private fun SenseeIconCatalogDarkPreview() =
    SenseePreview(themeMode = SenseeThemeMode.Dark) {
        SenseeIconCatalog()
    }

@Composable
private fun SenseeIconCatalog() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.medium),
    ) {
        SenseeIconCatalogEntries.forEach { (name, icon) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SenseeTheme.spacing.extraSmall),
            ) {
                SenseeIcon(imageVector = icon, contentDescription = name)
                Text(text = name, style = SenseeTheme.typography.labelSmall)
            }
        }
    }
}

private val SenseeIconCatalogEntries: List<Pair<String, ImageVector>> =
    listOf(
        "Add" to Add24px,
        "AddCircle" to AddCircle24px,
        "ArrowBack" to ArrowBack24px,
        "ArrowDownward" to ArrowDownwardAlt24px,
        "ArrowLeft" to ArrowLeftAlt24px,
        "ArrowRight" to ArrowRightAlt24px,
        "ArrowUpward" to ArrowUpwardAlt24px,
        "Check" to Check,
        "CheckBox" to CheckBox,
        "CheckBoxBlank" to CheckBoxOutlineBlank,
        "CheckCircle" to CheckCircle24px,
        "Close" to Close24px,
        "Delete" to Delete24px,
        "Flip" to Flip24px,
        "Help" to Help24px,
        "Home" to Home24px,
        "Info" to Info24px,
        "Language" to Language24px,
        "LibraryBooks" to LibraryBooks24px,
        "Person" to Person24px,
        "Psychology" to Psychology24px,
        "Search" to Search24px,
        "Sad" to SentimentDissatisfied24px,
        "Happy" to SentimentSatisfied24px,
        "VerySad" to SentimentVeryDissatisfied24px,
        "VeryHappy" to SentimentVerySatisfied24px,
        "Settings" to Settings24px,
        "SoundSampler" to SoundSampler24px,
        "Star" to Star24px,
        "StarFilled" to StarFilled24px,
        "SwipeDown" to SwipeDown24px,
        "SwipeLeft" to SwipeLeft24px,
        "SwipeRight" to SwipeRight24px,
        "SwipeUp" to SwipeUp24px,
        "Undo" to Undo24px,
        "WandStars" to WandStars24px,
    )
