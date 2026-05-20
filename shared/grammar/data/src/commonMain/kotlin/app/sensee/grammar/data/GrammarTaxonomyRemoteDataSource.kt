package app.sensee.grammar.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/** Provider-agnostic seam over the taxonomy endpoint so consumers stay fakeable. */
public interface GrammarTaxonomySource {
    public suspend fun getGrammarTaxonomy(): GrammarTaxonomyDto
}

/**
 * Reads the single `practice/grammar/taxonomy` endpoint. The fixture is served
 * by [GrammarTaxonomyMockFixtures]; a real backend would answer the same path.
 */
@SingleIn(AppScope::class)
@ContributesBinding(scope = AppScope::class, binding = binding<GrammarTaxonomySource>())
@Inject
public class GrammarTaxonomyRemoteDataSource(
    private val httpClient: HttpClient,
) : GrammarTaxonomySource {
    override suspend fun getGrammarTaxonomy(): GrammarTaxonomyDto = httpClient.get("practice/grammar/taxonomy").body()
}
