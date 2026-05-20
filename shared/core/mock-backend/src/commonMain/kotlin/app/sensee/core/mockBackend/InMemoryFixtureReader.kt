package app.sensee.core.mockBackend

public class InMemoryFixtureReader(
    fixtures: Map<String, String> = emptyMap(),
) : FixtureReader {
    private val fixturesByPath: Map<String, String> =
        fixtures.mapKeys { (path, _) -> path.toFixturePath() }

    override suspend fun readText(path: String): String? = fixturesByPath[path.toFixturePath()]
}

internal fun String.toFixturePath(): String = trim().trimStart('/')
