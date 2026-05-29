package app.sensee.core.secureStorage

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import app.sensee.core.coroutines.runCatchingCancellable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal const val ANDROID_SECURE_PREFERENCES_FILE: String = "app.sensee.secure_storage"

private const val ANDROID_KEYSTORE_NAME: String = "AndroidKeyStore"
private const val MASTER_KEY_ALIAS: String = "app.sensee.secure_storage.master"
private const val AES_TRANSFORMATION: String = "AES/GCM/NoPadding"
private const val AES_GCM_IV_LENGTH_BYTES: Int = 12
private const val AES_GCM_TAG_LENGTH_BITS: Int = 128
private const val AES_KEY_SIZE_BITS: Int = 256

/**
 * Android implementation: a 256-bit AES key lives in the Android Keystore
 * (non-extractable; the OS guards it inside the TEE or StrongBox where
 * available), per-value envelopes are AES-256-GCM with a provider-generated
 * IV and Base64-stored in a private `SharedPreferences` file. The app
 * module's backup rules also exclude this file outright, so even cloud backup
 * uploads only an unreadable blob.
 *
 * Hand-rolled rather than via Jetpack Security's `EncryptedSharedPreferences`
 * because `androidx.security:security-crypto` is formally `@Deprecated`; the
 * primitives it composed (Keystore master key + AES/GCM) are stable and
 * directly available from the platform.
 */
internal class AndroidSecureStorage(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : SecureStorage {
    private val appContext = context.applicationContext
    private val writeMutex = Mutex()

    private val preferences: SharedPreferences by lazy {
        appContext.getSharedPreferences(ANDROID_SECURE_PREFERENCES_FILE, Context.MODE_PRIVATE)
    }

    private val masterKey: SecretKey by lazy { getOrCreateMasterKey() }

    override suspend fun read(key: SecureStorageKey): String? =
        withContext(ioDispatcher) {
            val ciphertext = preferences.getString(key.value, null) ?: return@withContext null
            runCatchingCancellable {
                decrypt(ciphertext)
            }.getOrElse { throwable ->
                // A decrypt failure typically means the Keystore master key
                // was wiped (factory reset, app-data clear, TEE reset) while
                // the SharedPreferences blob survived. Don't silently drop —
                // the user thought the integration was configured.
                throw SecureStorageException(
                    "Failed to decrypt ${key.value}; secure store may have been reset",
                    throwable,
                )
            }
        }

    override suspend fun write(
        key: SecureStorageKey,
        value: String?,
    ) {
        withContext(ioDispatcher) {
            writeMutex.withLock {
                val editor = preferences.edit()
                if (value == null) editor.remove(key.value) else editor.putString(key.value, encrypt(value))
                if (!editor.commit()) {
                    throw SecureStorageException("Failed to persist ${key.value} into secure storage")
                }
            }
        }
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv: ByteArray = cipher.iv
        val cipherBytes = cipher.doFinal(plaintext.encodeToByteArray())
        val out = ByteArray(iv.size + cipherBytes.size)
        iv.copyInto(out, destinationOffset = 0)
        cipherBytes.copyInto(out, destinationOffset = iv.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decrypt(payload: String): String {
        val raw = Base64.decode(payload, Base64.NO_WRAP)
        require(raw.size > AES_GCM_IV_LENGTH_BYTES) { "secure payload truncated" }
        val iv = raw.copyOfRange(0, AES_GCM_IV_LENGTH_BYTES)
        val cipherBytes = raw.copyOfRange(AES_GCM_IV_LENGTH_BYTES, raw.size)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(AES_GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(cipherBytes).decodeToString()
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val store = loadAndroidKeyStore()
        store.getKey(MASTER_KEY_ALIAS, null)?.let { return it as SecretKey }
        return createMasterKey()
    }

    private fun loadAndroidKeyStore(): KeyStore =
        runCatchingCancellable {
            KeyStore.getInstance(ANDROID_KEYSTORE_NAME).apply { load(null) }
        }.getOrElse { throwable ->
            throw SecureStorageException("AndroidKeyStore unavailable", throwable)
        }

    private fun createMasterKey(): SecretKey {
        // StrongBox (API 28+) keeps the key in a dedicated hardware chip
        // isolated from the main TEE, harder to attack with side-channel
        // techniques. Not every device ships it: a `StrongBoxUnavailableException`
        // means we fall through to the regular Keystore-backed AES key.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            generateStrongBoxMasterKeyOrNull()?.let { return it }
        }
        return generateRegularMasterKey()
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun generateStrongBoxMasterKeyOrNull(): SecretKey? =
        runCatchingCancellable {
            generateMasterKey(strongBox = true)
        }.getOrElse { throwable ->
            when (throwable) {
                // Fall back to a regular Keystore-backed AES key below.
                is StrongBoxUnavailableException -> null
                else -> throw SecureStorageException("Failed to generate Keystore master key", throwable)
            }
        }

    private fun generateRegularMasterKey(): SecretKey =
        runCatchingCancellable {
            generateMasterKey(strongBox = false)
        }.getOrElse { throwable ->
            throw SecureStorageException("Failed to generate Keystore master key", throwable)
        }

    private fun generateMasterKey(strongBox: Boolean): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_NAME)
        val spec =
            KeyGenParameterSpec
                .Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .apply {
                    if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        setIsStrongBoxBacked(true)
                    }
                }.build()
        generator.init(spec)
        return generator.generateKey()
    }
}
