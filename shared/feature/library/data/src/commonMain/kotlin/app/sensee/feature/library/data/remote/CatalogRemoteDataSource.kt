package app.sensee.feature.library.data.remote

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

@SingleIn(AppScope::class)
@Inject
public class CatalogRemoteDataSource(
    private val httpClient: HttpClient,
) {
    public suspend fun listDecks(): DeckListDto = httpClient.get("practice/decks").body()

    public suspend fun getDeck(deckId: String): DeckDto = httpClient.get("practice/decks/$deckId").body()

    public suspend fun getLemma(lemmaId: String): LemmaDto = httpClient.get("practice/lemmas/$lemmaId").body()
}
