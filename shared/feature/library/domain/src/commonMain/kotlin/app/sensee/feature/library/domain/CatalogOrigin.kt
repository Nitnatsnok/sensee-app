package app.sensee.feature.library.domain

/**
 * Where catalog material came from (ADR-001). An explicit, typed provenance —
 * NOT inferred from id-string prefixes — so the Library can group "your own"
 * vs "service suggestions" and adoption keeps provenance.
 */
public enum class CatalogOrigin {
    /** Created by the user (capture); lives in the local DB. */
    Personal,

    /** Curated by the service (backend-compatible boundary / mock backend). */
    Service,
}
