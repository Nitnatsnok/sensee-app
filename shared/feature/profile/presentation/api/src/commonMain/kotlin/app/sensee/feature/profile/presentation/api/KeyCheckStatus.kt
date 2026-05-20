package app.sensee.feature.profile.presentation.api

/**
 * Per-section key-verification state. Progressive field disclosure in the
 * settings screen is a pure function of this: model/voice fields appear only
 * once a key reaches [Valid]; [Invalid] still shows the fields as manual entry
 * (ADR-005 — verification never hard-blocks configuration).
 */
public sealed interface KeyCheckStatus {
    /** Not verified yet — nothing below the key field. */
    public data object Idle : KeyCheckStatus

    /** Verification request in flight. */
    public data object Checking : KeyCheckStatus

    /** Key accepted; the provider's model/voice options are available. */
    public data object Valid : KeyCheckStatus

    /** Key rejected or provider unreachable; manual entry is offered instead. */
    public data class Invalid(
        val reason: String,
    ) : KeyCheckStatus
}
