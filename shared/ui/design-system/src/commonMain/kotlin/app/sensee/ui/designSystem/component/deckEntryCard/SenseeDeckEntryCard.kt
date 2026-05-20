package app.sensee.ui.designSystem.component.deckEntryCard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.LocalContentColor

@Composable
public fun SenseeDeckEntryCard(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    leading: (@Composable BoxScope.() -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    meta: (@Composable RowScope.() -> Unit)? = null,
    progress: Float? = null,
    trailing: (@Composable BoxScope.() -> Unit)? = null,
    colors: SenseeDeckEntryCardColors = SenseeDeckEntryCardDefaults.colors(),
    shape: Shape = SenseeDeckEntryCardDefaults.Shape,
    contentPadding: PaddingValues = SenseeDeckEntryCardDefaults.contentPadding(),
    minHeight: Dp = SenseeDeckEntryCardDefaults.MinHeight,
    borderWidth: Dp = SenseeDeckEntryCardDefaults.BorderWidth,
) {
    val spacing = SenseeTheme.spacing

    val clickableModifier =
        if (onClick != null) {
            Modifier.clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
        } else {
            Modifier
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .clip(shape)
                .background(colors.containerColor(enabled))
                .border(
                    width = borderWidth,
                    color = colors.border,
                    shape = shape,
                ).then(clickableModifier)
                .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        if (leading != null) {
            Box(
                modifier = Modifier.size(SenseeDeckEntryCardDefaults.LeadingSize),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.contentColor(enabled),
                ) {
                    leading()
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.titleColor(enabled),
            ) {
                title()
            }

            if (subtitle != null) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.subtitleColor(enabled),
                ) {
                    subtitle()
                }
            }

            if (meta != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.metaColor(enabled),
                    ) {
                        meta()
                    }
                }
            }

            if (progress != null) {
                Spacer(modifier = Modifier.height(spacing.extraSmall))

                SenseeDeckEntryProgressBar(
                    progress = progress,
                    trackColor = colors.progressTrack,
                    indicatorColor = colors.progressIndicator,
                )
            }
        }

        if (trailing != null) {
            Box(
                modifier = Modifier.size(SenseeDeckEntryCardDefaults.TrailingSize),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.contentColor(enabled),
                ) {
                    trailing()
                }
            }
        }
    }
}
