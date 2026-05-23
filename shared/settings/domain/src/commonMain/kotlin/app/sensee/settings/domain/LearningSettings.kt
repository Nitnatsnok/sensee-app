package app.sensee.settings.domain

public data class LearningSettings(
    val studyLanguageTag: String? = null,
    val translationLanguageTag: String? = null,
    /** Ids of [LearningTopic]s the user picked to steer AI example generation. */
    val preferredTopicIds: Set<String> = emptySet(),
)
