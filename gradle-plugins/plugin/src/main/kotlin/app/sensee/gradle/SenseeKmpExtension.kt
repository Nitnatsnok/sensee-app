package app.sensee.gradle

import app.sensee.gradle.dsl.createNestedExtension
import app.sensee.gradle.dsl.whenPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import javax.inject.Inject

/**
 * Shared module DSL applied by `kmp-library`.
 *
 * Use [iosTargets] to add binaries, exports, or compiler options without
 * re-typing the
 * `targets.withType<KotlinNativeTarget>().matching { ... }` filter.
 *
 * Example:
 * ```
 * sensee {
 *     iosTargets.configureEach {
 *         binaries.framework {
 *             export(projects.shared.someOtherModule)
 *         }
 *     }
 * }
 * ```
 */
abstract class SenseeKmpExtension
    @Inject
    constructor(
        private val project: Project,
    ) {
        /** Live filtered view of iOS-family Kotlin Native targets. */
        val iosTargets get() =
            project.extensions
                .getByType<KotlinMultiplatformExtension>()
                .targets
                .withType<KotlinNativeTarget>()
                .matching { it.konanTarget.family == Family.IOS }
    }

internal fun Project.createSenseeKmpExtension(): SenseeKmpExtension =
    createNestedExtension(
        parent = extensions.getByType<KotlinMultiplatformExtension>(),
        name = "sensee",
        constructionArguments = arrayOf(this),
    )

internal fun Project.applyIosTargetsDefaults(extension: SenseeKmpExtension) {
    val frameworkBaseName = path.removePrefix(":") + "Kit"
    whenPlugin(id = "org.jetbrains.kotlin.multiplatform") {
        extension.iosTargets.configureEach {
            binaries.framework {
                baseName = frameworkBaseName
            }
        }
    }
}
