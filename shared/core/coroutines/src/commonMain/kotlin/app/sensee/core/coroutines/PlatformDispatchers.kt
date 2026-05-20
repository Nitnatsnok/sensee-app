package app.sensee.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The dispatcher for blocking IO work.
 *
 * This is `expect`/`actual` — not a runtime `when (platform)` or DI choice — because
 * `Dispatchers.IO` is a JVM-only API. It simply does not exist in the JS/Wasm/Native
 * kotlinx-coroutines artifacts, so a single common expression referencing it would not
 * compile off-JVM. JVM/Android map to `Dispatchers.IO`; the other targets fall back to
 * `Dispatchers.Default` (their event loop has no separate IO pool). Keep the per-target
 * actuals; collapsing them is not possible without reintroducing platform code anyway.
 */
internal expect val appIoDispatcher: CoroutineDispatcher
