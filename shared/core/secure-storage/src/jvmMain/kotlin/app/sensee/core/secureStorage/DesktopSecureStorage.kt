package app.sensee.core.secureStorage

import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Desktop JVM implementation backed by `java-keyring`, which delegates to the
 * OS-native vault:
 * - Windows: Credential Vault (DPAPI).
 * - macOS: Keychain (`security` framework).
 * - Linux: libsecret (gnome-keyring, KWallet via Secret Service API).
 *
 * If no OS backend is available — typically a Linux box without libsecret —
 * `Keyring.create()` throws on first use. We fall back to a process-local
 * in-memory store: the user re-enters their key each launch, but the AI/TTS
 * features still work in that session. This is a deliberate UX choice; persisting
 * a plaintext file would be worse than asking the user to set up libsecret.
 */
internal class DesktopSecureStorage(
    private val service: String,
    private val ioDispatcher: CoroutineDispatcher,
) : SecureStorage {
    private val accessMutex = Mutex()
    private val backend: KeyringBackend by lazy { resolveBackend() }

    override suspend fun read(key: SecureStorageKey): String? =
        withContext(ioDispatcher) {
            accessMutex.withLock { backend.read(key.value) }
        }

    override suspend fun write(
        key: SecureStorageKey,
        value: String?,
    ) {
        withContext(ioDispatcher) {
            accessMutex.withLock {
                if (value == null) backend.delete(key.value) else backend.write(key.value, value)
            }
        }
    }

    private fun resolveBackend(): KeyringBackend =
        try {
            OsKeyringBackend(keyring = Keyring.create(), service = service)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            InMemoryKeyringBackend(reason = throwable.message ?: throwable::class.simpleName.orEmpty())
        }

    private interface KeyringBackend {
        fun read(account: String): String?

        fun write(
            account: String,
            value: String,
        )

        fun delete(account: String)
    }

    private class OsKeyringBackend(
        private val keyring: Keyring,
        private val service: String,
    ) : KeyringBackend {
        override fun read(account: String): String? =
            try {
                keyring.getPassword(service, account)
            } catch (notFound: PasswordAccessException) {
                if (notFound.isMissingEntry()) null else throw notFound.asSecureStorageException(account)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                throw SecureStorageException("OS keyring read for $account failed", throwable)
            }

        override fun write(
            account: String,
            value: String,
        ) {
            try {
                keyring.setPassword(service, account, value)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                throw SecureStorageException("OS keyring write for $account failed", throwable)
            }
        }

        override fun delete(account: String) {
            try {
                keyring.deletePassword(service, account)
            } catch (notFound: PasswordAccessException) {
                if (!notFound.isMissingEntry()) throw notFound.asSecureStorageException(account)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                throw SecureStorageException("OS keyring delete for $account failed", throwable)
            }
        }

        // java-keyring conflates "no such entry" with other access errors into one
        // exception type — sniff the message so a missing-entry read returns null
        // instead of surfacing as a crypto error.
        private fun PasswordAccessException.isMissingEntry(): Boolean {
            val text = message?.lowercase().orEmpty()
            return "not found" in text || "no such" in text || "no password" in text
        }

        private fun PasswordAccessException.asSecureStorageException(account: String): SecureStorageException =
            SecureStorageException("OS keyring access for $account failed", this)
    }

    private class InMemoryKeyringBackend(
        private val reason: String,
    ) : KeyringBackend {
        private val store: MutableMap<String, String> = mutableMapOf()

        override fun read(account: String): String? = store[account]

        override fun write(
            account: String,
            value: String,
        ) {
            store[account] = value
        }

        override fun delete(account: String) {
            store.remove(account)
        }

        // Recorded so it surfaces in heap dumps / debugger inspection when a
        // dev wonders why secrets are not surviving an app restart.
        @Suppress("unused")
        val backendUnavailableReason: String = reason
    }
}
