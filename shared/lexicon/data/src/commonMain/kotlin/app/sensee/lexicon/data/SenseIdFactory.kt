package app.sensee.lexicon.data

import app.sensee.lexicon.domain.SenseId

/**
 * Mints [SenseId]s for the sense store. Split out so identity generation is a
 * single, testable seam (the `ExperimentalUuidApi` opt-in stays localized to the
 * implementation, and tests inject a deterministic factory).
 */
public interface SenseIdFactory {
    /** A fresh random id for a new Personal sense. */
    public fun mintPersonal(): SenseId

    /** A deterministic, namespaced id for a Service sense — stable across re-sync. */
    public fun forServiceSource(sourceRef: String): SenseId
}
