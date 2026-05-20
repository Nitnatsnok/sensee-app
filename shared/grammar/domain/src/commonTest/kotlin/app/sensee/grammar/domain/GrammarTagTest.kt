package app.sensee.grammar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GrammarTagTest {
    @Test
    fun `a form is valid only for its own category`() {
        val tag = GrammarTag(GrammarCategory.Number, GrammarForm.Plural)

        assertEquals(GrammarCategory.Number, tag.category)
        assertEquals(GrammarForm.Plural, tag.form)
    }

    @Test
    fun `an out-of-table category and form pair cannot be constructed`() {
        assertFailsWith<IllegalArgumentException> {
            GrammarTag(GrammarCategory.Number, GrammarForm.PastTense)
        }
    }

    @Test
    fun `regular and irregular verb categories both allow the verb-form set`() {
        val regular = GrammarTag(GrammarCategory.Verb, GrammarForm.PastTense)
        val irregular = GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.PastParticiple)

        assertEquals(GrammarCategory.Verb, regular.category)
        assertEquals(GrammarCategory.VerbIrregular, irregular.category)
    }
}
