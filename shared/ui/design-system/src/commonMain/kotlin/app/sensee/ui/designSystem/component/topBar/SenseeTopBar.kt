package app.sensee.ui.designSystem.component.topBar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.LocalContentColor

@Composable
public fun SenseeTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: SenseeTopBarColors = SenseeTopBarDefaults.colors(),
    height: Dp = SenseeTopBarDefaults.Height,
    contentPadding: PaddingValues = SenseeTopBarDefaults.contentPadding(),
    showDivider: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.container)
                .statusBarsPadding()
                .height(height),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(SenseeTopBarDefaults.NavigationSlotWidth)
                        .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                if (navigation != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.navigationIcon,
                    ) {
                        navigation()
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.content,
                ) {
                    title()
                }
            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        SenseeTheme.spacing.extraSmall,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.actionIcon,
                    ) {
                        actions()
                    }
                },
            )
        }

        if (showDivider) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(SenseeTopBarDefaults.DividerThickness)
                        .background(colors.divider),
            )
        }
    }
}
