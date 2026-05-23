package app.sensee.settings.domain

/**
 * Read-only access to the backend-served catalog of [LearningTopic]s. Consumers
 * resolve user-selected [LearningSettings.preferredTopicIds] against this
 * catalog (the UI to display labels, the AI seam to obtain prompt keywords).
 */
public interface TopicCatalogRepository {
    public suspend fun topics(): List<LearningTopic>
}
