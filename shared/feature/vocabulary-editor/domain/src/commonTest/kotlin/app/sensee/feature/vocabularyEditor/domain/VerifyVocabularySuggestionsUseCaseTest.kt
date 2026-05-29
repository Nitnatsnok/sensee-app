package app.sensee.feature.vocabularyEditor.domain

import app.sensee.core.testKit.noOpAppDiagnostics
import app.sensee.grammar.domain.GrammarUnitType
import app.sensee.grammar.domain.StudiedSentence
import app.sensee.lexicon.domain.ContextualApplication
import app.sensee.verification.core.ExampleFinding
import app.sensee.verification.core.ExampleIssue
import app.sensee.verification.core.ExampleLocation
import app.sensee.verification.core.FindingSeverity
import app.sensee.verification.core.LexicalVerificationQuery
import app.sensee.verification.core.LexicalVerificationReport
import app.sensee.verification.core.LexicalVerifier
import app.sensee.verification.core.VerifierAvailability
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VerifyVocabularySuggestionsUseCaseTest {
    @Test
    fun `verification query carries candidate examples with sense ids`() =
        runTest {
            val verifier = RecordingVerifier()
            val useCase = VerifyVocabularySuggestionsUseCase(verifier, noOpAppDiagnostics())
            val candidates =
                listOf(
                    candidate("sense-a", "They [[come across]] well."),
                    candidate("sense-b", "We [[come across]] old photos."),
                )

            val result = useCase("come across", "en", candidates)

            val examples = assertNotNull(verifier.lastQuery).examplesToValidate
            assertEquals(listOf("sense-a", "sense-b"), examples.map { it.senseHintId })
            assertEquals("They come across well.", examples.first().sentence.plainText())
            assertEquals("come across", examples.first().sentence.studiedUnitDisplay())
            assertEquals(candidates.size, result.count { it.verification != null })
        }

    @Test
    fun `verification query separates entry type from part of speech`() =
        runTest {
            val verifier = RecordingVerifier()
            val useCase = VerifyVocabularySuggestionsUseCase(verifier, noOpAppDiagnostics())
            val candidates =
                listOf(
                    candidate(
                        "sense-a",
                        "They [[come across]] well.",
                        unitType = GrammarUnitType.PhrasalVerb,
                    ),
                )

            useCase("come across", "en", candidates)

            val query = assertNotNull(verifier.lastQuery)
            assertEquals("phrasal_verb", query.expectedEntryType?.id)
            assertEquals("verb", query.expectedPartOfSpeech?.id)
            assertEquals(
                "verb",
                query
                    .senseHints
                    .single()
                    .pos
                    ?.id,
            )
        }

    @Test
    fun `example findings map global verifier indices back to candidate-local indices`() =
        runTest {
            val verifier =
                RecordingVerifier(
                    LexicalVerificationReport(
                        availability = VerifierAvailability.Available,
                        exampleFindings =
                            listOf(
                                ExampleFinding(
                                    senseHintId = null,
                                    exampleIndex = 1,
                                    issues = listOf(issue("STYLE")),
                                ),
                                ExampleFinding(
                                    senseHintId = "sense-b",
                                    exampleIndex = 2,
                                    issues = listOf(issue("GRAMMAR")),
                                ),
                            ),
                    ),
                )
            val useCase = VerifyVocabularySuggestionsUseCase(verifier, noOpAppDiagnostics())
            val candidates =
                listOf(
                    candidate(
                        "sense-a",
                        "They [[come across]] well.",
                        "It [[comes across]] as cold.",
                    ),
                    candidate("sense-b", "We [[come across]] old photos."),
                )

            val result = useCase("come across", "en", candidates)

            assertEquals(
                SenseVerificationScope.Example(1),
                result[0]
                    .verification
                    ?.findings
                    ?.single()
                    ?.scope,
            )
            assertEquals(
                SenseVerificationScope.Example(0),
                result[1]
                    .verification
                    ?.findings
                    ?.single()
                    ?.scope,
            )
        }

    @Test
    fun `a finding tagged for a sense but carrying another sense's example index is dropped`() =
        runTest {
            val verifier =
                RecordingVerifier(
                    LexicalVerificationReport(
                        availability = VerifierAvailability.Available,
                        exampleFindings =
                            listOf(
                                // Tagged for sense-a, but global index 2 belongs
                                // to sense-b's example — an inconsistent provider.
                                ExampleFinding(
                                    senseHintId = "sense-a",
                                    exampleIndex = 2,
                                    issues = listOf(issue("GRAMMAR")),
                                ),
                            ),
                    ),
                )
            val useCase = VerifyVocabularySuggestionsUseCase(verifier, noOpAppDiagnostics())
            val candidates =
                listOf(
                    candidate(
                        "sense-a",
                        "They [[come across]] well.",
                        "It [[comes across]] as cold.",
                    ),
                    candidate("sense-b", "We [[come across]] old photos."),
                )

            val result = useCase("come across", "en", candidates)

            // The out-of-range tagged finding must not be mapped onto a local
            // example it does not belong to; it is dropped from every candidate.
            assertEquals(0, result[0].verification?.findings?.size ?: 0)
            assertEquals(0, result[1].verification?.findings?.size ?: 0)
        }

    private class RecordingVerifier(
        private val report: LexicalVerificationReport =
            LexicalVerificationReport(availability = VerifierAvailability.Available),
    ) : LexicalVerifier {
        var lastQuery: LexicalVerificationQuery? = null
            private set

        override suspend fun verify(query: LexicalVerificationQuery): LexicalVerificationReport {
            lastQuery = query
            return report
        }
    }

    private fun candidate(
        id: String,
        vararg examples: String,
        unitType: GrammarUnitType? = null,
    ): SenseCandidate =
        SenseCandidate(
            id = SenseCandidateId(id),
            translation = "translation-$id",
            unitType = unitType,
            contextualApplications =
                examples.map { example ->
                    ContextualApplication(StudiedSentence.parse(example))
                },
        )

    private fun issue(code: String): ExampleIssue =
        ExampleIssue(
            code = code,
            severity = FindingSeverity.Warning,
            location = ExampleLocation.WholeSentence,
            message = "test issue",
            sources = emptyList(),
        )
}
