package app.sensee.core.mockBackend

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<FixtureReader>(),
)
@Inject
public class MergedFixtureReader(
    fixtureSets: Set<MockFixtureSet>,
) : FixtureReader {
    private val reader =
        InMemoryFixtureReader(
            buildMap {
                fixtureSets
                    .flatMap { fixtureSet -> fixtureSet.fixtures.entries }
                    .forEach { (path, body) ->
                        require(path !in this) { "Duplicate mock fixture path: $path" }
                        put(path, body)
                    }
            },
        )

    override suspend fun readText(path: String): String? = reader.readText(path)
}
