package app.sensee.quality.konsist.layering

import app.sensee.quality.konsist.KonsistTestSupport
import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.importedPath
import app.sensee.quality.konsist.isProductionSourcePath
import app.sensee.quality.konsist.matchesPackagePrefix
import app.sensee.quality.konsist.normalizedProjectPath
import app.sensee.quality.konsist.violation
import org.junit.jupiter.api.Test

/**
 * The sense-store cutover makes `practice/domain` a leaf over `srs.core`:
 * `submitReview` returns a practice-owned `ReviewOutcome` and due counts come from
 * a narrow `DuePracticeRepository`, so neither `practice/domain` nor a Home
 * dashboard pulls in Library's catalog projection just to advance or count cards.
 */
class PracticeDomainLeafKonsistTest {
    @Test
    fun `practice domain does not depend on library`() {
        val violations = libraryImportViolations("/feature/practice/domain/src/")

        assertNoViolations(violations)
    }

    @Test
    fun `home presentation impl stays free of the library catalog projection`() {
        val violations = libraryImportViolations("/feature/home/presentation/impl/src/")

        assertNoViolations(violations)
    }

    private fun libraryImportViolations(pathFragment: String): List<String> =
        KonsistTestSupport.sharedScope.files
            .filter { file ->
                val path = file.normalizedProjectPath()
                path.contains(pathFragment) && path.isProductionSourcePath()
            }.flatMap { file ->
                file.imports.mapNotNull { importDeclaration ->
                    val imported = importDeclaration.importedPath()
                    if (imported.matchesPackagePrefix("app.sensee.feature.library")) {
                        violation(
                            subject = file.normalizedProjectPath(),
                            message = "depends on library package '$imported'",
                        )
                    } else {
                        null
                    }
                }
            }
}
