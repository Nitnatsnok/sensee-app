package app.sensee.quality.konsist.structure

import app.sensee.quality.konsist.KonsistTestSupport
import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.kebabToCamel
import app.sensee.quality.konsist.matchesPackagePrefix
import app.sensee.quality.konsist.normalizedProjectPath
import app.sensee.quality.konsist.qualifiedDisplayName
import app.sensee.quality.konsist.violation
import com.lemonappdev.konsist.api.provider.KoDeclarationCastProvider
import org.junit.jupiter.api.Test

class FeatureStructureKonsistTest {
    @Test
    fun `feature presentation packages should match module path`() {
        val violations =
            KonsistTestSupport.featureScope.files.mapNotNull { file ->
                val path = file.normalizedProjectPath()
                val match =
                    KonsistTestSupport.featurePresentationSourceRegex.find(path)
                        ?: return@mapNotNull null
                val featureName = match.groupValues[1].kebabToCamel()
                val layer = match.groupValues[2].kebabToCamel()
                val expectedPrefix = "app.sensee.feature.$featureName.presentation.$layer"
                val actualPackage = file.packagee?.name

                if (actualPackage.matchesPackagePrefix(expectedPrefix)) {
                    null
                } else {
                    violation(
                        subject = path,
                        message = "expected package '$expectedPrefix', was '${actualPackage.orEmpty()}'",
                    )
                }
            }

        assertNoViolations(violations)
    }

    @Test
    fun `screen configs should stay in api navigation contracts`() {
        val violations =
            KonsistTestSupport.featureScope
                .classesAndInterfacesAndObjects(includeLocal = false)
                .filter { it.hasParentWithName("ScreenConfig") }
                .mapNotNull { declaration ->
                    val path = declaration.normalizedProjectPath()
                    if (path.contains("/presentation/api/") ||
                        path.contains("/presentation/navigation-api/")
                    ) {
                        null
                    } else {
                        violation(
                            subject = declaration.qualifiedDisplayName(),
                            message = "$path must stay in api navigation contracts",
                        )
                    }
                }

        assertNoViolations(violations)
    }

    @Test
    fun `feature components in api should be interface-based app contracts`() {
        val invalidComponentClasses =
            KonsistTestSupport.featureScope
                .classes(includeLocal = false)
                .filter { it.isTopLevel && it.hasNameEndingWith("Component") }
                .filter { it.normalizedProjectPath().contains("/presentation/api/") }
                .map { declaration ->
                    violation(
                        subject = declaration.qualifiedDisplayName(),
                        message = "presentation.api component contracts must be interfaces",
                    )
                }

        val invalidComponentObjects =
            KonsistTestSupport.featureScope
                .objects()
                .filter { it.isTopLevel && it.hasNameEndingWith("Component") }
                .filter { it.normalizedProjectPath().contains("/presentation/api/") }
                .map { declaration ->
                    violation(
                        subject = declaration.qualifiedDisplayName(),
                        message = "presentation.api component contracts must be interfaces",
                    )
                }

        val interfacesWithoutAppComponent =
            KonsistTestSupport.featureScope
                .interfaces(includeNested = false)
                .filter { it.hasNameEndingWith("Component") }
                .filter { it.normalizedProjectPath().contains("/presentation/api/") }
                .filterNot { it.hasParentWithName("AppComponent") }
                .map { declaration ->
                    violation(
                        subject = declaration.qualifiedDisplayName(),
                        message = "must extend AppComponent",
                    )
                }

        val invalidFactories = invalidFeatureComponentFactories()

        assertNoViolations(
            invalidComponentClasses +
                invalidComponentObjects +
                interfacesWithoutAppComponent +
                invalidFactories,
        )
    }
}

private fun invalidFeatureComponentFactories(): List<String> =
    KonsistTestSupport.featureScope
        .interfaces(includeNested = true)
        .asSequence()
        .filter { !it.isTopLevel && it.name == "Factory" }
        .filter { it.normalizedProjectPath().contains("/presentation/api/") }
        .filter { factory ->
            val parent = factory.containingDeclaration as? KoDeclarationCastProvider
            parent?.asInterfaceDeclaration()?.hasNameEndingWith("Component") == true
        }.filterNot { factory ->
            factory.hasFunction(includeNested = false, includeLocal = false) { function ->
                function.name == "create" &&
                    function.parameters.firstOrNull()?.representsType("AppComponentContext") == true
            }
        }.map { declaration ->
            violation(
                subject = declaration.qualifiedDisplayName(),
                message = "must define create(AppComponentContext, ...)",
            )
        }.toList()
