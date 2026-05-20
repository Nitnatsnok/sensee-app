package app.sensee.feature.library.data

import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.feature.library.domain.LemmaId
import app.sensee.feature.vocabularyEditor.domain.EntryId
import app.sensee.feature.vocabularyEditor.domain.EntryStatus
import app.sensee.feature.vocabularyEditor.domain.LexicalEntry
import app.sensee.feature.vocabularyEditor.domain.Meaning
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CapturedCatalogDerivationTest {
    @Test
    fun `only confirmed entries become captured cards`() {
        val entries =
            listOf(
                LexicalEntry(EntryId("e1"), "run", EntryStatus.Draft),
                LexicalEntry(
                    EntryId("e2"),
                    "walk",
                    EntryStatus.Confirmed,
                    meanings = listOf(Meaning(translation = "идти", explanation = "to move on foot")),
                ),
            )

        val deck = CapturedCatalogDerivation.capturedDeck(entries)

        assertTrue(deck != null)
        assertEquals(1, deck.cards.size)
        val card = deck.cards.single()
        assertEquals("walk", card.headword)
        assertEquals("идти", card.translation)
        assertTrue(CapturedCatalogDerivation.isCapturedCard(card.id))
        assertEquals(CapturedCatalogDerivation.DECK_ID, deck.deck.id)
        assertEquals(CatalogOrigin.Personal, deck.deck.origin)
    }

    @Test
    fun `senses sharing a base lemma are siblings under one parent lemma across entries`() {
        val entries =
            listOf(
                LexicalEntry(
                    EntryId("e1"),
                    "come across",
                    EntryStatus.Confirmed,
                    meanings = listOf(Meaning(translation = "наткнуться", baseLemma = "come")),
                ),
                LexicalEntry(
                    EntryId("e2"),
                    "come up",
                    EntryStatus.Confirmed,
                    meanings = listOf(Meaning(translation = "возникать", baseLemma = "Come")),
                ),
            )

        val deck = CapturedCatalogDerivation.capturedDeck(entries)!!
        val lemmaIds = deck.cards.map { it.lemmaId }.toSet()

        assertEquals(1, lemmaIds.size)
        val lemma = CapturedCatalogDerivation.capturedLemma(entries, lemmaIds.single())!!
        assertEquals("come", lemma.text)
        assertEquals(
            listOf("наткнуться", "возникать"),
            lemma.relatedCards.map { it.translation },
        )
    }

    @Test
    fun `an irregular verb sense yields related form-variant cards tagged structurally`() {
        val entries =
            listOf(
                LexicalEntry(
                    EntryId("e1"),
                    "come",
                    EntryStatus.Confirmed,
                    meanings =
                        listOf(
                            Meaning(
                                translation = "приходить",
                                baseLemma = "come",
                                unitType = GrammarUnitType.IrregularVerb,
                                irregularForms = IrregularForms("come", "came", "come"),
                            ),
                        ),
                ),
            )

        val cards = CapturedCatalogDerivation.capturedDeck(entries)!!.cards

        assertEquals(4, cards.size)
        val variants = cards.drop(1)
        assertEquals(listOf("come", "came", "come"), variants.map { it.headword })
        assertEquals(
            listOf(
                GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.Infinitive),
                GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.PastTense),
                GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.PastParticiple),
            ),
            variants.map { it.grammarTags.single() },
        )
        assertEquals(1, cards.map { it.lemmaId }.toSet().size)
    }

    @Test
    fun `a captured lemma id that has no senses does not resolve`() {
        assertNull(
            CapturedCatalogDerivation.capturedLemma(emptyList(), LemmaId("captured-lemma:come")),
        )
    }

    @Test
    fun `no confirmed entries yields no captured deck`() {
        val entries = listOf(LexicalEntry(EntryId("e1"), "run", EntryStatus.Draft))

        assertNull(CapturedCatalogDerivation.capturedDeck(entries))
    }
}
