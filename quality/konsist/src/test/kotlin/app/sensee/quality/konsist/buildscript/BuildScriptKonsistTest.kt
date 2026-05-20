package app.sensee.quality.konsist.buildscript

import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.konsistProjectRoot
import app.sensee.quality.konsist.maskCommentsAndStringLiterals
import app.sensee.quality.konsist.violation
import org.junit.jupiter.api.Test

class BuildScriptKonsistTest {
    @Test
    fun `test-kit modules must only be referenced from test source sets`() {
        val projectRoot = konsistProjectRoot()
        val sharedRoot = projectRoot.resolve("shared")

        val violations =
            sharedRoot
                .walkTopDown()
                .filter { it.isFile && it.name == "build.gradle.kts" }
                .flatMap { buildFile ->
                    val relativePath = buildFile.relativeTo(projectRoot).path.replace('\\', '/')
                    findTestKitReferencesInMainScopes(buildFile.readText()).map { lineNumber ->
                        violation(
                            subject = "$relativePath:$lineNumber",
                            message = "test-kit module referenced from a non-test source set",
                        )
                    }
                }.toList()

        assertNoViolations(violations)
    }

    /**
     * Scans a Gradle Kotlin DSL file and returns the line numbers where a
     * `projects.shared.<...>.testKit` reference appears inside a source set
     * block whose name does not end with `Test`. For each `<name>Main {` /
     * `<name>Test {` opening, the matching closing brace is located via
     * brace counting and the enclosed region is searched. Comments and string
     * literals are masked first (length- and newline-preserving) so a `{`/`}`
     * or token inside a string or comment cannot unbalance the brace count or
     * cause a false match; offsets still map onto the original line numbers.
     */
    private fun findTestKitReferencesInMainScopes(text: String): List<Int> {
        val scan = text.maskCommentsAndStringLiterals()
        return SOURCE_SET_OPENING_REGEX
            .findAll(scan)
            .filterNot { it.groupValues[1].endsWith("Test") }
            .flatMap { match ->
                val openBracePos = match.range.last
                val closePos = matchingCloseBrace(scan, openBracePos)
                if (closePos == null) {
                    emptySequence()
                } else {
                    val region = scan.substring(openBracePos + 1, closePos)
                    TEST_KIT_REFERENCE_REGEX.findAll(region).map { refMatch ->
                        scan.lineNumberAt(openBracePos + 1 + refMatch.range.first)
                    }
                }
            }.toList()
    }

    private fun matchingCloseBrace(
        text: String,
        openBracePos: Int,
    ): Int? {
        var depth = 0
        for (i in openBracePos until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    private fun String.lineNumberAt(offset: Int): Int = substring(0, offset).count { it == '\n' } + 1

    private companion object {
        // Matches `commonMain {`, `androidTest {`, `iosMain {`, etc. — Gradle KMP source-set blocks.
        val SOURCE_SET_OPENING_REGEX =
            Regex("""\b([a-z][a-zA-Z0-9]*(?:Main|Test))\s*\{""")

        // Matches `projects.shared.<...>.testKit` references.
        val TEST_KIT_REFERENCE_REGEX =
            Regex("""\bprojects\.shared\.(?:\w+\.)+testKit\b""")
    }
}
