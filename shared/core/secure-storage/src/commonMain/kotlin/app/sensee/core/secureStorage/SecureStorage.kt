package app.sensee.core.secureStorage

/**
 * String-by-key store backed by the OS-native secret vault on each platform.
 * Used for user-supplied third-party credentials (AI/TTS API keys); the
 * settings repository persists everything else in the regular SQLite table.
 *
 * Reads return `null` for absent values — the AI/TTS seam treats that as
 * "integration not configured" and degrades to the offline fixture. Both
 * operations **throw** [SecureStorageException] when the platform vault itself
 * fails: hiding a crypto/OS error as "no key" would silently disable a paid
 * integration the user thought was working.
 */
public interface SecureStorage {
    public suspend fun read(key: SecureStorageKey): String?

    /** Pass `null` to delete the entry. */
    public suspend fun write(
        key: SecureStorageKey,
        value: String?,
    )
}

/**
 * Thrown when the platform secret vault itself fails (Keystore/Keychain/OS
 * vault error or, on web, an unrecoverable storage failure). A missing entry
 * is **not** an error — [SecureStorage.read] returns `null` for that.
 */
public class SecureStorageException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
