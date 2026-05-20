package app.sensee.tts.openai.synthesis

import app.sensee.tts.openai.api.OpenAiSpeechApi
import app.sensee.tts.openai.config.OpenAiCredentialsProvider
import app.sensee.tts.openai.config.OpenAiSpeechConfig
import io.ktor.client.HttpClient

public object OpenAiSynthesizerFactory {
    public fun create(
        httpClient: HttpClient,
        config: OpenAiSpeechConfig,
        credentials: OpenAiCredentialsProvider,
    ): OpenAiSynthesizer =
        OpenAiSynthesizer(
            api = OpenAiSpeechApi(httpClient, config, credentials),
            config = config,
        )
}
