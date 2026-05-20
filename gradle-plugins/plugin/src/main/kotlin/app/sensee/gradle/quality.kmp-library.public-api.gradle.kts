package app.sensee.gradle

import app.sensee.gradle.dsl.kotlinMultiplatform
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

// ABI dump validation only. Explicit-API enforcement is owned by the kmp-library
// plugin (explicitApiWarning, promotable to explicitApi); this stays an opt-in,
// separately-applied gate for modules that want a tracked binary API surface.
plugins.withId("org.jetbrains.kotlin.multiplatform") {
    kotlinMultiplatform {
        @OptIn(ExperimentalAbiValidation::class)
        extensions.configure<AbiValidationMultiplatformExtension> {
            enabled.set(true)

            filters {
                exclude {
                    byNames.add("**.internal.**")
                    byNames.add("**.*Internal*")
                }
            }

            klib {
                keepUnsupportedTargets.set(true)
            }
        }
    }
}
