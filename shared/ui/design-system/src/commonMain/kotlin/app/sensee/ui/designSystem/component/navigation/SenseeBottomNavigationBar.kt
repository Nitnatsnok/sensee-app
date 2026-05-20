package app.sensee.ui.designSystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import app.sensee.ui.designSystem.theme.SenseeTheme

@Composable
public fun SenseeBottomNavigationBar(
    modifier: Modifier = Modifier,
    colors: SenseeNavigationContainerColors =
        SenseeNavigationDefaults.containerColors(),
    content: @Composable RowScope.() -> Unit,
) {
    val layout = SenseeTheme.layout
    val spacing = SenseeTheme.spacing

    Box(
        modifier =
            modifier
                .background(colors.container)
                .navigationBarsPadding()
                .height(layout.bottomNavigationHeight)
                .selectableGroup()
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color = colors.divider,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = SenseeNavigationDefaults.DividerThickness.toPx(),
                    )
                }.padding(horizontal = spacing.large),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(
                LocalSenseeNavigationItemLayout provides SenseeNavigationItemLayout.BottomBar,
            ) {
                content()
            }
        }
    }
}
