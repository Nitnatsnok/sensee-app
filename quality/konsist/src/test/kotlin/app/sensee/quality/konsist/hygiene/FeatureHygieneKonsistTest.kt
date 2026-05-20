package app.sensee.quality.konsist.hygiene

import app.sensee.quality.konsist.KonsistTestSupport
import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.normalizedProjectPath
import app.sensee.quality.konsist.violation
import org.junit.jupiter.api.Test

class FeatureHygieneKonsistTest {
    @Test
    fun `feature modules should not keep example placeholder tests`() {
        val violations =
            KonsistTestSupport.featureScope.files
                .map { it.normalizedProjectPath() }
                .filter { path ->
                    path.endsWith("/ExampleUnitTest.kt") ||
                        path.endsWith("/ExampleInstrumentedTest.kt")
                }.map { path ->
                    violation(
                        subject = path,
                        message = "remove template Example* tests; add real coverage when needed",
                    )
                }

        assertNoViolations(violations)
    }
}
