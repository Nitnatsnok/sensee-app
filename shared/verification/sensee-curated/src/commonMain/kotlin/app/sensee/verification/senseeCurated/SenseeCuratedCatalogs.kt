package app.sensee.verification.senseeCurated

import app.sensee.core.coroutines.runCatchingCancellable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

/**
 * Fetches the Sensee-curated reference catalogs from the backend once per
 * process and caches them in memory — they are immutable reference data. A
 * fetch/parse failure returns `null` (the adapter degrades to `Unavailable`)
 * and is not cached, so a later lookup retries.
 */
@SingleIn(AppScope::class)
@Inject
public class SenseeCuratedCatalogs(
    private val httpClient: HttpClient,
) {
    private val mutex = Mutex()

    @Volatile
    private var frequency: FrequencyCatalogDto? = null

    @Volatile
    private var cefr: CefrCatalogDto? = null

    @Volatile
    private var senses: SenseCatalogDto? = null

    @Volatile
    private var family: FamilyCatalogDto? = null

    internal suspend fun frequency(): FrequencyCatalogDto? {
        frequency?.let { return it }
        return mutex.withLock {
            frequency ?: fetch<FrequencyCatalogDto>(PATH_FREQUENCY)?.also { frequency = it }
        }
    }

    internal suspend fun cefr(): CefrCatalogDto? {
        cefr?.let { return it }
        return mutex.withLock {
            cefr ?: fetch<CefrCatalogDto>(PATH_CEFR)?.also { cefr = it }
        }
    }

    internal suspend fun senses(): SenseCatalogDto? {
        senses?.let { return it }
        return mutex.withLock {
            senses ?: fetch<SenseCatalogDto>(PATH_SENSES)?.also { senses = it }
        }
    }

    internal suspend fun family(): FamilyCatalogDto? {
        family?.let { return it }
        return mutex.withLock {
            family ?: fetch<FamilyCatalogDto>(PATH_FAMILY)?.also { family = it }
        }
    }

    private suspend inline fun <reified T> fetch(path: String): T? =
        runCatchingCancellable {
            httpClient.get(path).body<T>()
        }.getOrElse {
            null
        }

    private companion object {
        const val PATH_FREQUENCY: String = "verification/frequency"
        const val PATH_CEFR: String = "verification/cefr"
        const val PATH_SENSES: String = "verification/senses"
        const val PATH_FAMILY: String = "verification/family"
    }
}
