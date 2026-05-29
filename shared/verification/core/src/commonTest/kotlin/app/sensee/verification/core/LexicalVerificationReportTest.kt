package app.sensee.verification.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LexicalVerificationReportTest {
    @Test
    fun `the unavailable factory carries the reason and stays empty`() {
        val report = LexicalVerificationReport.unavailable("no provider answered")

        assertEquals(VerifierAvailability.Unavailable("no provider answered"), report.availability)
        assertTrue(report.findings.isEmpty())
        assertNull(report.family)
    }

    @Test
    fun `passing missingProviders to unavailable degrades instead of disabling`() {
        val report = LexicalVerificationReport.unavailable("two providers down", listOf("free-dictionary", "datamuse"))

        val availability = report.availability
        assertTrue(availability is VerifierAvailability.Degraded)
        assertEquals(listOf("free-dictionary", "datamuse"), availability.missingProviders)
    }

    @Test
    fun `an unavailable report cannot smuggle evidence findings or family through the construction guard`() {
        assertFailsWith<IllegalArgumentException> {
            LexicalVerificationReport(
                availability = VerifierAvailability.Unavailable("test"),
                findings =
                    listOf(
                        Finding(
                            code = "X",
                            severity = FindingSeverity.Info,
                            target = FindingTarget.Headword,
                            message = "should never appear",
                            sources = emptyList(),
                        ),
                    ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            LexicalVerificationReport(
                availability = VerifierAvailability.Unavailable("test"),
                family = FamilyContext(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            LexicalVerificationReport(
                availability = VerifierAvailability.Unavailable("test"),
                cefr =
                    EvidenceSet(
                        listOf(
                            Observation(
                                value = CefrLevel.A1,
                                source = LexicalSourceRef(sourceId = "x", fetchedAtEpochMillis = 0L),
                                confidence = Confidence.Low,
                            ),
                        ),
                    ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            LexicalVerificationReport(
                availability = VerifierAvailability.Unavailable("test"),
                normalized = NormalizationOutcome(canonical = "x"),
            )
        }
    }
}
