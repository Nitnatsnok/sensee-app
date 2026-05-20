package app.sensee.ui.designSystem.component.topBar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.LocalContentColor

@Composable
public fun SenseeImmersiveTopBar(
    navigation: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    colors: SenseeTopBarColors = SenseeTopBarDefaults.immersiveColors(),
    height: Dp = SenseeTopBarDefaults.FullscreenHeight,
    contentPadding: PaddingValues = SenseeTopBarDefaults.contentPadding(),
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.container)
                .statusBarsPadding()
                .height(height)
                .padding(contentPadding),
    ) {
        Box(
            modifier = Modifier.align(Alignment.CenterStart),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.navigationIcon,
            ) {
                navigation()
            }
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement =
                Arrangement.spacedBy(
                    SenseeTheme.spacing.extraSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.actionIcon,
            ) {
                actions()
            }
        }
    }
}
