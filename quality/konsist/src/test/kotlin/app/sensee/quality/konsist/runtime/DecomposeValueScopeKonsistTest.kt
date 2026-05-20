package app.sensee.quality.konsist.runtime

import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.importedPath
import app.sensee.quality.konsist.normalizedProjectPath
import app.sensee.quality.konsist.productionScopeFiles
import app.sensee.quality.konsist.strippedText
import app.sensee.quality.konsist.violation
import org.junit.jupiter.api.Test

/**
 * Decompose `Value`/`MutableValue` is reserved for router navigation
 * (`ChildStack`/`ChildPanels`); every other observable UI state belongs on a
 * `StateFlow`. Scoped to files that actually import the Decompose value
 * package, excluding the `shared/core/decompose` seam that owns the generic
 * `Value` plumbing (`asFlow`, child-stack/panels builders).
 *
 * The import scoping uses the declaration API; the type-argument shape check
 * uses the docs' sanctioned "last resort" `text` scan, since `Value<…>` here
 * appears in many positions (properties, returns, params) Konsist would need
 * several queries to cover uniformly.
 */
class DecomposeValueScopeKonsistTest {
    @Test
    fun `Decompose Value is navigation-only, other state uses StateFlow`() {
        val violations =
            productionScopeFiles()
                .filterNot { it.normalizedProjectPath().contains(DECOMPOSE_SEAM) }
                .filter { file -> file.imports.any { it.importedPath().startsWith(DECOMPOSE_VALUE_PKG) } }
                .flatMap { file ->
                    val code = file.strippedText()
                    buildList {
                        if (MUTABLE_VALUE.containsMatchIn(code)) {
                            add("MutableValue")
                        }
                        VALUE_GENERIC
                            .findAll(code)
                            .map { it.groupValues[1] }
                            .firstOrNull { arg -> NAV_TYPES.none { it in arg } }
                            ?.let { add("Value<$it>") }
                    }.map { offender ->
                        violation(
                            subject = file.normalizedProjectPath(),
                            message = "$offender is non-navigation Decompose state; model it as a StateFlow instead",
                        )
                    }
                }

        assertNoViolations(violations)
    }

    private companion object {
        const val DECOMPOSE_SEAM = "shared/core/decompose/"
        const val DECOMPOSE_VALUE_PKG = "com.arkivanov.decompose.value"

        val NAV_TYPES = listOf("ChildStack", "ChildPanels")

        val MUTABLE_VALUE = Regex("""\bMutableValue\b""")

        // First type token inside `Value< … >`; `\b` excludes `MutableValue<`.
        val VALUE_GENERIC = Regex("""\bValue<\s*([\w.]+)""")
    }
}
