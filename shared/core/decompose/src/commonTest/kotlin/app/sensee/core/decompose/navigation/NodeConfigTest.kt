package app.sensee.core.decompose.navigation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NodeConfigTest {
    @Test
    fun `bringNodeToFront adds a new node when composite match key differs`() {
        val initialStack: List<NodeConfig<TestConfig>> =
            listOf(
                NodeConfig(
                    destination =
                        TestConfig.Deck(
                            deckId = "a",
                            source = "library",
                        ),
                ),
            )

        val updatedStack =
            initialStack.bringNodeToFront(
                configuration = TestConfig.Deck(deckId = "a", source = "favorites"),
                recreateIfSameConfig = false,
                matchKeySelector = TestConfig::matchKey,
            )

        assertEquals(2, updatedStack.size)
        assertEquals(TestConfig.Deck(deckId = "a", source = "library"), updatedStack.first().destination)
        assertEquals(TestConfig.Deck(deckId = "a", source = "favorites"), updatedStack.last().destination)
    }

    @Test
    fun `pushNodeToFront updates existing node when match key is the same`() {
        val initialStack: List<NodeConfig<TestConfig>> =
            listOf(
                NodeConfig(
                    destination =
                        TestConfig.Deck(
                            deckId = "a",
                            source = "library",
                            sessionId = "first",
                        ),
                ),
            )

        val updatedStack =
            initialStack.pushNodeToFront(
                configuration = TestConfig.Deck(deckId = "a", source = "library", sessionId = "second"),
                matchKeySelector = TestConfig::matchKey,
            )

        assertEquals(1, updatedStack.size)
        assertEquals(
            TestConfig.Deck(deckId = "a", source = "library", sessionId = "second"),
            updatedStack.single().destination,
        )
    }

    @Test
    fun `bringNodeToFront changes id when same config must be recreated`() {
        val initialNode: NodeConfig<TestConfig> = NodeConfig(destination = TestConfig.Home)

        val updatedStack =
            listOf(initialNode).bringNodeToFront(
                configuration = TestConfig.Home,
                recreateIfSameConfig = true,
                matchKeySelector = TestConfig::matchKey,
            )

        assertEquals(TestConfig.Home, updatedStack.single().destination)
        assertNotEquals(initialNode.id, updatedStack.single().id)
    }

    @Test
    fun `replaceAllNodes reuses matching nodes by composite key order`() {
        val deckNode: NodeConfig<TestConfig> =
            NodeConfig(
                id = 7L,
                destination = TestConfig.Deck(deckId = "a", source = "library", sessionId = "first"),
            )
        val homeNode: NodeConfig<TestConfig> =
            NodeConfig(
                id = 8L,
                destination = TestConfig.Home,
            )

        val updatedStack =
            listOf(deckNode, homeNode).replaceAllNodes(
                configurations =
                    arrayOf(
                        TestConfig.Home,
                        TestConfig.Deck(deckId = "a", source = "library", sessionId = "second"),
                    ),
                recreateIfSameConfig = false,
                matchKeySelector = TestConfig::matchKey,
            )

        assertEquals(
            listOf(
                TestConfig.Home,
                TestConfig.Deck(deckId = "a", source = "library", sessionId = "second"),
            ),
            updatedStack.map { it.destination },
        )
        assertEquals(8L, updatedStack[0].id)
        assertEquals(7L, updatedStack[1].id)
    }

    @Test
    fun `nodeConfigSerializer serializes and deserializes node config`() {
        val serializer = nodeConfigSerializer<TestConfig>(TestConfig.serializer())
        val json = Json
        val nodeConfig: NodeConfig<TestConfig> =
            NodeConfig(
                id = 42L,
                destination = TestConfig.Deck(deckId = "a", source = "library", sessionId = "session"),
            )

        val encoded = json.encodeToString(serializer, nodeConfig)
        val decoded = json.decodeFromString(serializer, encoded)

        assertEquals(nodeConfig, decoded)
    }
}

@Serializable
private sealed interface TestConfig : ScreenConfig {
    @Serializable
    data object Home : TestConfig

    @Serializable
    data class Deck(
        val deckId: String,
        val source: String,
        val sessionId: String? = null,
    ) : TestConfig
}

private fun TestConfig.matchKey(): TestConfigMatchKey =
    when (this) {
        TestConfig.Home -> TestConfigMatchKey.Home
        is TestConfig.Deck -> TestConfigMatchKey.Deck(deckId = deckId, source = source)
    }

private sealed interface TestConfigMatchKey {
    data object Home : TestConfigMatchKey

    data class Deck(
        val deckId: String,
        val source: String,
    ) : TestConfigMatchKey
}
