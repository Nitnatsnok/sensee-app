package app.sensee.ui.designSystem.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

/**
 * Centered, full-area status block — the generic primitive that powers the named wrappers
 * [SenseeLoadingState], [SenseeErrorState] and [SenseeEmptyState].
 *
 * Slot layout (top → bottom, all centered):
 *
 * ```
 *   visual           ← optional, room for a Lottie / icon / illustration
 *   title            ← short headline (titleMedium / textPrimary)
 *   description      ← longer line (bodyMedium / textMuted)
 *   actions          ← horizontal row of buttons (retry, primary CTA, …)
 * ```
 *
 * Every slot is optional. Pass `visual = { LottieAnimation(…) }` to attach an animation
 * without losing the structural padding / centering. The block always fills the available
 * space and centers vertically, so it works both inside a parent column with `weight(1f)`
 * and as a screen-level container.
 */
@Composable
public fun SenseeStatusBlock(
    modifier: Modifier = Modifier,
    visual: (@Composable () -> Unit)? = null,
    title: String? = null,
    description: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = SenseeStatusBlockDefaults.contentPadding(),
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (visual != null) {
            visual()
            if (title != null || description != null || actions != null) {
                Spacer(modifier = Modifier.height(spacing.large))
            }
        }
        if (title != null) {
            Text(
                text = title,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                style = typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (description != null) {
            if (title != null) {
                Spacer(modifier = Modifier.height(spacing.small))
            }
            Text(
                text = description,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                style = typography.bodyMedium,
            )
        }
        if (actions != null) {
            Spacer(modifier = Modifier.height(spacing.large))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                actions()
            }
        }
    }
}

/** Loading variant of [SenseeStatusBlock]. */
@Composable
public fun SenseeLoadingState(
    modifier: Modifier = Modifier,
    title: String? = null,
    visual: (@Composable () -> Unit)? = null,
) {
    SenseeStatusBlock(
        modifier = modifier,
        visual = visual,
        title = title,
    )
}

/**
 * Empty-data variant. Same structure as [SenseeStatusBlock] but named to make intent explicit
 * at call sites — "no decks yet", "deck finished", etc. Pass an [actions] slot for a primary
 * CTA like "Create a deck".
 */
@Composable
public fun SenseeEmptyState(
    modifier: Modifier = Modifier,
    visual: (@Composable () -> Unit)? = null,
    title: String? = null,
    description: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    SenseeStatusBlock(
        modifier = modifier,
        visual = visual,
        title = title,
        description = description,
        actions = actions,
    )
}

/**
 * Error variant. Identical layout to [SenseeEmptyState], named separately so a
 * screen's error path reads explicitly at call sites.
 */
@Composable
public fun SenseeErrorState(
    modifier: Modifier = Modifier,
    visual: (@Composable () -> Unit)? = null,
    title: String? = null,
    description: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    SenseeStatusBlock(
        modifier = modifier,
        visual = visual,
        title = title,
        description = description,
        actions = actions,
    )
}

public object SenseeStatusBlockDefaults {
    @Composable
    public fun contentPadding(): PaddingValues {
        val spacing = SenseeTheme.spacing
        return PaddingValues(horizontal = spacing.large, vertical = spacing.extraLarge)
    }
}
