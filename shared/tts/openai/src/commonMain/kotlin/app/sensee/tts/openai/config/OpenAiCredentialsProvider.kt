package app.sensee.tts.openai.config

/** Supplies the OpenAI API key for Bearer authentication. */
public fun interface OpenAiCredentialsProvider {
    public suspend fun apiKey(): String
}

public class StaticOpenAiCredentialsProvider(
    private val apiKey: String,
) : OpenAiCredentialsProvider {
    override suspend fun apiKey(): String = apiKey
}
