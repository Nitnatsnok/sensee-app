package app.sensee.ui.designSystem.component.senseCard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.component.SenseeIcon
import app.sensee.ui.designSystem.component.layout.SenseeSurface
import app.sensee.ui.designSystem.component.layout.SenseeSurfaceDefaults
import app.sensee.ui.designSystem.component.sentence.SenseeSentence
import app.sensee.ui.designSystem.component.sentence.SenseeSentenceText
import app.sensee.ui.designSystem.icons.ArrowDownwardAlt24px
import app.sensee.ui.designSystem.icons.Check
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.Text

/**
 * The card shell for a single lexical sense — the visual primitive shared by capture's
 * selectable candidate list and Library's read-only deck browsing. It is deliberately
 * domain-neutral: callers map their `Sense`/grammar models into these slots and strings,
 * so this stays a design-system primitive with no lexicon or grammar dependency.
 *
 * The primary tier ([title], [translation], [explanation], one [example]) carries what
 * disambiguates a sense. Optional [secondaryDetail] sits behind an edge-to-edge expand
 * handle so richer grammar does not bury the primary read; [footer] attaches extra
 * controls below it.
 *
 * Selection: pass [onClick] to make the whole card a toggle and [selected] to show the
 * accent border plus a corner checkmark. Leave [onClick] null for a static, read-only
 * card (no checkmark slot, no border, no click).
 */
@Composable
public fun SenseeSenseCard(
    title: @Composable () -> Unit,
    translation: String,
    modifier: Modifier = Modifier,
    explanation: String? = null,
    example: SenseeSentence? = null,
    badge: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    expandShowLabel: String? = null,
    expandHideLabel: String? = null,
    initiallyExpanded: Boolean = false,
    expandResetKey: Any? = null,
    secondaryDetail: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = SenseeTheme.colors
    val typography = SenseeTheme.typography
    val spacing = SenseeTheme.spacing

    // Re-seed the expand state when the card's content identity changes ([expandResetKey]):
    // a caller that swaps the rendered sense in place (a positional list without item keys)
    // gets a collapsed card again instead of a stale "expanded" carried over from the prior one.
    var expanded by remember(expandResetKey) { mutableStateOf(initiallyExpanded) }

    val surfaceColors =
        if (selected) {
            SenseeSurfaceDefaults.colors(border = colors.accent)
        } else {
            SenseeSurfaceDefaults.colors()
        }

    val hasTrailingBlock = secondaryDetail != null || footer != null

    SenseeSurface(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.semantics { this.selected = selected } else Modifier),
        onClick = onClick,
        colors = surfaceColors,
        // The expand handle is edge-to-edge, so the surface drops its own padding and each
        // section controls its inset.
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Column(
                modifier =
                    Modifier.padding(
                        start = spacing.large,
                        top = spacing.large,
                        end = spacing.large,
                        bottom = if (hasTrailingBlock) 0.dp else spacing.large,
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                    SenseTitleRow(
                        title = title,
                        badge = badge,
                        selected = selected,
                        selectable = onClick != null,
                    )
                    Text(
                        text = translation,
                        color = colors.textPrimary,
                        style = typography.titleSmall,
                    )
                    explanation?.let {
                        Text(text = it, color = colors.textSecondary, style = typography.bodyMedium)
                    }
                }
                example?.let {
                    SenseeSentenceText(
                        sentence = it,
                        textStyle = typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (secondaryDetail != null) {
                SenseExpandHandle(
                    expanded = expanded,
                    onToggle = { expanded = !expanded },
                    label = if (expanded) expandHideLabel else expandShowLabel,
                )
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(trailingInset(spacing.large))) {
                        secondaryDetail()
                    }
                }
            }
            footer?.let { content ->
                Column(modifier = Modifier.padding(trailingInset(spacing.large))) {
                    content()
                }
            }
        }
    }
}

private fun trailingInset(side: Dp): PaddingValues = PaddingValues(start = side, end = side, bottom = side)

@Composable
private fun SenseTitleRow(
    title: @Composable () -> Unit,
    badge: (@Composable () -> Unit)?,
    selected: Boolean,
    selectable: Boolean,
) {
    val colors = SenseeTheme.colors
    val spacing = SenseeTheme.spacing

    // The title takes the remaining width; the badge and a fixed-size checkmark slot sit at
    // the trailing edge so toggling selection never shifts the title layout.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.weight(1f)) { title() }

        badge?.invoke()

        if (selectable) {
            if (selected) {
                SenseeIcon(
                    imageVector = Check,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(CornerCheckSize),
                )
            } else {
                Spacer(Modifier.size(CornerCheckSize))
            }
        }
    }
}

@Composable
private fun SenseExpandHandle(
    expanded: Boolean,
    onToggle: () -> Unit,
    label: String?,
) {
    val colors = SenseeTheme.colors
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle, onClickLabel = label),
    ) {
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(HandleHairlineThickness)
                    .background(colors.textMuted.copy(alpha = HANDLE_HAIRLINE_ALPHA)),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = HandleTapHeight),
            contentAlignment = Alignment.Center,
        ) {
            SenseeIcon(
                imageVector = ArrowDownwardAlt24px,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

private val CornerCheckSize = 20.dp
private val HandleTapHeight = 40.dp
private val HandleHairlineThickness = 1.dp
private const val HANDLE_HAIRLINE_ALPHA = 0.2f
