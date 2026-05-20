@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package app.sensee.core.secureStorage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecAttrSynchronizable
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS Keychain implementation. One [kSecClassGenericPassword] item per key,
 * scoped by a per-app [service] string and the [SecureStorageKey] as the
 * account. Items are written with
 * [kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly] so they decrypt only on
 * this device and never roam via iCloud Keychain or device-to-device transfer.
 *
 * Built against CoreFoundation rather than NSMutableDictionary because the
 * `kSec*` constants are `CFStringRef`s; the CF dictionary API takes them
 * directly without bridging gymnastics, and Keychain APIs accept
 * `CFDictionaryRef` natively.
 */
internal class IosKeychainSecureStorage(
    private val service: String,
    private val ioDispatcher: CoroutineDispatcher,
) : SecureStorage {
    override suspend fun read(key: SecureStorageKey): String? = withContext(ioDispatcher) { readSync(key.value) }

    override suspend fun write(
        key: SecureStorageKey,
        value: String?,
    ) {
        withContext(ioDispatcher) {
            if (value == null) deleteSync(key.value) else writeSync(key.value, value)
        }
    }

    private fun readSync(account: String): String? =
        memScoped {
            val query = baseQuery(account)
            try {
                CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
                CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
                val result = alloc<CFTypeRefVar>()
                when (val status = SecItemCopyMatching(query, result.ptr)) {
                    errSecSuccess -> {
                        val raw = result.value ?: return@memScoped null
                        try {
                            cfDataToString(raw.reinterpret())
                        } finally {
                            CFRelease(raw)
                        }
                    }
                    errSecItemNotFound -> null
                    else ->
                        throw SecureStorageException(
                            "Keychain read for $account failed (OSStatus $status)",
                        )
                }
            } finally {
                CFRelease(query)
            }
        }

    private fun writeSync(
        account: String,
        value: String,
    ) {
        memScoped {
            val plainBytes = value.encodeToByteArray()
            val cfData =
                plainBytes
                    .usePinned { pinned ->
                        CFDataCreate(
                            allocator = null,
                            bytes = pinned.addressOf(0).reinterpret(),
                            length = plainBytes.size.toLong(),
                        )
                    } ?: throw SecureStorageException("CFDataCreate failed for $account")
            val query = baseQuery(account)
            val updateAttrs =
                CFDictionaryCreateMutable(
                    allocator = null,
                    capacity = 0,
                    keyCallBacks = kCFTypeDictionaryKeyCallBacks.ptr,
                    valueCallBacks = kCFTypeDictionaryValueCallBacks.ptr,
                ) ?: run {
                    CFRelease(cfData)
                    CFRelease(query)
                    throw SecureStorageException("CFDictionaryCreateMutable failed")
                }
            try {
                CFDictionaryAddValue(updateAttrs, kSecValueData, cfData)
                when (val status = SecItemUpdate(query, updateAttrs)) {
                    errSecSuccess -> Unit
                    errSecItemNotFound -> insertSync(account, cfData)
                    else ->
                        throw SecureStorageException(
                            "Keychain update for $account failed (OSStatus $status)",
                        )
                }
            } finally {
                CFRelease(updateAttrs)
                CFRelease(query)
                CFRelease(cfData)
            }
        }
    }

    private fun MemScope.insertSync(
        account: String,
        cfData: CFTypeRef,
    ) {
        val attrs = baseQuery(account)
        try {
            CFDictionaryAddValue(attrs, kSecValueData, cfData)
            CFDictionaryAddValue(
                attrs,
                kSecAttrAccessible,
                kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            )
            val status = SecItemAdd(attrs, null)
            if (status != errSecSuccess) {
                throw SecureStorageException("Keychain add for $account failed (OSStatus $status)")
            }
        } finally {
            CFRelease(attrs)
        }
    }

    private fun deleteSync(account: String) =
        memScoped {
            val query = baseQuery(account)
            try {
                val status = SecItemDelete(query)
                if (status != errSecSuccess && status != errSecItemNotFound) {
                    throw SecureStorageException(
                        "Keychain delete for $account failed (OSStatus $status)",
                    )
                }
            } finally {
                CFRelease(query)
            }
        }

    private fun MemScope.baseQuery(account: String): CFMutableDictionaryRef {
        val dict =
            CFDictionaryCreateMutable(
                allocator = null,
                capacity = 0,
                keyCallBacks = kCFTypeDictionaryKeyCallBacks.ptr,
                valueCallBacks = kCFTypeDictionaryValueCallBacks.ptr,
            ) ?: throw SecureStorageException("CFDictionaryCreateMutable failed")
        CFDictionaryAddValue(dict, kSecClass, kSecClassGenericPassword)
        val serviceCf =
            CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
                ?: run {
                    CFRelease(dict)
                    throw SecureStorageException("Failed to encode service name")
                }
        CFDictionaryAddValue(dict, kSecAttrService, serviceCf)
        CFRelease(serviceCf)
        val accountCf =
            CFStringCreateWithCString(null, account, kCFStringEncodingUTF8)
                ?: run {
                    CFRelease(dict)
                    throw SecureStorageException("Failed to encode account name")
                }
        CFDictionaryAddValue(dict, kSecAttrAccount, accountCf)
        CFRelease(accountCf)
        // Defense in depth: `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`
        // already prevents iCloud Keychain sync, but spelling it out makes the
        // intent unmistakable on read, write, and delete paths.
        CFDictionaryAddValue(dict, kSecAttrSynchronizable, kCFBooleanFalse)
        return dict
    }

    private fun cfDataToString(data: CFDataRef): String? {
        val length = CFDataGetLength(data).toInt()
        if (length <= 0) return ""
        val ptr = CFDataGetBytePtr(data) ?: return null
        val bytes = ByteArray(length) { idx -> ptr[idx].toByte() }
        return bytes.decodeToString()
    }
}
