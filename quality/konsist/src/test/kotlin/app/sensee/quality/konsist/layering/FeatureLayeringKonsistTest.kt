package app.sensee.quality.konsist.layering

import app.sensee.quality.konsist.KonsistTestSupport
import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.featurePackageName
import app.sensee.quality.konsist.importedPath
import app.sensee.quality.konsist.isProductionSourcePath
import app.sensee.quality.konsist.kebabToCamel
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
    fun `feature presentation domain and data layers respect the dependency direction`() {
        Konsist.scopeFromProduction().assertArchitecture {
            val presentation = Layer("Presentation", "app.sensee.feature..presentation..")
            val domain = Layer("Domain", "app.sensee.feature..domain..")
            val data = Layer("Data", "app.sensee.feature..data..")

            // Higher layers do not depend on presentation (presentation is the leaf).
            domain.doesNotDependOn(presentation)
            data.doesNotDependOn(presentation)
            // Data is sealed: nothing reaches it through imports, neither
            // presentation nor domain. Even the same feature's presentation
            // must go through its domain contract — that is what makes a
            // live-backend `data` swap a one-module change.
            presentation.doesNotDependOn(data)
            domain.doesNotDependOn(data)
        }
    }

    /**
     * Non-feature core/domain modules: `shared/<area>/domain` (or
     * `<area>/core`, which we use as the provider-agnostic boundary in
     * `shared/ai`, `shared/tts`, `shared/srs`) must not depend on any
     * `*.data.*` package — that is the boundary that keeps the wire DTO
     * out of the neutral type module. Feature `domain` modules are covered
     * by the broader rule below ("nothing outside feature data layer…").
     */
    @Test
    fun `shared core or domain modules must not depend on any data package`() {
        assertNoViolations(dataPackageImportViolations(KonsistTestSupport.sharedDomainSourceRegex))
    }

    /**
     * `data` is a sealed module: nothing outside the data layer of feature X
     * should reach into any `*.data.*` package — neither X's own
     * `presentation`/`domain` (they go through X's `domain` contracts),
     * nor any other feature, nor any shared/<area>/data. The only legal
     * consumers are composition roots (`shared/app-shell`, host apps),
     * which are not features and are not covered by this rule.
     *
     * Note: intra-module composition inside the same `feature/X/data` Gradle
     * module (e.g. `DefaultCatalogRepository` → `local.CatalogLocalDataSource`)
     * is normal Kotlin visibility and is allowed. Data-layer files are still
     * checked for imports of another feature's `data` package or any other
     * shared `*.data.*` package.
     */
    @Test
    fun `nothing outside feature data layer may depend on a data package`() {
        val violations =
            KonsistTestSupport.featureScope.files
                .filter { file ->
                    val path = file.normalizedProjectPath()
                    if (!path.isProductionSourcePath()) return@filter false
                    if (!KonsistTestSupport.featureSourceRegex.containsMatchIn(path)) return@filter false
                    true
                }.flatMap { file ->
                    val path = file.normalizedProjectPath()
                    val ownerFeature =
                        KonsistTestSupport.featureSourceRegex
                            .find(path)
                            ?.groupValues
                            ?.get(1)
                            ?.kebabToCamel()
                            ?: return@flatMap emptyList()
                    val isOwnerDataFile = KonsistTestSupport.featureDataSourceRegex.containsMatchIn(path)
                    file.imports.mapNotNull { importDeclaration ->
                        val imported = importDeclaration.importedPath()
                        val packageSegments = imported.split('.').dropLast(1)
                        if ("data" !in packageSegments) return@mapNotNull null
                        val isSameFeatureDataImport =
                            imported == "app.sensee.feature.$ownerFeature.data" ||
                                imported.startsWith("app.sensee.feature.$ownerFeature.data.")
                        if (isOwnerDataFile && isSameFeatureDataImport) return@mapNotNull null
                        violation(
                            subject = file.normalizedProjectPath(),
                            message = "depends on data package '$imported'",
                        )
                    }
                }

        assertNoViolations(violations)
    }

    private fun dataPackageImportViolations(scopeRegex: Regex): List<String> =
        KonsistTestSupport.sharedScope.files
            .filter { file ->
                val path = file.normalizedProjectPath()
                scopeRegex.containsMatchIn(path) && path.isProductionSourcePath()
            }.flatMap { file ->
                file.imports.mapNotNull { importDeclaration ->
                    val imported = importDeclaration.importedPath()
                    val packageSegments = imported.split('.').dropLast(1)
                    if ("data" in packageSegments) {
                        violation(
                            subject = file.normalizedProjectPath(),
                            message = "depends on data package '$imported'",
                        )
                    } else {
                        null
                    }
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
