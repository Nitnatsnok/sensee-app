package app.sensee.quality.konsist.runtime

import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.normalizedProjectPath
import app.sensee.quality.konsist.productionScopeFiles
import app.sensee.quality.konsist.violation
import app.sensee.quality.konsist.withoutCommentsAndStringLiterals
import org.junit.jupiter.api.Test

class SystemClockKonsistTest {
    @Test
    fun `production code must not read the system clock directly`() {
        val violations =
            productionScopeFiles()
                .filterNot { file -> file.normalizedProjectPath() in CLOCK_SEAM_PATHS }
                .flatMap { file ->
                    val path = file.normalizedProjectPath()
                    val code = file.text.withoutCommentsAndStringLiterals()
                    SYSTEM_CLOCK
                        .filter { directRead -> directRead.containsMatchIn(code) }
                        .map { directRead ->
                            violation(
                                subject = path,
                                message =
                                    "reads the system clock via ${directRead.pattern}; inject Clock instead " +
                                        "(new adapters belong in CLOCK_SEAM_PATHS)",
                            )
                        }
                }

        assertNoViolations(violations)
    }

    private companion object {
        val CLOCK_SEAM_PATHS =
            setOf(
                "shared/core/platform/src/commonMain/kotlin/app/sensee/core/platform/PlatformClockProviders.kt",
                "shared/srs/engine/src/commonMain/kotlin/app/sensee/srs/engine/clock/SystemSrsClock.kt",
                "shared/tts/cache/src/commonMain/kotlin/app/sensee/tts/cache/Clock.kt",
            )

        val SYSTEM_CLOCK =
            listOf(
                Regex("""\bClock\.System\b"""),
                Regex("""\bSystem\.currentTimeMillis\s*\("""),
                Regex("""\bInstant\.now\s*\("""),
            )
    }
}
