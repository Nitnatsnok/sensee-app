package app.sensee.core.secureStorage

internal actual fun webLocalStorageGet(
    @Suppress("UNUSED_PARAMETER") key: String,
): String? =
    // Keep nullable: localStorage.getItem returns null for absent keys.
    js("window.localStorage.getItem(key)") as String?

internal actual fun webLocalStorageSet(
    @Suppress("UNUSED_PARAMETER") key: String,
    @Suppress("UNUSED_PARAMETER") value: String,
): Unit = js("{ window.localStorage.setItem(key, value); }")

internal actual fun webLocalStorageRemove(
    @Suppress("UNUSED_PARAMETER") key: String,
): Unit = js("{ window.localStorage.removeItem(key); }")
