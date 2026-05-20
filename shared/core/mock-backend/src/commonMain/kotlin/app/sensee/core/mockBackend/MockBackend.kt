package app.sensee.core.mockBackend

import dev.zacsweers.metro.Inject
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

@Inject
public class MockBackend(
    private val fixtureReader: FixtureReader,
) {
    public fun mockEngine(): MockEngine =
        MockEngine { request ->
            val path = request.url.encodedPath.toFixturePath()
            val fixture = fixtureReader.readText(path)

            if (fixture == null) {
                respond(
                    content = """{"error":"fixture_not_found","path":"$path"}""",
                    status = HttpStatusCode.NotFound,
                    headers =
                        headersOf(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString(),
                        ),
                )
            } else {
                respond(
                    content = fixture,
                    status = HttpStatusCode.OK,
                    headers =
                        headersOf(
                            HttpHeaders.ContentType,
                            ContentType.Application.Json.toString(),
                        ),
                )
            }
        }
}
