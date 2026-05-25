package app.sensee.grammar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GrammarTagTest {
    private val invariants =
        mapOf(
            "number" to setOf("singular", "plural"),
            "verb" to setOf("infinitive", "past_tense"),
        )

    @Test
    fun `direct construction carries the pair without enforcing the runtime invariant`() {
        val tag = GrammarTag(GrammarCategory.Number, GrammarForm.Plural)

        assertEquals(GrammarCategory.Number, tag.category)
        assertEquals(GrammarForm.Plural, tag.form)
    }

    @Test
    fun `resolve passes valid pairs through and maps known ids to their data objects`() {
        val tag = GrammarTag.resolve("number", "plural", invariants)

        assertEquals(GrammarTag(GrammarCategory.Number, GrammarForm.Plural), tag)
    }

    @Test
    fun `resolve drops a pair the runtime invariants do not allow`() {
        assertNull(GrammarTag.resolve("number", "past_tense", invariants))
    }

    @Test
    fun `known invariant map validates built-in category form pairs`() {
        assertEquals(
            GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.PastParticiple),
            GrammarTag.resolve(
                categoryId = "verb_irregular",
                formId = "past_participle",
                allowedFormsByCategory = GrammarTag.knownAllowedFormsByCategory,
            ),
        )
        assertNull(
            GrammarTag.resolve(
                categoryId = "number",
                formId = "past_tense",
                allowedFormsByCategory = GrammarTag.knownAllowedFormsByCategory,
            ),
        )
    }

    @Test
    fun `resolve drops a category absent from the runtime invariants`() {
        assertNull(GrammarTag.resolve("expression_type", "fixed", invariants))
    }

    @Test
    fun `null invariants disable runtime validation - boundary fallback`() {
        val tag = GrammarTag.resolve("expression_type", "fixed", allowedFormsByCategory = null)

        assertEquals(
            GrammarTag(GrammarCategory.ExpressionType, GrammarForm.Fixed),
            tag,
        )
    }

    @Test
    fun `unknown ids carried through invariants resolve to Unknown branches`() {
        val withUnknownCategory =
            mapOf("brand_new_axis" to setOf("brand_new_value"))

        val tag = GrammarTag.resolve("brand_new_axis", "brand_new_value", withUnknownCategory)

        assertEquals(
            GrammarTag(
                GrammarCategory.Unknown("brand_new_axis"),
                GrammarForm.Unknown("brand_new_value"),
            ),
            tag,
        )
    }
}
