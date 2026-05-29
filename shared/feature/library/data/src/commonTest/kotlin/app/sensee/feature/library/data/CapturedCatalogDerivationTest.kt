package app.sensee.feature.library.data

import app.sensee.feature.library.domain.CatalogOrigin
import app.sensee.feature.library.domain.LemmaId
import app.sensee.grammar.domain.ComponentRole
import app.sensee.grammar.domain.ComponentSalience
import app.sensee.grammar.domain.GrammarCategory
import app.sensee.grammar.domain.GrammarForm
import app.sensee.grammar.domain.GrammarTag
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.IrregularForms
import app.sensee.grammar.domain.UnitComponent
import app.sensee.lexicon.domain.EntryId
import app.sensee.lexicon.domain.EntryStatus
import app.sensee.lexicon.domain.LexicalEntry
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.WordFamilyMember
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
                    senses = listOf(Sense(translation = "идти", explanation = "to move on foot")),
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
                    senses = listOf(Sense(translation = "наткнуться", baseLemma = "come")),
                ),
                LexicalEntry(
                    EntryId("e2"),
                    "come up",
                    EntryStatus.Confirmed,
                    senses = listOf(Sense(translation = "возникать", baseLemma = "Come")),
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
    fun `headLemma overrides baseLemma so a multi-word lemma still groups under its family head`() {
        val entries =
            listOf(
                LexicalEntry(
                    EntryId("e1"),
                    "come",
                    EntryStatus.Confirmed,
                    senses = listOf(Sense(translation = "приходить", baseLemma = "come", headLemma = "come")),
                ),
                LexicalEntry(
                    EntryId("e2"),
                    "come across",
                    EntryStatus.Confirmed,
                    // baseLemma here would be the multi-word lemma — the family head
                    // is the verb `come`, exposed via headLemma.
                    senses =
                        listOf(
                            Sense(translation = "наткнуться", baseLemma = "come across", headLemma = "come"),
                        ),
                ),
            )

        val deck = CapturedCatalogDerivation.capturedDeck(entries)!!
        val lemmaIds = deck.cards.map { it.lemmaId }.toSet()

        assertEquals(1, lemmaIds.size)
        assertEquals("come", lemmaIds.single().value.removePrefix("captured-lemma:"))
    }

    @Test
    fun `irregular forms drive variant cards regardless of POS classification`() {
        val entries =
            listOf(
                LexicalEntry(
                    EntryId("e1"),
                    "come",
                    EntryStatus.Confirmed,
                    senses =
                        listOf(
                            // unitType is plain `verb`, not IrregularVerb — the
                            // presence of the principal parts is the trigger.
                            Sense(
                                translation = "приходить",
                                baseLemma = "come",
                                unitType = GrammarUnitType.Verb,
                                irregularForms = IrregularForms("come", "came", "come"),
                            ),
                        ),
                ),
            )

        val cards = CapturedCatalogDerivation.capturedDeck(entries)!!.cards

        // sense + past + past_participle (infinitive is the headword itself).
        assertEquals(3, cards.size)
        val variants = cards.drop(1)
        assertEquals(listOf("came", "come"), variants.map { it.headword })
        assertEquals(
            listOf(
                GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.PastTense),
                GrammarTag(GrammarCategory.VerbIrregular, GrammarForm.PastParticiple),
            ),
            variants.map { it.grammarTags.single() },
        )
        assertTrue(cards.all { CapturedCatalogDerivation.isCapturedCard(it.id) })
        assertEquals(1, cards.map { it.lemmaId }.toSet().size)
    }

    @Test
    fun `form-variant card ids are lemma-stable so re-deriving keeps the same identity`() {
        fun entryFor(id: String) =
            LexicalEntry(
                EntryId(id),
                "come",
                EntryStatus.Confirmed,
                senses =
                    listOf(
                        Sense(
                            translation = "приходить",
                            baseLemma = "come",
                            irregularForms = IrregularForms("come", "came", "come"),
                        ),
                    ),
            )

        val first = CapturedCatalogDerivation.capturedDeck(listOf(entryFor("e1")))!!.cards.drop(1)
        val second = CapturedCatalogDerivation.capturedDeck(listOf(entryFor("e2")))!!.cards.drop(1)

        // Two different entries (capture sessions) for the same verb produce
        // form cards with the same id — SRS state for `came`/`come` survives.
        assertEquals(first.map { it.id }, second.map { it.id })
    }

    @Test
    fun `two senses of the same verb do not duplicate the form-variant cards`() {
        val entries =
            listOf(
                LexicalEntry(
                    EntryId("e1"),
                    "run",
                    EntryStatus.Confirmed,
                    senses =
                        listOf(
                            Sense(
                                translation = "бежать",
                                baseLemma = "run",
                                irregularForms = IrregularForms("run", "ran", "run"),
                            ),
                            Sense(
                                translation = "управлять",
                                baseLemma = "run",
                                irregularForms = IrregularForms("run", "ran", "run"),
                            ),
                        ),
                ),
            )

        val cards = CapturedCatalogDerivation.capturedDeck(entries)!!.cards

        // 2 sense cards + 2 distinct form cards (past + past_participle), not 2 × 2.
        assertEquals(4, cards.size)
        assertEquals(cards.size, cards.map { it.id }.toSet().size)
    }

    @Test
    fun `a lemma surfaces the word-family derivatives of its senses`() {
        val entries =
            listOf(
                LexicalEntry(
                    EntryId("e1"),
                    "decide",
                    EntryStatus.Confirmed,
                    senses =
                        listOf(
                            Sense(
                                translation = "решать",
                                baseLemma = "decide",
                                wordFamily =
                                    listOf(
                                        WordFamilyMember("decision", GrammarUnitType.Noun),
                                        WordFamilyMember("decisive", GrammarUnitType.Adjective),
                                    ),
                            ),
                        ),
                ),
            )

        val deck = CapturedCatalogDerivation.capturedDeck(entries)!!
        val lemma = CapturedCatalogDerivation.capturedLemma(entries, deck.cards.single().lemmaId)!!

        assertEquals(
            listOf("decision" to GrammarUnitType.Noun, "decisive" to GrammarUnitType.Adjective),
            lemma.derivatives.map { it.text to it.unitType },
        )
    }

    @Test
    fun `a captured card carries its component breakdown with salience`() {
        val entries =
            listOf(
                LexicalEntry(
                    EntryId("e1"),
                    "come across",
                    EntryStatus.Confirmed,
                    senses =
                        listOf(
                            Sense(
                                translation = "наткнуться",
                                baseLemma = "come",
                                headLemma = "come",
                                components =
                                    listOf(
                                        UnitComponent("come", ComponentRole.Head, ComponentSalience.Primary),
                                        UnitComponent("across", ComponentRole.Particle, ComponentSalience.Secondary),
                                    ),
                            ),
                        ),
                ),
            )

        val card = CapturedCatalogDerivation.capturedDeck(entries)!!.cards.single()

        assertEquals(
            listOf("come" to ComponentSalience.Primary, "across" to ComponentSalience.Secondary),
            card.components.map { it.text to it.salience },
        )
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
