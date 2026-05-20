package app.sensee.quality.konsist.structure

import app.sensee.quality.konsist.KonsistTestSupport
import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.collectModuleBuildRoots
import app.sensee.quality.konsist.isProductionSourcePath
import app.sensee.quality.konsist.kebabToCamel
import app.sensee.quality.konsist.konsistProjectRoot
import app.sensee.quality.konsist.matchesPackagePrefix
import app.sensee.quality.konsist.normalizedProjectPath
import app.sensee.quality.konsist.qualifiedDisplayName
import app.sensee.quality.konsist.violation
import org.junit.jupiter.api.Test

class ModuleStructureKonsistTest {
    @Test
    fun `production files must use the canonical module package prefix`() {
        val projectRoot = konsistProjectRoot()
        val moduleRoots =
            collectModuleBuildRoots(projectRoot.resolve("shared"))
                .map { it.relativeTo(projectRoot).path.replace('\\', '/') }
                .sortedByDescending { it.length }

        val violations =
            KonsistTestSupport.sharedScope.files
                .filter { it.normalizedProjectPath().isProductionSourcePath() }
                .mapNotNull { file ->
                    val path = file.normalizedProjectPath()
                    // Feature modules are covered by FeatureStructureKonsistTest.
                    if (path.startsWith("shared/feature/")) return@mapNotNull null

                    val moduleRoot = moduleRoots.firstOrNull { path.startsWith("$it/") } ?: return@mapNotNull null
                    val expectedPrefix = expectedPackagePrefixFor(moduleRoot)
                    val actualPackage = file.packagee?.name
                    if (actualPackage.matchesPackagePrefix(expectedPrefix)) {
                        null
                    } else {
                        violation(
                            subject = path,
                            message = "expected package starting with '$expectedPrefix', was '${actualPackage.orEmpty()}'",
                        )
                    }
                }

        assertNoViolations(violations)
    }

    @Test
    fun `at most one DependencyGraph should exist in the project`() {
        val graphs =
            KonsistTestSupport.sharedScope
                .interfaces(includeNested = false)
                .filter { it.hasAnnotationWithName("DependencyGraph") }

        val violations =
            if (graphs.size <= 1) {
                emptyList()
            } else {
                graphs.map { graph ->
                    violation(
                        subject = graph.qualifiedDisplayName(),
                        message =
                            "found ${graphs.size} @DependencyGraph declarations; " +
                                "only the app-shell composition root should declare one",
                    )
                }
            }

        assertNoViolations(violations)
    }

    /**
     * Derives the canonical package prefix for a shared (non-feature) module
     * from its Gradle path: `shared/<group>/<...>/<module>` →
     * `app.sensee.<group>.<...>.<module-as-camelCase>`.
     */
    private fun expectedPackagePrefixFor(moduleRoot: String): String =
        moduleRoot
            .removePrefix("shared/")
            .split('/')
            .joinToString(".") { it.kebabToCamel() }
            .let { "app.sensee.$it" }
}
