package app.sensee.core.secureStorage

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.platform.PlatformEnvironment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val DEFAULT_WEB_PREFIX: String = "app.sensee.secret/"

internal actual fun platformSecureStorage(
    platformEnvironment: PlatformEnvironment,
    appDispatchers: AppDispatchers,
): SecureStorage =
    WebSecureStorage(
        prefix = DEFAULT_WEB_PREFIX,
        ioDispatcher = appDispatchers.io,
    )

/**
 * Browser implementation backed by `window.localStorage`. The browser security
 * model gives any same-origin script full read access to localStorage, so this
 * is **best-effort**: it removes the key from the SQLite settings table (which
 * still lives in IndexedDB) but cannot guard against a same-origin XSS. The
 * web target is the reviewer demo; production-grade secret storage on the web
 * needs a server-mediated session or a browser extension API.
 */
internal class WebSecureStorage(
    private val prefix: String,
    private val ioDispatcher: CoroutineDispatcher,
) : SecureStorage {
    override suspend fun read(key: SecureStorageKey): String? =
        withContext(ioDispatcher) {
            try {
                webLocalStorageGet(prefix + key.value)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                throw SecureStorageException("Failed to read ${key.value} from localStorage", throwable)
            }
        }

    override suspend fun write(
        key: SecureStorageKey,
        value: String?,
    ) {
        withContext(ioDispatcher) {
            try {
                val storageKey = prefix + key.value
                if (value == null) webLocalStorageRemove(storageKey) else webLocalStorageSet(storageKey, value)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                throw SecureStorageException("Failed to write ${key.value} to localStorage", throwable)
            }
        }
    }
}

internal expect fun webLocalStorageGet(key: String): String?

internal expect fun webLocalStorageSet(
    key: String,
    value: String,
): Unit

internal expect fun webLocalStorageRemove(key: String): Unit
