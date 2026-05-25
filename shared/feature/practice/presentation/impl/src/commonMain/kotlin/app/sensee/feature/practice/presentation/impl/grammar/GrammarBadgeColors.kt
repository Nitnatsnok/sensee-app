package app.sensee.feature.practice.presentation.impl.grammar

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.ui.designSystem.component.badge.SenseeBadgeColors
import app.sensee.ui.designSystem.component.badge.SenseeBadgeDefaults
import app.sensee.ui.designSystem.theme.SenseeTheme

// Practice uses a feature-level palette because inline grammar chips need
// stronger category contrast than the design-system tonal button colors.
@Composable
internal fun grammarUnitBadgeColors(type: GrammarUnitType): SenseeBadgeColors {
    val dark = SenseeTheme.colors.background.luminance() < DARK_BACKGROUND_LUMINANCE_THRESHOLD
    return when (type) {
        GrammarUnitType.Verb,
        GrammarUnitType.IrregularVerb,
        GrammarUnitType.PhrasalVerb,
        GrammarUnitType.ModalVerb,
        GrammarUnitType.AuxiliaryVerb,
        -> if (dark) VerbDark else VerbLight
        GrammarUnitType.Noun -> if (dark) NounDark else NounLight
        GrammarUnitType.Adjective,
        GrammarUnitType.Adverb,
        -> if (dark) ModifierDark else ModifierLight
        GrammarUnitType.Idiom,
        GrammarUnitType.Phrase,
        -> if (dark) ExpressionDark else ExpressionLight
        GrammarUnitType.Pronoun,
        GrammarUnitType.Determiner,
        GrammarUnitType.Numeral,
        GrammarUnitType.Article,
        GrammarUnitType.Preposition,
        GrammarUnitType.Conjunction,
        GrammarUnitType.Interjection,
        -> SenseeBadgeDefaults.neutralColors()
        is GrammarUnitType.Unknown -> SenseeBadgeDefaults.neutralColors()
    }
}

// Currently returns the neutral palette regardless of [tag]; the parameter is
// kept so future per-category palettes can land without touching call sites.
@Composable
@Suppress("UnusedParameter")
internal fun grammarTagBadgeColors(tag: GrammarTag): SenseeBadgeColors = SenseeBadgeDefaults.neutralColors()

private const val DARK_BACKGROUND_LUMINANCE_THRESHOLD = 0.5f

private fun badge(
    container: Color,
    content: Color,
): SenseeBadgeColors =
    SenseeBadgeColors(
        container = container,
        content = content,
        border = Color.Transparent,
    )

private val VerbLight = badge(container = Color(0xFFC4D6FF), content = Color(0xFF1A2A6B))
private val VerbDark = badge(container = Color(0xFF2A3870), content = Color(0xFFD8E1FF))

private val NounLight = badge(container = Color(0xFFB8F2E2), content = Color(0xFF0F4D44))
private val NounDark = badge(container = Color(0xFF154D43), content = Color(0xFFB8F2E2))

private val ModifierLight = badge(container = Color(0xFFFFE3A8), content = Color(0xFF5C3A00))
private val ModifierDark = badge(container = Color(0xFF5C3A00), content = Color(0xFFFFE3A8))

private val ExpressionLight = badge(container = Color(0xFFFFD0E0), content = Color(0xFF7A1845))
private val ExpressionDark = badge(container = Color(0xFF6E1A3E), content = Color(0xFFFFD0E0))
