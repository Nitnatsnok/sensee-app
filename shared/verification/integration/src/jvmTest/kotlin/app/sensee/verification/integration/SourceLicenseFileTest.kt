package app.sensee.verification.integration

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.fail

class SourceLicenseFileTest {
    @Test
    fun `every catalog license file reference points at a checked-in notice`() {
        val root = repositoryRoot()
        val missing =
            providers()
                .provideKnownLexicalSources()
                .mapNotNull { source ->
                    val licenseFile = source.attribution.licenseFile ?: return@mapNotNull null
                    val path = root.resolve("shared").resolve("verification").resolve(licenseFile)
                    if (Files.exists(path)) null else "${source.id}: $licenseFile"
                }

        if (missing.isNotEmpty()) {
            fail("Missing lexical source license files: $missing")
        }
    }

    private fun providers(): VerificationIntegrationProviders = object : VerificationIntegrationProviders {}

    private fun repositoryRoot(): Path {
        var current: Path? = Path.of("").toAbsolutePath()
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))) return current
            current = current.parent
        }
        fail("Could not locate repository root from ${Path.of("").toAbsolutePath()}")
    }
}
