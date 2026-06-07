package app.sensee.settings.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiProviderTest {
    @Test
    fun `OpenAI structured output model detection includes dotted GPT 5 families`() {
        assertTrue(AiProvider.OpenAi.supportsJsonSchemaResponseFormat("gpt-5.5"))
        assertTrue(AiProvider.OpenAi.supportsJsonSchemaResponseFormat("gpt-5.4-mini"))
        assertTrue(AiProvider.OpenAi.supportsJsonSchemaResponseFormat("gpt-4.1-mini"))
        assertFalse(AiProvider.OpenAi.supportsJsonSchemaResponseFormat("gpt-4-turbo"))
    }

    @Test
    fun `OpenRouter stays on plain json mode for OpenAI-compatible models`() {
        assertFalse(AiProvider.OpenRouter.supportsJsonSchemaResponseFormat("openai/gpt-5.5"))
    }
}
