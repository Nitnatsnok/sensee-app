package app.sensee.feature.practice.presentation.impl.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.sensee.core.presentation.text.TextProvider
import app.sensee.feature.practice.presentation.api.CardDetailComponent
import app.sensee.feature.practice.presentation.impl.detail.CardDetailScreen
import app.sensee.feature.practice.presentation.impl.text.PracticeTextKeys
import app.sensee.feature.practice.presentation.impl.text.rememberPracticeTextProvider
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.button.SenseeIconButton
import app.sensee.ui.designSystem.component.layout.SenseeModalBottomSheet
import app.sensee.ui.designSystem.component.layout.SenseeSheetHeader
import app.sensee.ui.designSystem.component.topBar.SenseeTopBar
import app.sensee.ui.designSystem.component.topBar.SenseeTopBarIconButton
import app.sensee.ui.designSystem.icons.Close24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

@Composable
internal fun DeckPracticeDetailPane(
    component: CardDetailComponent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberPracticeTextProvider(),
) {
    val colors = SenseeTheme.colors
    Column(
        modifier =
            modifier
                .background(colors.background),
    ) {
        SenseeTopBar(
            title = {
                Text(text = textProvider.text(PracticeTextKeys.CardDetailSheetTitle))
            },
            actions = {
                SenseeTopBarIconButton(
                    onClick = onDismiss,
                    accessibilityLabel = textProvider.text(PracticeTextKeys.ActionClose),
                    icon = { SenseeIcon(imageVector = Close24px, contentDescription = null) },
                )
            },
            showDivider = false,
        )
        CardDetailScreen(component = component, modifier = Modifier.fillMaxSize(), textProvider = textProvider)
    }
}

@Composable
internal fun DeckPracticeDetailSheet(
    component: CardDetailComponent?,
    visible: Boolean,
    onDismiss: () -> Unit,
    textProvider: TextProvider = rememberPracticeTextProvider(),
) {
    // Keep the last non-null component around so the exit animation has content to render
    // after `panels.details` is cleared by decompose. Updated synchronously in composition
    // (a feed-forward cache of the latest input) rather than through a LaunchedEffect, which
    // would add a recomposition pass and a frame where renderTarget is briefly stale.
    var lastComponent by remember { mutableStateOf<CardDetailComponent?>(null) }
    if (component != null && component != lastComponent) {
        lastComponent = component
    }

    SenseeModalBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        contentPadding = PaddingValues(0.dp),
    ) {
        val spacing = SenseeTheme.spacing

        SenseeSheetHeader(
            modifier = Modifier.padding(horizontal = spacing.large),
            title = {
                Text(
                    text = textProvider.text(PracticeTextKeys.CardDetailSheetTitle),
                    style = SenseeTheme.typography.titleMedium,
                )
            },
            actions = {
                SenseeIconButton(
                    onClick = onDismiss,
                    accessibilityLabel = textProvider.text(PracticeTextKeys.ActionClose),
                    icon = { SenseeIcon(imageVector = Close24px, contentDescription = null) },
                )
            },
        )
        Spacer(modifier = Modifier.height(spacing.small))
        val renderTarget = component ?: lastComponent
        if (renderTarget != null) {
            CardDetailScreen(
                component = renderTarget,
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                textProvider = textProvider,
            )
        }
    }
}
