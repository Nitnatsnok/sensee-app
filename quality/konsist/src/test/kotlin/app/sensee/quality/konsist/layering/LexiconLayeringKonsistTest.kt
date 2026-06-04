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
 * `lexicon` owns the canonical sense store while staying minimally dependent
 * (ADR-002): `lexicon/domain` is a pure neutral leaf and `lexicon/data` may
 * reach persistence but never a feature or SRS. The data-package ban for
 * `lexicon/domain` is already covered by the shared "no domain depends on data"
 * guard; these rules add the SRS / feature / persistence boundaries.
 */
class LexiconLayeringKonsistTest {
    @Test
    fun `lexicon domain stays free of persistence, SRS, and feature deps`() {
        val violations =
            forbiddenImportViolations(
                pathFragment = "/lexicon/domain/src/",
                forbidden =
                    listOf(
                        "app.sensee.feature" to "feature",
                        "app.sensee.srs" to "SRS",
                        "app.sensee.database" to "persistence",
                        "app.sensee.core.database" to "persistence",
                    ),
            )

        assertNoViolations(violations)
    }

    @Test
    fun `lexicon data does not reach into feature or SRS`() {
        val violations =
            forbiddenImportViolations(
                pathFragment = "/lexicon/data/src/",
                forbidden =
                    listOf(
                        "app.sensee.feature" to "feature",
                        "app.sensee.srs" to "SRS",
                    ),
            )

        assertNoViolations(violations)
    }

    private fun forbiddenImportViolations(
        pathFragment: String,
        forbidden: List<Pair<String, String>>,
    ): List<String> =
        KonsistTestSupport.sharedScope.files
            .filter { file ->
                val path = file.normalizedProjectPath()
                path.contains(pathFragment) && path.isProductionSourcePath()
            }.flatMap { file ->
                file.imports.mapNotNull { importDeclaration ->
                    val imported = importDeclaration.importedPath()
                    forbidden
                        .firstOrNull { (prefix, _) -> imported.matchesPackagePrefix(prefix) }
                        ?.let { (_, label) ->
                            violation(
                                subject = file.normalizedProjectPath(),
                                message = "depends on $label package '$imported'",
                            )
                        }
                }
            }
}
