package app.sensee.quality.konsist.hygiene

import app.sensee.quality.konsist.assertNoViolations
import app.sensee.quality.konsist.konsistProjectRoot
import app.sensee.quality.konsist.violation
import org.junit.jupiter.api.Test
import java.io.File

class AgentInstructionHygieneKonsistTest {
    @Test
    fun `agent instruction files should have Claude shims`() {
        val projectRoot = konsistProjectRoot()
        val violations =
            projectRoot
                .walkTopDown()
                .onEnter { dir -> dir.name !in ExcludedDirectories }
                .filter { file -> file.isFile && file.name == "AGENTS.md" }
                .flatMap { agentsFile ->
                    val claudeFile = agentsFile.resolveSibling("CLAUDE.md")
                    when {
                        !claudeFile.isFile ->
                            listOf(
                                violation(
                                    subject = agentsFile.relativizedPath(projectRoot),
                                    message = "missing sibling CLAUDE.md shim",
                                ),
                            )
                        claudeFile.readText().trimEnd() != "@AGENTS.md" ->
                            listOf(
                                violation(
                                    subject = claudeFile.relativizedPath(projectRoot),
                                    message = "CLAUDE.md must contain exactly @AGENTS.md",
                                ),
                            )
                        else -> emptyList()
                    }
                }.toList()

        assertNoViolations(violations)
    }

    private fun File.relativizedPath(base: File): String = relativeTo(base).path.replace('\\', '/')

    private companion object {
        val ExcludedDirectories = setOf("build", ".gradle", ".kotlin", "kotlin-js-store")
    }
}
