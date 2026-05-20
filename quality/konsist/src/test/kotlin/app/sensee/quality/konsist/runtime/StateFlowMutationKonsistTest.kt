package app.sensee.quality.konsist.runtime

import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.normalizedProjectPath
import app.sensee.quality.konsist.productionScopeFiles
import app.sensee.quality.konsist.strippedText
import app.sensee.quality.konsist.violation
import org.junit.jupiter.api.Test

/**
 * `MutableStateFlow` must be written through `.update { }`, never via a direct
 * `.value =` assignment (settled project habit, even for plain sets: `.value =`
 * is read-modify-write-racy and bypasses the single mutation seam).
 *
 * Precision: the offending `.value` is resolved against properties actually
 * typed/initialised as `MutableStateFlow` (Konsist property API), so a Compose
 * `MutableState.value = ` write in the same file is not mistaken for it. Only
 * the *assignment expression* itself falls back to the docs' sanctioned
 * "last resort" `text` scan — Konsist cannot query call/assignment sites.
 */
class StateFlowMutationKonsistTest {
    @Test
    fun `MutableStateFlow must be mutated via update, not direct value assignment`() {
        val violations =
            productionScopeFiles()
                .flatMap { file ->
                    val backingNames =
                        file
                            .properties(includeNested = true)
                            .filter { it.type?.name == STATE_FLOW || STATE_FLOW_CTOR in it.text }
                            .map { it.name }
                            .distinct()
                    if (backingNames.isEmpty()) {
                        emptyList()
                    } else {
                        val code = file.strippedText()
                        backingNames
                            .filter { name -> assignmentRegex(name).containsMatchIn(code) }
                            .map { name ->
                                violation(
                                    subject = file.normalizedProjectPath(),
                                    message =
                                        "'$name.value = ...' mutates a MutableStateFlow " +
                                            "directly; use $name.update { }",
                                )
                            }
                    }
                }

        assertNoViolations(violations)
    }

    // `=(?!=)` so `==` comparisons are not treated as assignment.
    private fun assignmentRegex(name: String): Regex =
        Regex("""\b${Regex.escape(name)}\.value\s*(?:\+=|-=|\*=|/=|=(?!=))""")

    private companion object {
        const val STATE_FLOW = "MutableStateFlow"
        const val STATE_FLOW_CTOR = "MutableStateFlow("
    }
}
