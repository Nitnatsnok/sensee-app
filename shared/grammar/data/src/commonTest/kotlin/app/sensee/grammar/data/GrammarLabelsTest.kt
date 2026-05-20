package app.sensee.grammar.data

import app.sensee.grammar.domain.ComplementType
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarLabels
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.UsageLabel
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GrammarLabelsTest {
    private val labels =
        Json { ignoreUnknownKeys = true }
            .decodeFromString<GrammarTaxonomyDto>(
                GrammarTaxonomyMockFixtures().fixtures.getValue("practice/grammar/taxonomy"),
            ).toGrammarLabels()

    @Test
    fun `resolves unit type and grammar tag and usage and complement to native labels`() {
        assertEquals("Фразовый глагол", labels.unitType(GrammarUnitType.PhrasalVerb))
        assertEquals(
            "Неразделяемый",
            labels.form(GrammarTag(GrammarCategory.Separability, GrammarForm.Inseparable)),
        )
        assertEquals("Неформальный", labels.usage(UsageLabel.resolve("register", "informal")!!))
        assertEquals("Инфинитив с to", labels.complement(ComplementType.ToInfinitive))
    }

    @Test
    fun `the empty resolver returns null instead of crashing`() {
        assertNull(GrammarLabels.EMPTY.unitType(GrammarUnitType.Idiom))
        assertNull(GrammarLabels.EMPTY.complement(ComplementType.Gerund))
    }
}
