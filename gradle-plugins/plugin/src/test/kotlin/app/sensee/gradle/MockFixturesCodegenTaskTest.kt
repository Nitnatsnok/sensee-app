package app.sensee.gradle

import org.gradle.api.GradleException
import org.gradle.kotlin.dsl.register
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MockFixturesCodegenTaskTest {
    @Test
    fun `generated source carries the input JSON byte-for-byte as an escaped string`() {
        val tempDir = createTempProject()
        val fixturesDir = File(tempDir, "fixtures").apply { mkdirs() }
        val nested = File(fixturesDir, "topic").apply { mkdirs() }
        val jsonBytes = "{\r\n  \"topics\": [{\"id\":\"a\",\"display\":\"A\"}]\r\n}"
        File(nested, "topics.json").writeText(jsonBytes)

        runGenerator(tempDir = tempDir, fixturesDir = fixturesDir).generate()

        val generated = readGenerated(tempDir)
        assertTrue("topic/topics" in generated.fixturesMap, "key is the path minus .json")
        assertEquals(jsonBytes, generated.fixturesMap.getValue("topic/topics"))
    }

    @Test
    fun `non-JSON content surfaces a clean error pointing at the offending file`() {
        val tempDir = createTempProject()
        val fixturesDir = File(tempDir, "fixtures").apply { mkdirs() }
        File(fixturesDir, "broken.json").writeText("definitely not json")

        val error =
            assertFailsWith<GradleException> {
                runGenerator(tempDir = tempDir, fixturesDir = fixturesDir).generate()
            }
        assertTrue(
            "broken.json" in error.message.orEmpty(),
            "the error message names the bad file, got: ${error.message}",
        )
        assertTrue(
            "invalid JSON" in error.message.orEmpty(),
            "JSON parse rejects non-JSON bytes before codegen, got: ${error.message}",
        )
    }

    @Test
    fun `a fixture with a literal dollar-brace is escaped before codegen`() {
        val tempDir = createTempProject()
        val fixturesDir = File(tempDir, "fixtures").apply { mkdirs() }
        // Build the literal `${user}` byte sequence without letting Kotlin
        // interpolate it on the way to disk.
        val dollar = "$"
        val payload = "{" + "\"template\": \"hello " + dollar + "{user}\"" + "}"
        File(fixturesDir, "templated.json").writeText(payload)

        runGenerator(tempDir = tempDir, fixturesDir = fixturesDir).generate()

        val generated = readGenerated(tempDir)
        assertEquals(payload, generated.fixturesMap.getValue("templated"))
    }

    @Test
    fun `an empty fixtures directory surfaces a clean error`() {
        val tempDir = createTempProject()
        val fixturesDir = File(tempDir, "fixtures").apply { mkdirs() }

        val error =
            assertFailsWith<GradleException> {
                runGenerator(tempDir = tempDir, fixturesDir = fixturesDir).generate()
            }
        assertTrue(
            "no .json files" in error.message.orEmpty(),
            "the error explains the empty directory, got: ${error.message}",
        )
    }

    @Test
    fun `non-ASCII content survives the codegen round trip`() {
        val tempDir = createTempProject()
        val fixturesDir = File(tempDir, "fixtures").apply { mkdirs() }
        // Cyrillic, an em-dash, and an emoji together exercise the >0x20 branch.
        val payload = "{\"ru\":\"Существительное — общее 🎯\"}"
        File(fixturesDir, "i18n.json").writeText(payload, Charsets.UTF_8)

        runGenerator(tempDir = tempDir, fixturesDir = fixturesDir).generate()

        val generated = readGenerated(tempDir)
        assertEquals(payload, generated.fixturesMap.getValue("i18n"))
    }

    @Test
    fun `a fixture with a bare dollar-identifier and triple quotes is escaped before codegen`() {
        val tempDir = createTempProject()
        val fixturesDir = File(tempDir, "fixtures").apply { mkdirs() }
        val dollar = "$"
        val payload = "{" + "\"v\": \"see " + dollar + "name and \\\"\\\"\\\"\"" + "}"
        File(fixturesDir, "bareDollar.json").writeText(payload)

        runGenerator(tempDir = tempDir, fixturesDir = fixturesDir).generate()

        val generated = readGenerated(tempDir)
        assertEquals(payload, generated.fixturesMap.getValue("bareDollar"))
    }

    private fun createTempProject(): File =
        File.createTempFile("mock-fixtures-codegen-", "").apply {
            delete()
            mkdirs()
        }

    private fun runGenerator(
        tempDir: File,
        fixturesDir: File,
    ): MockFixturesCodegenTask {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register<MockFixturesCodegenTask>("generate").get()
        task.fixturesDir.set(fixturesDir)
        task.outputDir.set(File(tempDir, "out"))
        task.packageName.set("test.pkg")
        task.className.set("TestFixtures")
        return task
    }

    private data class GeneratedSource(
        val raw: String,
        val fixturesMap: Map<String, String>,
    )

    private fun readGenerated(tempDir: File): GeneratedSource {
        val out = File(tempDir, "out/test/pkg/TestFixtures.kt")
        assertTrue(out.exists(), "expected generated file at $out")
        val text = out.readText()
        // Parse each `"key" to "escaped body"` entry — the same shape the codegen emits.
        val entryRegex = Regex("\"([^\"]+)\"\\s+to\\s+\"((?:\\\\.|[^\"\\\\])*)\"")
        val map =
            entryRegex.findAll(text).associate {
                it.groupValues[1] to it.groupValues[2].unescapeKotlinString()
            }
        return GeneratedSource(text, map)
    }

    private fun String.unescapeKotlinString(): String {
        val out = StringBuilder(length)
        var index = 0
        while (index < length) {
            val char = this[index]
            if (char != '\\') {
                out.append(char)
                index++
                continue
            }
            val escaped = this.getOrNull(index + 1) ?: error("dangling escape")
            when (escaped) {
                '\\' -> out.append('\\')
                '"' -> out.append('"')
                '$' -> out.append('$')
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                'b' -> out.append('\b')
                'u' -> {
                    val hex = substring(index + 2, index + 6)
                    out.append(hex.toInt(radix = 16).toChar())
                    index += 4
                }
                else -> error("unknown escape: \\$escaped")
            }
            index += 2
        }
        return out.toString()
    }
}
