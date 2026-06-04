package app.sensee.appShell.primary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import app.sensee.appShell.LocalPlatform
import app.sensee.core.compose.text.LocalTextProvider
import app.sensee.core.compose.thenIf
import app.sensee.core.presentation.text.TextKey
import app.sensee.core.presentation.text.TextProvider
import app.sensee.core.presentation.text.withFallback
import app.sensee.feature.home.presentation.api.HomeSectionComponent
import app.sensee.feature.home.presentation.impl.HomeSectionScreen
import app.sensee.feature.library.presentation.api.LibrarySectionComponent
import app.sensee.feature.library.presentation.impl.LibrarySectionScreen
import app.sensee.feature.practice.presentation.api.PracticeSectionComponent
import app.sensee.feature.practice.presentation.impl.section.PracticeSectionScreen
import app.sensee.feature.profile.presentation.api.ProfileSectionComponent
import app.sensee.feature.profile.presentation.impl.section.ProfileSectionScreen
import app.sensee.feature.vocabularyEditor.presentation.api.VocabularyEditorSectionComponent
import app.sensee.feature.vocabularyEditor.presentation.impl.screen.VocabularyEditorSectionScreen
import app.sensee.ui.adaptive.LocalAdaptiveInfo
import app.sensee.ui.designSystem.component.navigation.SenseeBottomNavigationBar
import app.sensee.ui.designSystem.component.navigation.SenseeNavigationActionButton
import app.sensee.ui.designSystem.component.navigation.SenseeNavigationItem
import app.sensee.ui.designSystem.component.navigation.SenseeNavigationRail
import app.sensee.ui.designSystem.component.navigation.SenseeTopNavigationBar
import app.sensee.ui.designSystem.icons.Add24px
import app.sensee.ui.designSystem.icons.Home24px
import app.sensee.ui.designSystem.icons.LibraryBooks24px
import app.sensee.ui.designSystem.icons.Person24px
import app.sensee.ui.designSystem.icons.Psychology24px
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation

/**
 * Ordered list of primary navigation entries. Shared by the rail, bottom-bar, and top-bar
 * layouts so each entry is declared exactly once. [PrimarySection.VocabularyEditor] is the
 * Add action — its position in this list ([PRIMARY_ACTION_INDEX]) is used differently by
 * each layout: the bottom bar splices the action button inline between the two halves of
 * the items, while the rail and top bar render it in their dedicated action slot (FAB at
 * the rail's bottom; trailing edge of the top bar) and skip the index during iteration.
 */
private data class PrimaryNavDestination(
    val section: PrimarySection,
    val icon: ImageVector,
    val labelKey: TextKey,
)

private val PrimaryNavDestinations: List<PrimaryNavDestination> =
    listOf(
        PrimaryNavDestination(
            section = PrimarySection.Home,
            icon = Home24px,
            labelKey = AppShellTextKeys.PrimaryNavigationHome,
        ),
        PrimaryNavDestination(
            section = PrimarySection.Practice,
            icon = Psychology24px,
            labelKey = AppShellTextKeys.PrimaryNavigationPractice,
        ),
        PrimaryNavDestination(
            section = PrimarySection.VocabularyEditor,
            icon = Add24px,
            labelKey = AppShellTextKeys.PrimaryNavigationAddEntry,
        ),
        PrimaryNavDestination(
            section = PrimarySection.Library,
            icon = LibraryBooks24px,
            labelKey = AppShellTextKeys.PrimaryNavigationLibrary,
        ),
        PrimaryNavDestination(
            section = PrimarySection.Profile,
            icon = Person24px,
            labelKey = AppShellTextKeys.PrimaryNavigationProfile,
        ),
    )

/**
 * Position of the Add action in [PrimaryNavDestinations]. Derived from the entry whose section
 * is [PrimarySection.VocabularyEditor], so reordering the list (or moving VocabularyEditor)
 * keeps the splice/skip position correct without manual bookkeeping.
 */
private val PRIMARY_ACTION_INDEX: Int =
    PrimaryNavDestinations.indexOfFirst { it.section == PrimarySection.VocabularyEditor }

@Composable
public fun PrimaryShellScreen(
    component: PrimaryShellComponent,
    modifier: Modifier = Modifier,
    textProvider: TextProvider = rememberPrimaryShellTextProvider(),
) {
    val adaptiveInfo = LocalAdaptiveInfo.current
    val platform = LocalPlatform.current
    val selectedSection by component.selectedSection.collectAsState()

    CompositionLocalProvider(LocalTextProvider provides textProvider) {
        when (selectPrimaryNavLayout(platform, adaptiveInfo)) {
            PrimaryNavLayout.TopBar ->
                PrimaryShellTopBarLayout(
                    component = component,
                    selectedSection = selectedSection,
                    modifier = modifier,
                    textProvider = textProvider,
                )

            PrimaryNavLayout.NavigationRail ->
                PrimaryShellNavigationRailLayout(
                    component = component,
                    selectedSection = selectedSection,
                    modifier = modifier,
                    textProvider = textProvider,
                )

            PrimaryNavLayout.BottomBar ->
                PrimaryShellBottomNavigationLayout(
                    component = component,
                    selectedSection = selectedSection,
                    modifier = modifier,
                    textProvider = textProvider,
                )
        }
    }
}

@Composable
private fun rememberPrimaryShellTextProvider(): TextProvider {
    val parent = LocalTextProvider.current
    return remember(parent) { DefaultAppShellTextProvider.withFallback(parent) }
}

