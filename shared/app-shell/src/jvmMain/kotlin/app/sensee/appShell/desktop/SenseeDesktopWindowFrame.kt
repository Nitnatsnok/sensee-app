package app.sensee.appShell.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.icons.Close24px
import app.sensee.ui.designSystem.icons.CollapseContent24px
import app.sensee.ui.designSystem.icons.ExpandContent24px
import app.sensee.ui.designSystem.icons.Minimize24px
import app.sensee.ui.designSystem.theme.SenseeTheme

private val TitleBarHeight = 40.dp
private val ControlButtonWidth = 46.dp
private val IconSize = 24.dp

/**
 * Desktop-only custom title bar, rendered as an [app.sensee.appShell.AppContentFrame]
 * so it sits inside the themed `AppComposeEnvironment` and reads `SenseeTheme`
 * tokens directly.
 *
 * The window stays a native (borderless) window — see the desktop app's
 * `installWindowsWindowDecoration` — so the OS keeps ownership of the window
 * shadow, rounded corners and minimise/maximise animations. This frame only
 * draws the title bar (drag area + window controls) and hosts the app content.
 */
@Composable
public fun FrameWindowScope.SenseeDesktopWindowFrame(
    windowState: WindowState,
    title: String,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val maximized = windowState.placement == WindowPlacement.Maximized
    Column(modifier = modifier.fillMaxSize().background(SenseeTheme.colors.surface)) {
        WindowTitleBar(
            title = title,
            maximized = maximized,
            onMinimize = onMinimize,
            onToggleMaximize = {
                windowState.placement =
                    if (maximized) WindowPlacement.Floating else WindowPlacement.Maximized
            },
            onClose = onClose,
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            content()
        }
    }
}

@Composable
private fun FrameWindowScope.WindowTitleBar(
    title: String,
    maximized: Boolean,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = SenseeTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(TitleBarHeight)
                .background(colors.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Only the title region drags the window; the control buttons sit
        // outside the drag area so a click never starts a move gesture.
        WindowDraggableArea(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = SenseeTheme.typography.labelLarge.copy(color = colors.textSecondary),
                )
            }
        }
        WindowControlButton(
            icon = Minimize24px,
            accessibilityLabel = "Minimize window",
            hoverContainer = colors.surfaceContainerHighest,
            hoverContent = colors.textPrimary,
            idleContent = colors.textSecondary,
            onClick = onMinimize,
        )
        WindowControlButton(
            icon = if (maximized) CollapseContent24px else ExpandContent24px,
            accessibilityLabel = if (maximized) "Restore window" else "Maximize window",
            hoverContainer = colors.surfaceContainerHighest,
            hoverContent = colors.textPrimary,
            idleContent = colors.textSecondary,
            onClick = onToggleMaximize,
        )
        WindowControlButton(
            icon = Close24px,
            accessibilityLabel = "Close window",
            hoverContainer = colors.dangerContainer,
            hoverContent = colors.textOnDangerContainer,
            idleContent = colors.textSecondary,
            onClick = onClose,
        )
    }
}

@Composable
private fun WindowControlButton(
    icon: ImageVector,
    accessibilityLabel: String,
    hoverContainer: Color,
    hoverContent: Color,
    idleContent: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val container = if (hovered) hoverContainer else Color.Transparent
    val content = if (hovered) hoverContent else idleContent
    Box(
        modifier =
            Modifier
                .size(width = ControlButtonWidth, height = TitleBarHeight)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .semantics { contentDescription = accessibilityLabel }
                .background(container),
        contentAlignment = Alignment.Center,
    ) {
        SenseeIcon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
            tint = content,
        )
    }
}
