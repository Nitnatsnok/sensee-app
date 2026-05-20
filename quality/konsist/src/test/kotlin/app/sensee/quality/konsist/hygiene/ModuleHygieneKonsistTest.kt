package app.sensee.quality.konsist.hygiene

import app.sensee.quality.konsist.KonsistTestSupport
import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.collectModuleBuildRoots
import app.sensee.quality.konsist.isProductionSourcePath
import app.sensee.quality.konsist.konsistProjectRoot
import app.sensee.quality.konsist.normalizedProjectPath
import app.sensee.quality.konsist.violation
import org.junit.jupiter.api.Test
import java.io.File

class ModuleHygieneKonsistTest {
    @Test
    fun `kotlin files should declare at least one top-level entity`() {
        val violations =
            KonsistTestSupport.sharedScope.files
                .filter { it.normalizedProjectPath().isProductionSourcePath() }
                .filter { it.declarations(includeNested = false, includeLocal = false).none() }
                .map { file ->
                    violation(
                        subject = file.normalizedProjectPath(),
                        message = "file has no top-level declarations; remove it or restore content",
                    )
                }

        assertNoViolations(violations)
    }

    @Test
    fun `shared modules should expose production sources or a schema`() {
        val projectRoot = konsistProjectRoot()
        val sharedRoot = projectRoot.resolve("shared")
        val moduleRoots = collectModuleBuildRoots(sharedRoot)

        val violations =
            moduleRoots
                .filter { moduleRoot ->
                    // Schema-only SQLDelight modules (a `.sq` tree, no Kotlin) are
                    // intentional and detected structurally — no hardcoded allowlist.
                    !moduleRoot.hasProductionKotlin() && !moduleRoot.hasSqlDelightSchema()
                }.map { moduleRoot ->
                    violation(
                        subject = moduleRoot.relativizedPath(projectRoot),
                        message = "module has no production sources and no SQLDelight schema; remove it or add code",
                    )
                }

        assertNoViolations(violations)
    }

    private fun File.hasProductionKotlin(): Boolean =
        PRODUCTION_SOURCE_SETS
            .map { resolve("src/$it/kotlin") }
            .any { dir -> dir.isDirectory && dir.walkTopDown().any { it.isFile && it.extension == "kt" } }

    private fun File.hasSqlDelightSchema(): Boolean =
        resolve("src").let { src ->
            src.isDirectory && src.walkTopDown().any { it.isFile && it.extension == "sq" }
        }

    private fun File.relativizedPath(base: File): String = relativeTo(base).path.replace('\\', '/')

    private companion object {
        val PRODUCTION_SOURCE_SETS =
            listOf(
                "commonMain",
                "androidMain",
                "iosMain",
                "jvmMain",
                "jsMain",
                "wasmJsMain",
                "webMain",
                "nativeMain",
            )
    }
}
