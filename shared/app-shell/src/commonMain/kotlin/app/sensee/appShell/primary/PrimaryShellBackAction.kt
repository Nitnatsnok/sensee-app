package app.sensee.appShell.primary

import app.sensee.core.decompose.navigation.ScreenConfig

internal enum class PrimaryShellBackAction { Pop, ReplaceWithHomeRoot, Unhandled }

/**
 * Action for a single back press in the primary shell.
 *
 * `ReplaceWithHomeRoot` covers two cases that would otherwise exit the app
 * from a section the user never consciously opened: a cold-start deep-link
 * landing in a non-Home section, and LRU rotation that pulled a non-Home tab
 * to the bottom of the stack. In both, the next back puts Home as the lone
 * root so a subsequent back is a clean app exit.
 */
internal fun primaryShellBackAction(destinations: List<ScreenConfig>): PrimaryShellBackAction =
    when {
        destinations.size > 1 -> PrimaryShellBackAction.Pop
        destinations.singleOrNull() is PrimarySectionConfig.HomeSection -> PrimaryShellBackAction.Unhandled
        else -> PrimaryShellBackAction.ReplaceWithHomeRoot
    }
