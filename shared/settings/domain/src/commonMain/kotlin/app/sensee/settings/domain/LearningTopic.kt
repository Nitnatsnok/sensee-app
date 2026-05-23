package app.sensee.settings.domain

/**
 * A learning topic the user can mark as a preference. The catalog is served by
 * the backend, not hardcoded in the client.
 *
 * [displayName] is shown in the UI (native language); [promptKeyword] is the
 * neutral English keyword handed to the AI seam to steer example generation.
 */
public data class LearningTopic(
    val id: String,
    val displayName: String,
    val promptKeyword: String,
)
