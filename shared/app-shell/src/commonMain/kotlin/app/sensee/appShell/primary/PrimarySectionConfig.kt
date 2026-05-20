package app.sensee.appShell.primary

import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.navigation.TargetedScreenConfig
import app.sensee.feature.home.presentation.navigationApi.HomeConfig
import app.sensee.feature.library.presentation.navigationApi.LibraryConfig
import app.sensee.feature.practice.presentation.navigationApi.PracticeConfig
import app.sensee.feature.practice.presentation.navigationApi.PracticeWebRoute
import app.sensee.feature.profile.presentation.navigationApi.ProfileConfig
import app.sensee.feature.vocabularyEditor.presentation.navigationApi.VocabularyEditorConfig
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface PrimarySectionConfig : TargetedScreenConfig<ScreenConfig> {
    @Serializable
    data class HomeSection(
        override val target: HomeConfig? = null,
    ) : PrimarySectionConfig

    @Serializable
    data class PracticeSection(
        override val target: PracticeConfig? = null,
    ) : PrimarySectionConfig

    @Serializable
    data class VocabularyEditor(
        override val target: VocabularyEditorConfig? = null,
    ) : PrimarySectionConfig

    @Serializable
    data class LibrarySection(
        override val target: LibraryConfig? = null,
    ) : PrimarySectionConfig

    @Serializable
    data class ProfileSection(
        override val target: ProfileConfig? = null,
    ) : PrimarySectionConfig
}

/**
 * Maps a root shell target to its section, carrying the target. Mirrors
 * `DefaultPrimaryShellComponent.open()` so the initial section and a later
 * navigation resolve identically. null = no root target (bootstrap → Home);
 * an unknown config is a wiring error, not a silent redirect to Home.
 */
internal fun ScreenConfig?.toPrimarySectionConfig(): PrimarySectionConfig =
    when (this) {
        null -> PrimarySectionConfig.HomeSection()
        is HomeConfig -> PrimarySectionConfig.HomeSection(this)
        is PracticeConfig -> PrimarySectionConfig.PracticeSection(this)
        is LibraryConfig -> PrimarySectionConfig.LibrarySection(this)
        is VocabularyEditorConfig -> PrimarySectionConfig.VocabularyEditor(this)
        is ProfileConfig -> PrimarySectionConfig.ProfileSection(this)
        else -> error("Unknown primary shell target: $this")
    }

/**
 * Single source of truth for the top-level section <-> URL path-segment
 * mapping used by Decompose Web Navigation. `pathFor` feeds the section
 * stack's `pathMapper`; `landingForPath` resolves a cold-start deep link
 * back to the section's default screen. Keeping both directions here keeps
 * them from drifting apart.
 */
internal object WebSectionRoute {
    fun pathFor(config: PrimarySectionConfig): String =
        when (config) {
            is PrimarySectionConfig.HomeSection -> "home"
            is PrimarySectionConfig.PracticeSection -> "practice"
            is PrimarySectionConfig.LibrarySection -> "library"
            is PrimarySectionConfig.VocabularyEditor -> "vocabulary"
            is PrimarySectionConfig.ProfileSection -> "profile"
        }

    fun landingForPath(
        segments: List<String>,
        parameters: Map<String, String>,
    ): ScreenConfig {
        val section = segments.firstOrNull().orEmpty()
        val rest = segments.drop(1)
        return when (section) {
            "practice" -> PracticeWebRoute.parse(rest, parameters) ?: PracticeConfig.Home
            "library" -> LibraryConfig.Home
            "vocabulary" -> VocabularyEditorConfig.QuickCapture
            "profile" -> ProfileConfig.Home
            else -> HomeConfig.Home
        }
    }
}
