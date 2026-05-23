package app.sensee.settings.data.topic

import app.sensee.core.mockBackend.MockFixtureSet
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Learning-topic catalog served by the mock backend. The list is intentionally
 * data, not a client-side enum: the set of topics is a backend concern.
 */
@ContributesIntoSet(AppScope::class)
@Inject
public class TopicCatalogMockFixtures : MockFixtureSet {
    override val fixtures: Map<String, String> =
        mapOf("learning/topics" to TOPIC_CATALOG_FIXTURE)
}

private const val TOPIC_CATALOG_FIXTURE = """
{
  "topics": [
    { "id": "everyday-life", "display_name": "Повседневная жизнь", "prompt_keyword": "everyday life" },
    { "id": "travel", "display_name": "Путешествия", "prompt_keyword": "travel and tourism" },
    { "id": "food-cooking", "display_name": "Еда и кулинария", "prompt_keyword": "food and cooking" },
    { "id": "business-work", "display_name": "Бизнес и работа", "prompt_keyword": "business and the workplace" },
    { "id": "technology", "display_name": "Технологии и IT", "prompt_keyword": "technology and software" },
    { "id": "science", "display_name": "Наука", "prompt_keyword": "science" },
    { "id": "health", "display_name": "Здоровье и медицина", "prompt_keyword": "health and medicine" },
    { "id": "arts-culture", "display_name": "Искусство и культура", "prompt_keyword": "arts and culture" },
    { "id": "sports", "display_name": "Спорт и фитнес", "prompt_keyword": "sports and fitness" },
    { "id": "nature", "display_name": "Природа и экология", "prompt_keyword": "nature and the environment" },
    { "id": "news-politics", "display_name": "Новости и политика", "prompt_keyword": "news and politics" },
    { "id": "entertainment", "display_name": "Развлечения и медиа", "prompt_keyword": "entertainment and media" }
  ]
}
"""
