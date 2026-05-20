package app.sensee.core.mockBackend

public fun interface FixtureReader {
    public suspend fun readText(path: String): String?
}
