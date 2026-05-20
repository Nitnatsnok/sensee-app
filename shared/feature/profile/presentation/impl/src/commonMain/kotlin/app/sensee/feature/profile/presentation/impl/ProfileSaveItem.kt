package app.sensee.feature.profile.presentation.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import app.sensee.feature.profile.presentation.api.ProfileHomeAction
import app.sensee.ui.designSystem.component.button.SenseeButton
import app.sensee.ui.designSystem.component.layout.SenseeScreenContentFrame
import com.composeunstyled.Text

internal fun LazyListScope.saveItem(
    context: ProfileHomeListContext,
    showSaved: Boolean,
) {
    val component = context.component
    val textProvider = context.textProvider
    val layoutMetrics = context.layoutMetrics

    item {
        SenseeScreenContentFrame(layoutMetrics = layoutMetrics) {
            SenseeButton(
                onClick = { component.onAction(ProfileHomeAction.Save) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    textProvider.text(
                        if (showSaved) ProfileHomeTextKeys.Saved else ProfileHomeTextKeys.Save,
                    ),
                )
            }
        }
    }
}
