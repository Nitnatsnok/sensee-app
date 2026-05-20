package app.sensee.quality.konsist.layering

import app.sensee.quality.konsist.KonsistTestSupport
import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.featurePackageName
import app.sensee.quality.konsist.importedPath
import app.sensee.quality.konsist.isProductionSourcePath
import app.sensee.quality.konsist.normalizedProjectPath
import app.sensee.quality.konsist.strippedText
import app.sensee.quality.konsist.violation
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.jupiter.api.Test

class FeatureLayeringKonsistTest {
    // --- @Composable placement -------------------------------------------------

    @Test
    fun `composable declarations should stay in ui app-shell or feature impl layers`() {
        val violations =
            KonsistTestSupport.sharedScope
                .functions(includeNested = true, includeLocal = false)
                .asSequence()
                .filter { it.hasAnnotationWithName("Composable") }
                .map { it.containingFile.normalizedProjectPath() }
                .filter { it.isProductionSourcePath() }
                .distinct()
                .filterNot { path ->
                    path.startsWith("shared/app-shell/") ||
                        path.startsWith("shared/ui/") ||
                        KonsistTestSupport.featurePresentationImplRegex.containsMatchIn(path)
                }.map { path ->
                    violation(
                        subject = path,
                        message = "contains @Composable outside allowed UI layers",
                    )
                }.toList()

        assertNoViolations(violations)
    }

    @Test
    fun `presentation navigation-api should not contain composables`() {
        val violations =
            KonsistTestSupport.featureScope
                .functions(includeNested = true, includeLocal = false)
                .asSequence()
                .filter { it.hasAnnotationWithName("Composable") }
                .map { it.containingFile.normalizedProjectPath() }
                .filter { it.contains("/presentation/navigation-api/src/") && it.isProductionSourcePath() }
                .distinct()
                .map { path ->
                    violation(
                        subject = path,
                        message = "presentation.navigation-api must stay UI-free (no @Composable)",
                    )
                }.toList()

        assertNoViolations(violations)
    }

    // --- Metro DI annotation bans ----------------------------------------------

    /**
     * Stripped-text scan is the deliberate "last resort" the Konsist docs
     * sanction when the declaration API is ergonomically short: Metro markers
     * land on classes, constructors, functions *and* properties, so a single
     * declaration-kind query would silently miss carriers. Text over the
     * comment/string-stripped production source is the reliable, narrow form
     * here, and covers both Metro tests below.
     */
    @Test
    fun `presentation api should not use metro implementation annotations`() {
        val violations =
            KonsistTestSupport.featureScope.files
                .filter { file ->
                    val path = file.normalizedProjectPath()
                    path.contains("/presentation/api/src/") &&
                        path.isProductionSourcePath() &&
                        KonsistTestSupport.metroAnnotationRegex.containsMatchIn(file.strippedText())
                }.map { file ->
                    violation(
                        subject = file.normalizedProjectPath(),
                        message = "uses Metro DI annotations in presentation.api",
                    )
                }

        assertNoViolations(violations)
    }

    @Test
    fun `presentation navigation-api should not use metro implementation annotations`() {
        val violations =
            KonsistTestSupport.featureScope.files
                .filter { file ->
                    val path = file.normalizedProjectPath()
                    path.contains("/presentation/navigation-api/src/") &&
                        path.isProductionSourcePath() &&
                        KonsistTestSupport.metroAnnotationRegex.containsMatchIn(file.strippedText())
                }.map { file ->
                    violation(
                        subject = file.normalizedProjectPath(),
                        message = "uses Metro DI annotations in presentation.navigation-api",
                    )
                }

        assertNoViolations(violations)
    }

    // --- Layer dependency direction --------------------------------------------

    /**
     * Layer-direction rules expressed via the official Konsist Architecture
     * API (package globs + import graph) instead of brittle import-substring
     * scans. Run over [Konsist.scopeFromProduction] so feature test sources do
     * not trip the rules; the globs only match feature packages. Cross-feature
     * isolation stays a custom check below — it is per-feature and cannot be a
     * static [Layer].
     */
    @Test
    fun `presentation api and navigation-api must not depend on impl`() {
        Konsist.scopeFromProduction().assertArchitecture {
            val api = Layer("PresentationApi", "app.sensee.feature..presentation.api..")
            val navApi = Layer("PresentationNavApi", "app.sensee.feature..presentation.navigationApi..")
            val impl = Layer("PresentationImpl", "app.sensee.feature..presentation.impl..")

            api.doesNotDependOn(impl)
            navApi.doesNotDependOn(impl)
        }
    }

    @Test
    fun `feature domain and data must not depend on presentation`() {
        Konsist.scopeFromProduction().assertArchitecture {
            val presentation = Layer("Presentation", "app.sensee.feature..presentation..")
            val domain = Layer("Domain", "app.sensee.feature..domain..")
            val data = Layer("Data", "app.sensee.feature..data..")

            domain.doesNotDependOn(presentation)
            data.doesNotDependOn(presentation)
        }
    }

    // --- Cross-feature isolation (per-feature; not a static Layer) --------------

    @Test
    fun `presentation impl should not depend on other feature packages`() {
        val violations =
            KonsistTestSupport.featureScope.files
                .filter { file ->
                    val path = file.normalizedProjectPath()
                    path.contains("/presentation/impl/src/") && path.isProductionSourcePath()
                }.flatMap { file ->
                    val ownerFeature = file.featurePackageName() ?: return@flatMap emptyList()
                    file.imports.mapNotNull { importDeclaration ->
                        val importedPath = importDeclaration.importedPath()
                        val importedFeature =
                            KonsistTestSupport.featurePackageRegex
                                .find(importedPath)
                                ?.groupValues
                                ?.get(1)

                        when {
                            importedFeature == null -> null
                            importedFeature == ownerFeature -> null
                            else ->
                                violation(
                                    subject = file.normalizedProjectPath(),
                                    message = "depends on other feature package '$importedPath'",
                                )
                        }
                    }
                }

        assertNoViolations(violations)
    }
}