@Composable
private fun PrimaryShellNavigationRailLayout(
    component: PrimaryShellComponent,
    selectedSection: PrimarySection,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxSize()
                .background(SenseeTheme.colors.background),
    ) {
        var railExpanded by rememberSaveable { mutableStateOf(false) }
        val addAction = PrimaryNavDestinations[PRIMARY_ACTION_INDEX]
        SenseeNavigationRail(
            modifier = Modifier.fillMaxHeight(),
            expanded = railExpanded,
            onExpandedChange = { railExpanded = it },
            action = {
                SenseeNavigationActionButton(
                    icon = addAction.icon,
                    onClick = { component.selectSection(addAction.section) },
                    contentDescription = textProvider.text(addAction.labelKey),
                )
            },
        ) {
            // Two weighted spacers — one above, one below — sandwich the nav items so they
            // sit vertically centred in the rail. The trailing spacer also pushes the rail's
            // `action` slot to the bottom edge (FAB-like). Gap between consecutive items is
            // provided by the rail's own `Arrangement.spacedBy`.
            //
            // `fillMaxWidth()` keeps every item the same horizontal width so the click pill
            // ends up the same width regardless of label length — same intent as `weight(1f)`
            // in the bottom bar, just expressed via Column's main axis.
            Spacer(modifier = Modifier.weight(1f))
            PrimaryNavDestinations.forEachIndexed { index, destination ->
                if (index == PRIMARY_ACTION_INDEX) return@forEachIndexed
                PrimaryNavigationItem(
                    destination = destination,
                    selected = selectedSection == destination.section,
                    onSelect = component::selectSection,
                    textProvider = textProvider,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
        ) {
            PrimaryShellChildren(component = component)
        }
    }
}

@Composable
private fun PrimaryShellTopBarLayout(
    component: PrimaryShellComponent,
    selectedSection: PrimarySection,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(SenseeTheme.colors.background),
    ) {
        val addAction = PrimaryNavDestinations[PRIMARY_ACTION_INDEX]
        val layout = SenseeTheme.layout
        val statusBarsTopPadding: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        SenseeTopNavigationBar(
            modifier = Modifier.fillMaxWidth(),
            action = {
                SenseeNavigationActionButton(
                    icon = addAction.icon,
                    onClick = { component.selectSection(addAction.section) },
                    contentDescription = textProvider.text(addAction.labelKey),
                )
            },
        ) {
            PrimaryNavDestinations.forEachIndexed { index, destination ->
                if (index == PRIMARY_ACTION_INDEX) return@forEachIndexed
                PrimaryNavigationItem(
                    destination = destination,
                    selected = selectedSection == destination.section,
                    onSelect = component::selectSection,
                    textProvider = textProvider,
                )
            }
        }

        // Tell descendants the top inset (status bar + bar height) is already consumed —
        // mirrors what PrimaryShellBottomNavigationLayout does for the bottom inset.
        // Dormant on web (statusBars == 0) but defensive if TopBar ever runs elsewhere.
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .consumeWindowInsets(
                        PaddingValues(top = layout.topNavigationHeight + statusBarsTopPadding),
                    ),
        ) {
            PrimaryShellChildren(component = component)
        }
    }
}

@Composable
private fun PrimaryShellBottomNavigationLayout(
    component: PrimaryShellComponent,
    selectedSection: PrimarySection,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(SenseeTheme.colors.background),
    ) {
        val navigationBarsBottomPadding: Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val layout = SenseeTheme.layout
        val showBottomBar by component.showBottomBar.collectAsState()

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .thenIf(showBottomBar) {
                        consumeWindowInsets(
                            PaddingValues(
                                bottom =
                                    layout.bottomNavigationHeight +
                                        navigationBarsBottomPadding,
                            ),
                        )
                    },
        ) {
            PrimaryShellChildren(component = component)
        }

        if (showBottomBar) {
            SenseeBottomNavigationBar(modifier = Modifier.fillMaxWidth()) {
                PrimaryNavDestinations.forEachIndexed { index, destination ->
                    if (index == PRIMARY_ACTION_INDEX) {
                        SenseeNavigationActionButton(
                            icon = destination.icon,
                            onClick = { component.selectSection(destination.section) },
                            contentDescription = textProvider.text(destination.labelKey),
                            modifier = Modifier.padding(horizontal = SenseeTheme.spacing.small),
                        )
                    } else {
                        PrimaryNavigationItem(
                            destination = destination,
                            selected = selectedSection == destination.section,
                            onSelect = component::selectSection,
                            textProvider = textProvider,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryNavigationItem(
    destination: PrimaryNavDestination,
    selected: Boolean,
    onSelect: (PrimarySection) -> Unit,
    textProvider: TextProvider,
    modifier: Modifier = Modifier,
) {
    SenseeNavigationItem(
        label = textProvider.text(destination.labelKey),
        icon = destination.icon,
        selected = selected,
        onClick = { onSelect(destination.section) },
        modifier = modifier,
    )
}

@Composable
private fun PrimaryShellChildren(component: PrimaryShellComponent) {
    Children(
        stack = component.stack,
        modifier = Modifier.fillMaxSize(),
        animation = stackAnimation(fade()),
    ) { child ->
        when (val instance = child.instance) {
            is HomeSectionComponent ->
                HomeSectionScreen(
                    component = instance,
                )

            is PracticeSectionComponent ->
                PracticeSectionScreen(
                    component = instance,
                )

            is LibrarySectionComponent ->
                LibrarySectionScreen(
                    component = instance,
                )

            is VocabularyEditorSectionComponent ->
                VocabularyEditorSectionScreen(
                    component = instance,
                )

            is ProfileSectionComponent ->
                ProfileSectionScreen(
                    component = instance,
                )

            else -> error("Unknown primary shell child: ${instance::class}")
        }
    }
}
