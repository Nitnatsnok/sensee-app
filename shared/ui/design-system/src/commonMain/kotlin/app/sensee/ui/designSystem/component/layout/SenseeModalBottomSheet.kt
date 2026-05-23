@file:Suppress("MatchingDeclarationName")

package app.sensee.ui.designSystem.component.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.sensee.ui.designSystem.theme.SenseeShapes
import app.sensee.ui.designSystem.theme.SenseeSpacing
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.composeunstyled.DragIndication
import com.composeunstyled.ModalBottomSheetScope
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.Scrim
import com.composeunstyled.Sheet
import com.composeunstyled.SheetDetent
import com.composeunstyled.UnstyledModalBottomSheet
import com.composeunstyled.rememberModalBottomSheetState

public object SenseeModalBottomSheetDefaults {
    // Caps the sheet at a comfortable reading width on wide screens. Distinct from screen
    // content-max-width layout tokens because bottom sheets size to their own constraints,
    // not the surrounding content area.
    public val MaxWidth: Dp = 640.dp
    public val DragIndicatorWidth: Dp = 40.dp
    public val DragIndicatorHeight: Dp = 4.dp

    // Resting fraction of the container the sheet occupies when it first opens.
    // The user can drag up to `SheetDetent.FullyExpanded` (full container) or
    // down to dismiss. Mirrors M3 bottom-sheet "half-expanded" semantics.
    public const val PARTIAL_HEIGHT_FRACTION: Float = 0.5f

    @Composable
    public fun contentPadding(): PaddingValues {
        val spacing = SenseeTheme.spacing
        return PaddingValues(
            start = spacing.large,
            end = spacing.large,
            bottom = spacing.large,
        )
    }
}

// Half-container detent — always 50% regardless of the content size. The
// surface inside fills this allocated height (see fillMaxHeight below) so the
// scrim never bleeds through under the content. Content longer than the
// detent scrolls inside; drag-up goes to FullyExpanded.
private val PARTIAL_DETENT =
    SheetDetent(identifier = "partial") { containerHeight, _ ->
        containerHeight * SenseeModalBottomSheetDefaults.PARTIAL_HEIGHT_FRACTION
    }

/**
 * Modal bottom sheet. Mounts the sheet in its own OS-level window (`Dialog` on JVM/iOS,
 * `ComponentDialog` on Android via composeunstyled's `UnstyledModalBottomSheet` → `Modal`),
 * so the sheet overlays the host content correctly — including the status bar / navigation
 * insets — and can be dismissed independently of the host's composition tree.
 *
 * Caller-controlled `visible` is pushed into the sheet detent state. When the user dismisses
 * by drag or tap-outside, the sheet animates itself to Hidden internally; the redundant
 * `animateTo(Hidden)` from the visibility effect is suppressed to avoid scrim flicker.
 *
 * The sheet opens at half-container height ([SenseeModalBottomSheetDefaults.PARTIAL_HEIGHT_FRACTION])
 * and can be dragged up to fully expanded. The surface fills the allocated detent height —
 * content shorter than the detent leaves the rest as surface color (M3 behavior); content
 * taller than the current detent scrolls inside its own scroll container (the caller is
 * expected to use `LazyColumn` or `Modifier.verticalScroll`).
 */
@Composable
public fun SenseeModalBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = SenseeModalBottomSheetDefaults.MaxWidth,
    contentPadding: PaddingValues = SenseeModalBottomSheetDefaults.contentPadding(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SenseeTheme.colors
    // Top corners follow the theme's extra-large radius; bottom corners are
    // squared because the sheet is docked to the bottom edge.
    val sheetShape =
        SenseeTheme.shapes.extraLarge.copy(
            bottomStart = ZeroCornerSize,
            bottomEnd = ZeroCornerSize,
        )
    // Always start Hidden so an initially-visible sheet still animates in: the
    // LaunchedEffect(visible) below drives Hidden -> Partial on first composition.
    val sheetState =
        rememberModalBottomSheetState(
            initialDetent = SheetDetent.Hidden,
            detents = listOf(SheetDetent.Hidden, PARTIAL_DETENT, SheetDetent.FullyExpanded),
        )

    // Mirror external `visible` into the sheet state; user-driven dismissals
    // already animate the sheet to Hidden internally, so the `currentDetent`
    // check keeps the scrim animation single-source.
    LaunchedEffect(visible) {
        val target = if (visible) PARTIAL_DETENT else SheetDetent.Hidden
        if (sheetState.currentDetent != target) {
            sheetState.animateTo(target)
        }
    }

    UnstyledModalBottomSheet(
        state = sheetState,
        onDismiss = onDismissRequest,
        overlay = {
            Scrim(scrimColor = colors.scrim)
        },
    ) {
        // `Sheet` places its child at x=0; the full-width box + BottomCenter alignment
        // centers the width-capped surface on tablet/desktop and lets phones (< max)
        // go edge-to-edge (M3 bottom-sheet behavior).
        Sheet(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .imePadding(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier =
                        Modifier
                            .statusBarsPadding()
                            // Cap BEFORE fill: `widthIn(max).fillMaxWidth()` sizes the
                            // node to min(available, max) so BottomCenter can center it.
                            // The reverse order makes the node span the full width and
                            // only caps the content, killing centering.
                            .widthIn(max = maxWidth)
                            .fillMaxWidth()
                            // Fill the detent's allocated height so the surface
                            // is the full sheet area on every detent — without
                            // this the surface wraps inner content height and a
                            // `LazyColumn` (whose `maxIntrinsicHeight` is 0)
                            // collapses the surface, exposing the scrim below
                            // the inner content while the anchor still sits at
                            // the partial-detent offset.
                            .fillMaxHeight()
                            .clip(sheetShape)
                            .background(colors.surfaceContainerHigh),
                ) {
                    ProvideContentColor(colors.textPrimary) {
                        SenseeModalBottomSheetContent(
                            dragIndicatorColor = colors.border,
                            spacing = SenseeTheme.spacing,
                            shapes = SenseeTheme.shapes,
                            contentPadding = contentPadding,
                            content = content,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModalBottomSheetScope.SenseeModalBottomSheetContent(
    dragIndicatorColor: Color,
    spacing: SenseeSpacing,
    shapes: SenseeShapes,
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DragIndication(
            modifier =
                Modifier
                    .padding(top = spacing.large, bottom = spacing.large)
                    .width(SenseeModalBottomSheetDefaults.DragIndicatorWidth)
                    .height(SenseeModalBottomSheetDefaults.DragIndicatorHeight)
                    .background(
                        color = dragIndicatorColor,
                        shape = shapes.circle,
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content,
            )
        }
    }
}
